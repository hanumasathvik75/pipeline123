package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Misfire-like combustion irregularity proxy.
 *
 * Method: engine_rpm is split into fixed-size rolling windows (window_samples consecutive valid
 * samples, engine running, above an idle floor to avoid idle-stall false positives). A window is
 * "flagged" when its sample standard deviation exceeds a load-scaled tolerance
 * (max(rpm_floor_std, rpm_std_fraction * windowMeanRpm)) — i.e. RPM should hold steadier than a
 * fixed fraction of itself; short-duration, high-variance windows are the statistical signature of
 * misfire/rough-running rather than normal throttle-driven RPM change (which this window length is
 * too short to capture as "steady-state variance"). All thresholds live in
 * config/diagnostic-index-config.json under "combustion", with their rationale.
 *
 * This is a proxy, not a cylinder-resolved misfire count — it cannot substitute for OEM misfire
 * counters and is labelled accordingly. It flags candidates for a technician to verify.
 */
public final class CombustionIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "combustion";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Combustion"; }
    @Override public String getPluginVersion() { return "rpm-residual-std.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("engine_rpm"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        int windowSamples = cfg.integer(ID, "window_samples");
        double rpmFloorStd = cfg.number(ID, "rpm_floor_std");
        double rpmStdFraction = cfg.number(ID, "rpm_std_fraction");
        double idleFloorRpm = cfg.number(ID, "idle_floor_rpm");
        double flagFractionForZeroHealth = cfg.number(ID, "flag_fraction_for_zero_health");
        double flagFractionForFlaggedState = cfg.number(ID, "flag_fraction_for_flagged_state");
        double minWindowsForConfidence = cfg.number(ID, "min_windows_for_confidence");

        List<SeriesUtil.Sample> rpm = SeriesUtil.series(session, "engine_rpm");
        if (rpm.size() < windowSamples * 4) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than " + (windowSamples * 4) + " valid engine_rpm samples", getPluginVersion());
        }
        int totalWindows = 0, flaggedWindows = 0;
        double thresholdSum = 0;
        for (int i = 0; i + windowSamples <= rpm.size(); i += windowSamples) {
            List<Double> window = new java.util.ArrayList<>();
            for (int j = i; j < i + windowSamples; j++) window.add(rpm.get(j).v);
            double windowMean = SeriesUtil.mean(window);
            if (windowMean < idleFloorRpm) continue;
            double windowStd = SeriesUtil.stddev(window);
            double threshold = Math.max(rpmFloorStd, rpmStdFraction * windowMean);
            thresholdSum += threshold;
            totalWindows++;
            if (windowStd > threshold) flaggedWindows++;
        }
        if (totalWindows == 0) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "No windows above idle floor (" + idleFloorRpm + " rpm)", getPluginVersion());
        }
        double flaggedFraction = (double) flaggedWindows / totalWindows;
        double health = 100.0 * (1.0 - SeriesUtil.clamp01(flaggedFraction / flagFractionForZeroHealth));
        double confidence = 100.0 * SeriesUtil.clamp01(totalWindows / minWindowsForConfidence) * 0.9;
        DiagnosticIndexResult.State state = flaggedFraction > flagFractionForFlaggedState
            ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("window_samples", String.valueOf(windowSamples));
        evidence.put("total_windows", String.valueOf(totalWindows));
        evidence.put("flagged_windows", String.valueOf(flaggedWindows));
        evidence.put("flagged_fraction", String.format(Locale.ROOT, "%.4f", flaggedFraction));
        evidence.put("mean_rpm_std_threshold", String.format(Locale.ROOT, "%.1f", thresholdSum / totalWindows));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence, state,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
