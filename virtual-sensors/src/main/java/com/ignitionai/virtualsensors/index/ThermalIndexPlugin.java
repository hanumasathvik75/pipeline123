package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Thermal management proxy: warm-up rate and steady-state stability of coolant temperature.
 *
 * warmup_rate = linear-regression slope of coolant_temperature against time, from the first sample
 * until the session first reaches the configured operating temperature (or session end).
 * steady_state_std = sample std-dev of coolant_temperature after operating temperature is reached
 * (thermostat-open steady state; a healthy thermostat should hold this tight). All thresholds live
 * in config/diagnostic-index-config.json under "thermal".
 *
 * A single-instant coolant reading (what a raw scan shows) cannot distinguish "slow to warm up"
 * or "won't hold steady temperature" from a normal one-off reading — both are classic early
 * signs of thermostat, cooling-fan or head-gasket-adjacent issues.
 */
public final class ThermalIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "thermal";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Thermal"; }
    @Override public String getPluginVersion() { return "warmup-and-steady-state.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("coolant_temperature"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double operatingTempC = cfg.number(ID, "operating_temp_c");
        double steadyStdNormalC = cfg.number(ID, "steady_std_normal_c");
        double steadyStdFlagC = cfg.number(ID, "steady_std_flag_c");
        double warmupRateMin = cfg.number(ID, "warmup_rate_min_c_per_min");
        double warmupRateMax = cfg.number(ID, "warmup_rate_max_c_per_min");

        List<SeriesUtil.Sample> coolant = SeriesUtil.series(session, "coolant_temperature");
        if (coolant.size() < 10) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 10 valid coolant_temperature samples", getPluginVersion());
        }
        long t0 = coolant.get(0).t;
        int reachedIdx = -1;
        for (int i = 0; i < coolant.size(); i++) if (coolant.get(i).v >= operatingTempC) { reachedIdx = i; break; }
        boolean reached = reachedIdx >= 0;

        Double warmupRatePerMin = null;
        int warmupEnd = reached ? reachedIdx : coolant.size() - 1;
        if (warmupEnd >= 2) {
            List<Double> xs = new java.util.ArrayList<>(), ys = new java.util.ArrayList<>();
            for (int i = 0; i <= warmupEnd; i++) { xs.add((coolant.get(i).t - t0) / 60000.0); ys.add(coolant.get(i).v); }
            warmupRatePerMin = linearSlope(xs, ys);
        }

        Double steadyStateStd = null;
        int steadyCount = 0;
        if (reached) {
            List<Double> steady = new java.util.ArrayList<>();
            for (int i = reachedIdx; i < coolant.size(); i++) steady.add(coolant.get(i).v);
            steadyStateStd = SeriesUtil.stddev(steady);
            steadyCount = steady.size();
        }

        double health = 100.0;
        boolean flagged = false;
        if (!reached) { health = 60.0; flagged = true; }
        if (steadyStateStd != null) {
            if (steadyStateStd > steadyStdFlagC) { health -= 40; flagged = true; }
            else if (steadyStateStd > steadyStdNormalC) { health -= 15; }
        }
        if (warmupRatePerMin != null && (warmupRatePerMin < warmupRateMin || warmupRatePerMin > warmupRateMax)) {
            health -= 15; flagged = true;
        }
        health = Math.max(0, Math.min(100, health));
        double confidence = 100.0 * SeriesUtil.clamp01(coolant.size() / 200.0) * (reached ? 0.9 : 0.6);

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("operating_temperature_reached", String.valueOf(reached));
        evidence.put("operating_temperature_c", String.format(Locale.ROOT, "%.1f", operatingTempC));
        if (warmupRatePerMin != null) evidence.put("warmup_rate_c_per_min", String.format(Locale.ROOT, "%.3f", warmupRatePerMin));
        if (steadyStateStd != null) {
            evidence.put("steady_state_std_c", String.format(Locale.ROOT, "%.3f", steadyStateStd));
            evidence.put("steady_state_samples", String.valueOf(steadyCount));
        }
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence,
            flagged ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }

    private static double linearSlope(List<Double> xs, List<Double> ys) {
        double mx = SeriesUtil.mean(xs), my = SeriesUtil.mean(ys), num = 0, den = 0;
        for (int i = 0; i < xs.size(); i++) { num += (xs.get(i) - mx) * (ys.get(i) - my); den += (xs.get(i) - mx) * (xs.get(i) - mx); }
        return den == 0 ? 0 : num / den;
    }
}
