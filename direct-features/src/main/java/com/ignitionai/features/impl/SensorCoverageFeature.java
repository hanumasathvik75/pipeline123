package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;
import java.util.Set;

/** Fraction of this pipeline's known raw-signal catalogue that produced at least one valid
 * reading this session -- a coarse "how well-instrumented was this vehicle/adapter" indicator,
 * distinct from SignalQualityScore (which measures quality of the signals actually received, not
 * how many of the catalogue's signals showed up at all). */
public class SensorCoverageFeature implements Feature {
    /** The raw PIDs this pipeline currently has a consumer (Feature or DiagnosticIndexPlugin) for.
     * Kept here rather than computed dynamically since "the catalogue" is a design-time constant,
     * not something derivable from one session's own data. */
    private static final Set<String> KNOWN_CATALOGUE = Set.of(
        "engine_rpm", "vehicle_speed", "coolant_temperature", "calculated_engine_load", "throttle_position",
        "short_term_fuel_trim_b1", "long_term_fuel_trim_b1", "short_term_fuel_trim_b2", "long_term_fuel_trim_b2",
        "control_module_voltage", "manifold_absolute_pressure", "mass_airflow_rate", "timing_advance",
        "oil_temperature", "intake_air_temperature", "barometric_pressure", "ambient_air_temperature",
        "fuel_rate_lph", "fuel_level", "accelerator_pedal_position", "commanded_throttle_position",
        "o2_bank1_sensor2_voltage", "o2_bank1_sensor2_stft");

    @Override public String getFeatureId() { return "sensor_coverage_pct"; }
    @Override public String getName() { return "Sensor Coverage"; }
    @Override public List<String> getInputs() { return List.of(); }
    @Override public String getUnits() { return "%"; }
    @Override public String getFormula() { return "100 * |observed signals in catalogue| / |catalogue|"; }
    @Override public String getWindow() { return "session"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        String cv = context.getContextVersion();
        Set<String> observed = buffer.knownSignalIds();
        long matched = observed.stream().filter(KNOWN_CATALOGUE::contains).count();
        double pct = 100.0 * matched / KNOWN_CATALOGUE.size();
        return new AnalyticalObservation(getFeatureId(), pct, getUnits(), ts, QualityState.AVAILABLE,
                List.of("SESSION-COVERAGE-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "matched=" + matched + "/" + KNOWN_CATALOGUE.size());
    }
}
