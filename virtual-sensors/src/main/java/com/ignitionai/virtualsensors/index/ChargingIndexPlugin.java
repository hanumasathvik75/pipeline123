package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Charging-system stability: is the alternator/regulator holding the 12V system inside its
 * normal running band, and is it noisy (ripple) doing so. Ripple is the median absolute
 * sample-to-sample step, a robust noise proxy that isn't thrown off by one or two real
 * load-switching transients (headlights, blower motor) the way a plain std-dev would be. All
 * thresholds live in config/diagnostic-index-config.json under "charging".
 */
public final class ChargingIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "charging";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Charging"; }
    @Override public String getPluginVersion() { return "voltage-band-and-ripple.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("control_module_voltage"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double minNormalV = cfg.number(ID, "min_normal_v");
        double maxNormalV = cfg.number(ID, "max_normal_v");
        double rippleNormalV = cfg.number(ID, "ripple_normal_v");
        double rippleFlagV = cfg.number(ID, "ripple_flag_v");

        List<SeriesUtil.Sample> voltage = SeriesUtil.series(session, "control_module_voltage");
        if (voltage.size() < 10) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 10 valid control_module_voltage samples", getPluginVersion());
        }
        double meanV = voltage.stream().mapToDouble(s -> s.v).average().orElse(0);
        List<Double> steps = new java.util.ArrayList<>();
        for (int i = 1; i < voltage.size(); i++) steps.add(Math.abs(voltage.get(i).v - voltage.get(i - 1).v));
        double ripple = SeriesUtil.median(steps);

        double health = 100.0;
        boolean flagged = false;
        if (meanV < minNormalV || meanV > maxNormalV) {
            double distance = meanV < minNormalV ? minNormalV - meanV : meanV - maxNormalV;
            health -= Math.min(70, distance * 40);
            flagged = true;
        }
        if (ripple > rippleFlagV) { health -= 30; flagged = true; }
        else if (ripple > rippleNormalV) { health -= 10; }
        health = Math.max(0, Math.min(100, health));
        double confidence = 100.0 * SeriesUtil.clamp01(voltage.size() / 200.0) * 0.9;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("mean_voltage_v", String.format(Locale.ROOT, "%.3f", meanV));
        evidence.put("acceptable_range_v", "[" + minNormalV + ", " + maxNormalV + "]");
        evidence.put("median_step_ripple_v", String.format(Locale.ROOT, "%.3f", ripple));
        evidence.put("samples", String.valueOf(voltage.size()));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence,
            flagged ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
