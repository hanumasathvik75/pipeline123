package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Driving-severity characterization: the fraction of the inspection drive spent in hard
 * acceleration or hard braking events, derived from vehicle_speed alone. This is a USAGE
 * indicator, not a mechanical-fault index — it speaks to expected wear/valuation for a pre-owned
 * inspection (how hard was this car driven during the test drive), and its "FLAGGED" state should
 * be read as "notably aggressive test-drive pattern", never as a defect finding. A single-instant
 * speed reading cannot reveal this; only the distribution of acceleration events across the whole
 * session can.
 *
 * Adapted from the rule-based event-detection approach reported in Rimpas et al. 2022 (cited in
 * Michailidis, Panagiotopoulou & Papadakis 2025's OBD-II machine-learning review, 88-95% accuracy
 * on standard PIDs), using named acceleration thresholds rather than a trained classifier, in
 * keeping with IgnitionAI's no-training-data-required contract. Thresholds and rationale live in
 * config/diagnostic-index-config.json under "driving_behavior".
 */
public final class DrivingBehaviorIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "driving_behavior";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Driving Behavior"; }
    @Override public String getPluginVersion() { return "hard-event-fraction.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("vehicle_speed"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double hardAccel = cfg.number(ID, "hard_accel_kmh_per_s");
        double hardDecel = cfg.number(ID, "hard_decel_kmh_per_s");
        double aggressiveForFlagged = cfg.number(ID, "aggressive_fraction_for_flagged_state");
        double aggressiveForZeroHealth = cfg.number(ID, "aggressive_fraction_for_zero_health");
        double minSecondsForConfidence = cfg.number(ID, "min_seconds_for_confidence");

        List<SeriesUtil.Sample> speed = SeriesUtil.series(session, "vehicle_speed");
        if (speed.size() < 5) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 5 valid vehicle_speed samples", getPluginVersion());
        }
        double totalDurationS = (speed.get(speed.size() - 1).t - speed.get(0).t) / 1000.0;
        if (totalDurationS < minSecondsForConfidence / 3.0) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Session shorter than " + (int) (minSecondsForConfidence / 3.0) + " seconds", getPluginVersion());
        }

        double aggressiveDurationS = 0;
        int hardAccelEvents = 0, hardDecelEvents = 0;
        for (int i = 1; i < speed.size(); i++) {
            double dt = (speed.get(i).t - speed.get(i - 1).t) / 1000.0;
            if (dt <= 0) continue;
            double rate = (speed.get(i).v - speed.get(i - 1).v) / dt;
            if (rate > hardAccel) { aggressiveDurationS += dt; hardAccelEvents++; }
            else if (rate < -hardDecel) { aggressiveDurationS += dt; hardDecelEvents++; }
        }
        double aggressiveFraction = aggressiveDurationS / totalDurationS;
        double health = 100.0 * (1.0 - SeriesUtil.clamp01(aggressiveFraction / aggressiveForZeroHealth));
        double confidence = 100.0 * SeriesUtil.clamp01(totalDurationS / minSecondsForConfidence) * 0.85;
        DiagnosticIndexResult.State state = aggressiveFraction > aggressiveForFlagged
            ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("interpretation", "usage severity indicator, not a mechanical fault");
        evidence.put("session_duration_s", String.format(Locale.ROOT, "%.0f", totalDurationS));
        evidence.put("aggressive_fraction", String.format(Locale.ROOT, "%.4f", aggressiveFraction));
        evidence.put("hard_accel_events", String.valueOf(hardAccelEvents));
        evidence.put("hard_decel_events", String.valueOf(hardDecelEvents));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence, state,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
