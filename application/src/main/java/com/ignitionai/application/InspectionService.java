package com.ignitionai.application;

import com.ignitionai.obdinput.schema.*;
import com.ignitionai.obdinput.preprocessor.*;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.expectedbehaviour.RollingMeanModel;
import com.ignitionai.residual.ResidualCalculator;
import com.ignitionai.anomaly.*;
import com.ignitionai.phase4.*;
import com.ignitionai.degradation.features.*;
import com.ignitionai.degradation.analyzer.*;
import com.ignitionai.degradation.state.*;
import com.ignitionai.degradation.risk.*;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.healthscore.*;
import com.ignitionai.severity.*;
import com.ignitionai.phase6.*;
import com.ignitionai.vhi.VhiCalculator;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import java.io.File;
import java.util.*;

/** Session-local orchestration. No UI dependencies and no fixture substitutions. */
public final class InspectionService {
    private final File sensorDirectory;
    public InspectionService(File sensorDirectory) { this.sensorDirectory = sensorDirectory; }

    public InspectionResult assess(List<ObdMessage> input) throws Exception {
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("No OBD records to assess");
        ObdMessage first = input.get(0);
        String vehicle = first.getVehicleRef().getVehicleId(), session = first.getSessionId();
        ObdPreProcessor pre = new ObdPreProcessor();
        VehicleContextManager cm = new VehicleContextManager();
        VehicleContext context = cm.initializeContext(vehicle, 0L);
        WindowBuffer features = new WindowBuffer();
        Map<String, WindowBuffer> baselines = new TreeMap<>();
        Map<String, List<com.ignitionai.features.SensorReading>> frozenBaselines = new TreeMap<>();
        Map<String, SignalAnomalyTracker> trackers = new TreeMap<>();
        Map<String, AnomalyEpisode> active = new TreeMap<>();
        Map<String, Double> latest = new TreeMap<>();
        Map<String, AnalyticalObservation> observed = new TreeMap<>();
        List<ObdMessage> normalized = new ArrayList<>();
        List<AnomalyEpisode> episodes = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Set<String> dtcCodes = new TreeSet<>();
        boolean completeDtcSeen = false;
        Map<String, Double> floors = Map.of("engine_rpm", 150.0, "coolant_temperature", 3.0,
            "vehicle_speed", 5.0, "calculated_engine_load", 5.0, "throttle_position", 5.0);
        long end = 0, start = Long.MAX_VALUE;
        int invalid = 0;
        for (ObdMessage raw : input) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Assessment cancelled");
            if (!vehicle.equals(raw.getVehicleRef().getVehicleId()) || !session.equals(raw.getSessionId()) || raw.getSourceType() != first.getSourceType())
                throw new IllegalArgumentException("Import one vehicle/session at a time");
            ProcessingResult processed = pre.process(raw);
            ObdMessage msg = processed.getMessage(); normalized.add(msg);
            if (msg.getDtcSnapshotCompleteness() == DtcSnapshotCompleteness.COMPLETE) {
                dtcCodes.clear(); completeDtcSeen = true;
            }
            for (DtcObservation dtc : msg.getDtcObservations()) {
                if (dtc.getObservationType() == ObservationType.CLEARED) dtcCodes.remove(dtc.getCode());
                else dtcCodes.add(dtc.getCode());
            }
            if (!processed.getEvents().isEmpty()) notes.add("Sequence discontinuity at " + raw.getMessageId());
            for (com.ignitionai.obdinput.schema.SensorReading r : msg.getSensorReadings()) {
                if (r.getQuality().getStatus() != QualityStatus.VALID || !Double.isFinite(r.getValue()) || r.getMonotonicMs() < end) { invalid++; continue; }
                long t = r.getMonotonicMs(); start = Math.min(start, t); end = Math.max(end, t);
                latest.put(r.getSignalId(), r.getValue());
                context = cm.transitionContext(context, latest, t);
                String ref = msg.getMessageId() + "/" + r.getSignalId() + "/" + t;
                AnalyticalObservation obs = new AnalyticalObservation(r.getSignalId(), r.getValue(), r.getUnit(), t,
                    AnalyticalObservation.QualityState.AVAILABLE, List.of(ref), context.getContextVersion(), "application.v1", "0", msg.getSourceType().name());
                observed.put(r.getSignalId(), obs);
                features.addReading(new com.ignitionai.features.SensorReading(r.getSignalId(), r.getValue(), t, r.getUnit()));
                if (!floors.containsKey(r.getSignalId())) continue;
                String key = r.getSignalId() + "/" + context.getOperatingConditions().getOperatingRegime();
                WindowBuffer baseline = baselines.computeIfAbsent(key, x -> new WindowBuffer());
                SignalAnomalyTracker tracker = trackers.computeIfAbsent(key, x -> new SignalAnomalyTracker(key, session));
                List<com.ignitionai.features.SensorReading> past = baseline.getReadings(r.getSignalId(), t - 1, 10000L);
                if (frozenBaselines.containsKey(key)) past = frozenBaselines.get(key);
                Double expected = past.size() >= 5 ? past.stream().mapToDouble(com.ignitionai.features.SensorReading::getValue).average().orElseThrow() : null;
                Double sigma = null;
                if (expected != null) {
                    double sum = 0;
                    for (com.ignitionai.features.SensorReading p : past) sum += Math.pow(p.getValue() - expected, 2);
                    sigma = Math.sqrt(sum / Math.max(1, past.size() - 1) + Math.pow(floors.get(r.getSignalId()), 2));
                }
                Double residual = ResidualCalculator.calculateResidual(r.getValue(), expected);
                Double normalizedResidual = ResidualCalculator.calculateNormalizedResidual(residual, sigma);
                // Quarantine suspect observations from the learned nominal baseline, including onset.
                if (normalizedResidual != null && Math.abs(normalizedResidual) > 3)
                    frozenBaselines.putIfAbsent(key, List.copyOf(past));
                Phase4Observation p4 = new Phase4Observation(vehicle, session, r.getSignalId(), List.of(ref), r.getValue(), expected,
                    residual, sigma, ResidualCalculator.calculateNormalizedResidual(residual, sigma), null, null, t,
                    context.getContextVersion(), "causal-regime-mean.v1", "preliminary.v1", List.of(ref));
                double score = new RollingWindowDetector(3, 2000, 3000).evaluate(p4, tracker);
                AnomalyState state = tracker.getCurrentState();
                if (state == AnomalyState.ANOMALY_ACTIVE || state == AnomalyState.ESCALATION) {
                    AnomalyEpisode ep = active.get(key);
                    if (ep == null) {
                        ep = new AnomalyEpisode(vehicle, session, r.getSignalId(), session + "/" + key + "/" + tracker.getFirstViolationMs(),
                            tracker.getFirstViolationMs(), t, score, (double)(t - tracker.getFirstViolationMs()), 0, 1.0,
                            context.getOperatingConditions().getOperatingRegime().name(), Map.of("confidence", 0.5), new ArrayList<>(),
                            context.getContextVersion(), "causal-regime-mean.v1", "uncalibrated", null);
                        active.put(key, ep); episodes.add(ep);
                    }
                    ep.setSeverity(Math.max(ep.getSeverity(), score)); ep.setEndTimeMs(t);
                    ep.setDurationMs((double)(t - ep.getStartTimeMs())); ep.getEvidenceReferences().add(ref);
                } else if (state == AnomalyState.CLOSURE || state == AnomalyState.NOMINAL) active.remove(key);
                if (state == AnomalyState.CLOSURE || state == AnomalyState.NOMINAL) frozenBaselines.remove(key);
                if (!frozenBaselines.containsKey(key)) baseline.addReading(new com.ignitionai.features.SensorReading(r.getSignalId(), r.getValue(), t, r.getUnit()));
                baseline.cleanup(t, 15000L); features.cleanup(t, 120000L);
            }
        }
        Map<String, AnalyticalObservation> all = new TreeMap<>(observed);
        for (Feature feature : new FeatureCatalogue().getAllFeatures()) all.put(feature.getFeatureId(), feature.calculate(features, context));
        if (sensorDirectory != null && sensorDirectory.isDirectory()) {
            VirtualSensorRuntime runtime = new VirtualSensorRuntime();
            runtime.loadSensors(new ManifestLoader().loadManifests(sensorDirectory));
            for (String id : new TreeSet<>(runtime.getRegisteredSensors().keySet())) all.put(id, runtime.evaluate(id, all, context, end));
        } else notes.add("Virtual sensor catalog not found");
        List<DegradationOutput> degradation = new ArrayList<>();
        Map<String, List<AnomalyEpisode>> histories = new TreeMap<>();
        for (AnomalyEpisode ep : episodes) {
            List<AnomalyEpisode> history = histories.computeIfAbsent(ep.getSubsystemId(), x -> new ArrayList<>());
            DegradationFeatures f = new DegradationFeatureBuilder().buildFeatures(ep, history);
            TrendResult trend = new TrendAnalyzer().analyzeTrend(ep, history);
            DegradationStateResult state = new DegradationStateEstimator().estimateState(f, trend);
            EventRiskResult risk = new EventRiskEstimator().estimateRisk(f, trend, state, "critical_failure", 168, false);
            DegradationOutput d = new DegradationOutput();
            d.setVehicleId(vehicle); d.setSubsystemId(ep.getSubsystemId()); d.setEvaluationTimeMs(ep.getEndTimeMs());
            d.setDegradationState(state.getState()); d.setDegradationScore(Math.max(0, Math.min(1, state.getScore())) * 100);
            d.setPersistenceScore(f.getMeanPersistence()); d.setRecurrenceScore(history.size() / (history.size() + 1.0));
            d.setTrendDirection(trend.getDirection()); d.setTrendSlope(trend.getTrendSlope());
            d.setRiskStatus(risk.getStatus()); d.setEventRiskEstimate(risk.getStatus() == DegradationOutput.RiskStatus.AVAILABLE ? risk.getRiskEstimate() : null);
            d.setPredictionHorizonMs(168L * 3600000); d.setConfidence(0.5); d.setUncertainty(Math.min(1, trend.getUncertainty()));
            d.setDataSufficiencyStatus(history.size() >= 2 ? DegradationOutput.DataSufficiency.SPARSE : DegradationOutput.DataSufficiency.INSUFFICIENT);
            d.setEvidenceReferences(List.copyOf(ep.getEvidenceReferences())); d.setModelVersion("state-space.v1"); d.setConfigurationVersion("preliminary.v1");
            degradation.add(d); history.add(ep);
        }
        List<SubsystemHealthAssessment> subs = new ArrayList<>();
        SubsystemScoreCalculator calculator = new SubsystemScoreCalculator(new SeverityClassifier(null));
        for (String signal : new TreeSet<>(floors.keySet())) {
            DegradationOutput last = null;
            for (DegradationOutput d : degradation) if (signal.equals(d.getSubsystemId())) last = d;
            if (last != null) subs.add(calculator.calculateScore(last));
            else subs.add(new SubsystemHealthAssessment(signal, signal, SeverityLevel.UNKNOWN, null, 0.0, 1.0,
                List.of(observed.containsKey(signal) ? "NO_ESTABLISHED_DEGRADATION_HISTORY" : "NOT_OBSERVED"),
                observed.containsKey(signal) ? observed.get(signal).getSourceObservationReferences() : List.of(), false));
        }
        List<String> refs = new ArrayList<>(); for (AnomalyEpisode ep : episodes) refs.addAll(ep.getEvidenceReferences());
        Map<String, Double> weights = new TreeMap<>(); for (String id : floors.keySet()) weights.put(id, 1.0);
        VehicleHealthAssessment assessment = new VhiCalculator().calculateVhi(vehicle,
            normalized.get(normalized.size() - 1).getReceivedAt().toEpochMilli(), start == Long.MAX_VALUE ? 0 : end - start,
            subs, weights, refs, "preliminary.v1", "UNPUBLISHED", context.getContextVersion());
        notes.add("Preliminary indicator assessment: generic baselines require vehicle validation. Unassessed systems are unknown.");
        notes.add("Failure probability is unavailable until a calibrated model and sufficient history exist.");
        notes.add("Excluded invalid/stale/out-of-order readings: " + invalid);
        notes.add("Source: " + first.getSourceType() + "; session: " + session);
        notes.add("Latest DTC observations: " + (dtcCodes.isEmpty() ? (completeDtcSeen ? "none in the latest complete scan" : "scan unavailable") : String.join(", ", dtcCodes)));
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        for (ObdMessage message : normalized) digest.update((com.ignitionai.obdinput.storage.ObdJsonMapper.serialize(message) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder fingerprint = new StringBuilder();
        for (byte b : digest.digest()) fingerprint.append(String.format(Locale.ROOT, "%02x", b & 255));
        return new InspectionResult(normalized, episodes, degradation, new ArrayList<>(all.values()), notes,
            new CertificateSnapshot(assessment, first.getSourceType().name(), session, fingerprint.toString()));
    }
}
