package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.SignalIds;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.QualityStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Session-level data-quality composite: how much of the incoming stream was usable at all,
 * independent of any single subsystem's health. Unlike the other 8 indices, this one needs no
 * particular signal to be present -- it looks at every reading in the session. Three real,
 * independently-computed measurements (missingness, implausible-value fraction, achieved-vs-
 * expected sample rate); no new statistical technique, just honest bookkeeping the pipeline
 * already had the raw material for. All thresholds live in config/diagnostic-index-config.json
 * under "signal_quality".
 */
public final class SignalQualityScorePlugin implements DiagnosticIndexPlugin {
    private static final String ID = "signal_quality";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Signal Quality"; }
    @Override public String getPluginVersion() { return "coverage-plausibility-rate.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("all_observed_signals"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        Map<String, double[]> plausibleRanges = cfg.signalQualityPlausibleRanges();
        Map<String, Double> expectedMinHz = cfg.signalQualityExpectedMinHz();

        int totalReadings = 0, invalidReadings = 0, implausibleReadings = 0;
        Map<String, Integer> validCountBySignal = new TreeMap<>();
        Map<String, Long> firstMsBySignal = new TreeMap<>();
        Map<String, Long> lastMsBySignal = new TreeMap<>();

        for (ObdMessage m : session) {
            for (var r : m.getSensorReadings()) {
                totalReadings++;
                boolean validQuality = r.getQuality().getStatus() == QualityStatus.VALID && Double.isFinite(r.getValue());
                if (!validQuality) { invalidReadings++; continue; }
                String canonical = SignalIds.canonical(r.getSignalId());
                validCountBySignal.merge(canonical, 1, Integer::sum);
                long t = r.getMonotonicMs();
                firstMsBySignal.merge(canonical, t, Math::min);
                lastMsBySignal.merge(canonical, t, Math::max);
                double[] range = plausibleRanges.get(canonical);
                if (range != null && (r.getValue() < range[0] || r.getValue() > range[1])) implausibleReadings++;
            }
        }
        if (totalReadings == 0) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "No sensor readings in session", getPluginVersion());
        }

        double missingnessFraction = (double) invalidReadings / totalReadings;
        int plausibilityDenominator = totalReadings - invalidReadings;
        double implausibleFraction = plausibilityDenominator == 0 ? 0.0 : (double) implausibleReadings / plausibilityDenominator;

        List<Double> rateRatios = new ArrayList<>();
        for (Map.Entry<String, Double> e : expectedMinHz.entrySet()) {
            Integer count = validCountBySignal.get(e.getKey());
            if (count == null || count < 2) continue;
            double spanSeconds = (lastMsBySignal.get(e.getKey()) - firstMsBySignal.get(e.getKey())) / 1000.0;
            if (spanSeconds <= 0) continue;
            double achievedHz = count / spanSeconds;
            rateRatios.add(SeriesUtil.clamp01(achievedHz / e.getValue()));
        }
        double avgRateRatio = rateRatios.isEmpty() ? 1.0 : rateRatios.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);

        double weightMissingness = cfg.number(ID, "weight_missingness");
        double weightImplausibility = cfg.number(ID, "weight_implausibility");
        double weightSampleRate = cfg.number(ID, "weight_sample_rate");
        double health = 100.0
            - missingnessFraction * 100.0 * weightMissingness
            - implausibleFraction * 100.0 * weightImplausibility
            - (1.0 - avgRateRatio) * 100.0 * weightSampleRate;
        health = Math.max(0, Math.min(100, health));

        boolean flagged = missingnessFraction > cfg.number(ID, "missingness_flag_fraction")
            || implausibleFraction > cfg.number(ID, "implausible_flag_fraction")
            || avgRateRatio < cfg.number(ID, "min_sample_rate_ratio_for_flag");

        double confidence = 100.0 * SeriesUtil.clamp01(totalReadings / cfg.number(ID, "min_readings_for_full_confidence")) * cfg.number(ID, "confidence_scale");

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("missingness_fraction", String.format(Locale.ROOT, "%.4f", missingnessFraction));
        evidence.put("implausible_fraction", String.format(Locale.ROOT, "%.4f", implausibleFraction));
        evidence.put("avg_sample_rate_ratio", String.format(Locale.ROOT, "%.2f", avgRateRatio));
        evidence.put("total_readings", String.valueOf(totalReadings));
        evidence.put("invalid_readings", String.valueOf(invalidReadings));
        evidence.put("implausible_readings", String.valueOf(implausibleReadings));
        evidence.put("rate_checked_signals", String.valueOf(rateRatios.size()));

        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence,
            flagged ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
