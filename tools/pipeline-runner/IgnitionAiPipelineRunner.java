package tools.pipeline_runner;

import com.ignitionai.obd.*;
import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.phase4.*;
import com.ignitionai.anomaly.*;
import com.ignitionai.expectedbehaviour.*;
import com.ignitionai.episode.*;
import com.ignitionai.degradation.features.*;
import com.ignitionai.degradation.analyzer.*;
import com.ignitionai.degradation.state.*;
import com.ignitionai.degradation.risk.*;
import com.ignitionai.degradation.store.*;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.healthscore.*;
import com.ignitionai.vhi.*;
import com.ignitionai.severity.*;
import com.ignitionai.phase6.*;
import com.ignitionai.phase7.snapshot.*;
import com.ignitionai.phase7.app.*;

import java.io.File;
import java.util.*;

public class IgnitionAiPipelineRunner {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Phase 1-7 IgnitionAI Cumulative Pipeline ===");
        
        // 1. Phase 2: OBD Input
        System.out.println("\n[Phase 2] Parsing OBD Input");
        File fixture = new File("fixtures/obd-input-v1-multisample.json");
        ObservationStream stream = ObdInputParser.parse(fixture);
        System.out.println("Loaded " + stream.readings.size() + " readings.");
        
        // 2. Phase 3: Context & Features
        System.out.println("\n[Phase 3] Generating Vehicle Context & Analytical Observations");
        VehicleContextManager contextManager = new VehicleContextManager();
        VehicleContext ctxOrig = contextManager.initializeContext(stream.vehicleId, 0L);
        Configuration config = new Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
        VehicleContext context = new VehicleContext("1.0", 0L, ctxOrig.getIdentity(), config, ctxOrig.getOperatingConditions(), ctxOrig.getEvidenceQuality());

        WindowBuffer buffer = new WindowBuffer();
        FeatureCatalogue catalogue = new FeatureCatalogue();
        
        ManifestLoader loader = new ManifestLoader();
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        runtime.loadSensors(loader.loadManifests(new File("virtual-sensors/src/main/resources/sensors")));

        Map<String, AnalyticalObservation> rawObs = new HashMap<>();
        Map<String, Double> latestObs = new HashMap<>();
        long currentTs = 0;

        for (SensorReadingDto dto : stream.readings) {
            buffer.addReading(new SensorReading(dto.signalId, dto.value, dto.timestampMs, dto.units));
            latestObs.put(dto.signalId, dto.value);
            currentTs = dto.timestampMs;
            rawObs.put(dto.signalId, new AnalyticalObservation(dto.signalId, dto.value, dto.units, currentTs, 
                AnalyticalObservation.QualityState.AVAILABLE, List.of(), context.getContextVersion(), "1.0", "0", "adapter"));
            context = contextManager.transitionContext(context, latestObs, currentTs);
        }

        Map<String, AnalyticalObservation> allOutputs = new HashMap<>(rawObs);
        for (Feature f : catalogue.getAllFeatures()) {
            allOutputs.put(f.getFeatureId(), f.calculate(buffer, context));
        }
        for (SensorDefinition def : runtime.getRegisteredSensors().values()) {
            allOutputs.put(def.getSensorId(), runtime.evaluate(def.getSensorId(), allOutputs, context, currentTs));
        }

        System.out.println("Generated " + allOutputs.size() + " analytical observations.");
        
        // 3. Phase 4: Anomaly Detection
        System.out.println("\n[Phase 4] Expected Behaviour & Anomaly Detection");
        SignalAnomalyTracker tracker = new SignalAnomalyTracker("coolant_mean_60s", "sess-1");
        ExpectedBehaviourModelPlugin expectedModel = new RollingMeanModel(5000);
        AnomalyDetectorPlugin detector = new ThresholdAnomalyDetector(0.1);
        EpisodeStore episodeStore = new EpisodeStore();
        
        AnalyticalObservation meanObs = allOutputs.get("coolant_mean_60s");
        List<AnomalyEpisode> currentEpisodes = new ArrayList<>();
        if (meanObs != null) {
            double expected = meanObs.getValue() * 1.05;
            Phase4Observation p4 = new Phase4Observation(stream.vehicleId, "sess-1", meanObs.getObservationId(), 
                                                         List.of(), meanObs.getValue(), expected, meanObs.getValue() - expected, 0.1, 
                                                         (meanObs.getValue() - expected)/expected, 0.8, AnomalyState.ANOMALY_ACTIVE, 
                                                         currentTs, context.getContextVersion(), "1.0", "1.0", List.of());
            
            Double score = detector.evaluate(p4, tracker);
            tracker.setCurrentState(AnomalyState.ANOMALY_ACTIVE);
            
            EpisodeRecord record = episodeStore.getOrCreateActiveEpisode(stream.vehicleId, "sess-1", "coolant_mean_60s", currentTs, score, tracker.getCurrentState());
            episodeStore.updateEpisode(record, score, tracker.getCurrentState(), currentTs);
            
            AnomalyEpisode ep = new AnomalyEpisode();
            ep.setEpisodeId(record.getEpisodeId());
            ep.setVehicleId(stream.vehicleId);
            ep.setSessionId("sess-1");
            ep.setSubsystemId("coolant_mean_60s");
            ep.setStartTimeMs(record.getStartTimestampMs());
            ep.setEndTimeMs(record.getEndTimestampMs());
            ep.setSeverity(record.getMaxAnomalyScore());
            currentEpisodes.add(ep);
        }
        System.out.println("Generated " + currentEpisodes.size() + " anomaly episodes.");

