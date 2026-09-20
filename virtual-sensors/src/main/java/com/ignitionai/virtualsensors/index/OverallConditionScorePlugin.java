package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Confidence-weighted blend of the 7 mechanical-condition indices (Combustion, Fuel, Thermal,
 * Load Response, Charging, Air Intake Consistency, Timing Compliance) into one overall score at
 * the same resolution VHI operates at, but built from indices that are actually live and reactive
 * -- this is the structural fix for the gap identified earlier this session: VHI could read
 * 100/EXCELLENT while the Diagnostic Index had real flagged findings, because the two systems
 * never talked to each other. Driving Behavior and Signal Quality are deliberately excluded --
 * they characterise usage and data quality, not mechanical condition.
 *
 * Implementation note: this plugin evaluates its 7 component plugins directly rather than reading
 * their results from {@link DiagnosticIndexRuntime} (which has no mechanism to pass one plugin's
 * output to another). Each component index's computation is cheap pure-Java session analysis, so
 * the resulting duplicate evaluation is an accepted, minor cost, not a correctness concern -- the
 * alternative would be changing the DiagnosticIndexPlugin contract for every existing plugin.
 */
public final class OverallConditionScorePlugin implements DiagnosticIndexPlugin {
    private static final String ID = "overall_condition";

    private static final List<DiagnosticIndexPlugin> COMPONENT_PLUGINS = List.of(
        new CombustionIndexPlugin(), new FuelIndexPlugin(), new ThermalIndexPlugin(),
        new LoadResponseIndexPlugin(), new ChargingIndexPlugin(),
        new AirIntakeConsistencyIndexPlugin(), new TimingComplianceIndexPlugin());

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Overall Condition"; }
    @Override public String getPluginVersion() { return "confidence-weighted-blend.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("(blend of all mechanical-condition indices)"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        double weightedHealthSum = 0, weightSum = 0;
        int flaggedCount = 0, supportedCount = 0;
        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();

        for (DiagnosticIndexPlugin p : COMPONENT_PLUGINS) {
            DiagnosticIndexResult r;
            try { r = p.evaluate(session, context); } catch (Exception e) { continue; }
            if (r.getHealthPct() == null || r.getConfidencePct() == null) continue;
            supportedCount++;
            double weight = r.getConfidencePct() / 100.0;
            weightedHealthSum += r.getHealthPct() * weight;
            weightSum += weight;
            if (r.getState() == DiagnosticIndexResult.State.FLAGGED) flaggedCount++;
            evidence.put(r.getIndexId() + "_health", String.format(Locale.ROOT, "%.1f", r.getHealthPct()));
            evidence.put(r.getIndexId() + "_state", r.getState().name());
        }

        if (supportedCount == 0 || weightSum <= 0) {
            return DiagnosticIndexResult.insufficientData(ID, getDisplayName(), getRequiredSignalIds(),
                "None of the 7 component indices produced a supported result this session", getPluginVersion());
        }

        double overallHealth = weightedHealthSum / weightSum;
        // Honest confidence: how many of the 7 components actually had enough data to contribute,
        // not a calibrated statistical confidence -- same coverage-based idea used elsewhere in this file.
        double confidence = 100.0 * supportedCount / COMPONENT_PLUGINS.size();
        evidence.put("components_supported", supportedCount + "/" + COMPONENT_PLUGINS.size());
        evidence.put("components_flagged", String.valueOf(flaggedCount));

        DiagnosticIndexResult.State state = flaggedCount > 0 ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;
        return DiagnosticIndexResult.of(ID, getDisplayName(), overallHealth, confidence, state,
            List.of("combustion", "fuel", "thermal", "load_response", "charging", "air_intake_consistency", "timing_compliance"),
            evidence, getPluginVersion());
    }
}
