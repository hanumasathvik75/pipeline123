package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Fuel-trim correction load proxy: how hard the ECU is having to correct the commanded fuel
 * mixture. Combines short- and long-term bank-1 trims (their sum approximates total correction —
 * see CombinedFuelTrim_pct in the sensor catalogue) and reports the fraction of the session where
 * that correction exceeds a configured tolerance (config/diagnostic-index-config.json, "fuel").
 * A raw scan tool shows LTFT/STFT as two independent numbers per instant; this reports how
 * persistently the ECU is compensating across the whole drive, which is the more actionable
 * signal for vacuum leaks, injector or MAF drift.
 */
public final class FuelIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "fuel";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Fuel"; }
    @Override public String getPluginVersion() { return "combined-trim-exceedance.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("short_term_fuel_trim_b1", "long_term_fuel_trim_b1"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double thresholdPct = cfg.number(ID, "threshold_pct");
        double exceedanceForZeroHealth = cfg.number(ID, "exceedance_for_zero_health");
        double exceedanceForFlaggedState = cfg.number(ID, "exceedance_for_flagged_state");
        double minSamplesForConfidence = cfg.number(ID, "min_samples_for_confidence");

        List<SeriesUtil.Sample> stft = SeriesUtil.series(session, "short_term_fuel_trim_b1");
        List<SeriesUtil.Sample> ltft = SeriesUtil.series(session, "long_term_fuel_trim_b1");
        if (stft.isEmpty() || ltft.isEmpty()) {
            return DiagnosticIndexResult.unsupported(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "short_term_fuel_trim_b1 or long_term_fuel_trim_b1 not present in session", getPluginVersion());
        }
        int j = 0;
        List<Double> combined = new java.util.ArrayList<>();
        for (SeriesUtil.Sample s : stft) {
            while (j + 1 < ltft.size() && Math.abs(ltft.get(j + 1).t - s.t) <= Math.abs(ltft.get(j).t - s.t)) j++;
            if (Math.abs(ltft.get(j).t - s.t) > 5000) continue;
            combined.add(s.v + ltft.get(j).v);
        }
        if (combined.size() < 20) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 20 time-aligned STFT/LTFT pairs", getPluginVersion());
        }
        long exceed = combined.stream().filter(v -> Math.abs(v) > thresholdPct).count();
        double exceedanceFraction = (double) exceed / combined.size();
        double meanAbsTrim = combined.stream().mapToDouble(Math::abs).average().orElse(0);
        double health = 100.0 * (1.0 - SeriesUtil.clamp01(exceedanceFraction / exceedanceForZeroHealth));
        double confidence = 100.0 * SeriesUtil.clamp01(combined.size() / minSamplesForConfidence) * 0.9;
        DiagnosticIndexResult.State state = exceedanceFraction > exceedanceForFlaggedState
            ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("steady_samples", String.valueOf(combined.size()));
        evidence.put("threshold_pct", String.format(Locale.ROOT, "%.1f", thresholdPct));
        evidence.put("exceedance_fraction", String.format(Locale.ROOT, "%.4f", exceedanceFraction));
        evidence.put("mean_abs_combined_trim_pct", String.format(Locale.ROOT, "%.2f", meanAbsTrim));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence, state,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