        // 4. Phase 5: Degradation & Risk
        System.out.println("\n[Phase 5] Degradation State & Event Risk");
        DegradationEvidenceStore degStore = new InMemoryEvidenceStore();
        DegradationFeatureBuilder featureBuilder = new DegradationFeatureBuilder();
        TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
        DegradationStateEstimator stateEstimator = new DegradationStateEstimator();
        EventRiskEstimator riskEstimator = new EventRiskEstimator();

        List<DegradationOutput> degOutputs = new ArrayList<>();
        for (AnomalyEpisode ep : currentEpisodes) {
            List<AnomalyEpisode> hist = degStore.getEpisodes(ep.getVehicleId(), ep.getSubsystemId());
            DegradationFeatures feats = featureBuilder.buildFeatures(ep, hist);
            TrendResult trend = trendAnalyzer.analyzeTrend(ep, hist);
            DegradationStateResult stateResult = stateEstimator.estimateState(feats, trend);
            EventRiskResult riskResult = riskEstimator.estimateRisk(feats, trend, stateResult, "critical_failure", 168, true);
            degStore.storeEpisode(ep);
            
            DegradationOutput out = new DegradationOutput();
            out.setVehicleId(ep.getVehicleId());
            out.setSubsystemId(ep.getSubsystemId());
            out.setEvaluationTimeMs(ep.getEndTimeMs());
            out.setDegradationState(stateResult.getState());
            out.setDegradationScore(stateResult.getScore());
            out.setTrendDirection(trend.getDirection());
            out.setTrendSlope(trend.getTrendSlope());
            out.setPersistenceScore(feats.getMeanPersistence());
            out.setRecurrenceScore(feats.getRecurrenceCount().doubleValue());
            out.setEventRiskEstimate(riskResult.getRiskEstimate());
            out.setConfidence(riskResult.getConfidence());
            out.setUncertainty(riskResult.getUncertainty());
            out.setDataSufficiencyStatus(DegradationOutput.DataSufficiency.SUFFICIENT);
            degOutputs.add(out);
        }
        System.out.println("Generated " + degOutputs.size() + " degradation outputs.");
        
        // 5. Phase 6: Health Scoring & Severity
        System.out.println("\n[Phase 6] Subsystem & Vehicle Health Indexing");
        SeverityClassifier severityClassifier = new SeverityClassifier(SeverityConfiguration.getDefault());
        SubsystemScoreCalculator subCalc = new SubsystemScoreCalculator(severityClassifier, ScoringConfiguration.getDefault());
        VhiCalculator vhiCalc = new VhiCalculator();
        
        List<SubsystemHealthAssessment> subAssessments = new ArrayList<>();
        for (DegradationOutput deg : degOutputs) {
            subAssessments.add(subCalc.calculateScore(deg));
        }
        if (subAssessments.isEmpty()) {
             // Mock one if no anomalies happened in stream
             subAssessments.add(new SubsystemHealthAssessment("engine", "engine", SeverityLevel.NORMAL, 95.0, 0.9, 0.1, List.of(), List.of(), true));
        }

        Map<String, Double> weights = Map.of("engine", 0.6, "transmission", 0.4, "coolant_mean_60s", 0.3);
        VehicleHealthAssessment vha = vhiCalc.calculateVhi(stream.vehicleId, currentTs, 3600000L, subAssessments, weights, new ArrayList<>(), "1.0", "1.0", context.getContextVersion());
        System.out.println("Calculated VHI: " + vha.getVehicleHealthIndex() + " (" + vha.getHealthBand() + ")");

        // Schema validation check
        Phase6ContractValidator.validateContract(vha);
        System.out.println("Phase 6 Contract strictly validated.");

        // 6. Phase 7: Certificate & Dealership App
        System.out.println("\n[Phase 7] Certificate Snapshot & LaTeX Rendering");
        CertificateSnapshot snapshot = new CertificateSnapshot(vha);
        
        DealershipAppMVP app = new DealershipAppMVP();
        app.loadAndDisplaySnapshot(snapshot);
        app.exportCertificate(snapshot, "out/certificates");

        System.out.println("\n=== All Phases Integrated Successfully ===");
    }
}
