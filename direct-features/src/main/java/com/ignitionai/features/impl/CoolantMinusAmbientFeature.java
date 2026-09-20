package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Thermal delta: coolant temperature minus ambient air temperature -- a plausibility/warm-up
 * reference distinct from the Thermal diagnostic index's own warm-up-rate model. No threshold or
 * flagging logic here, just the raw delta. */
public class CoolantMinusAmbientFeature implements Feature {
    @Override public String getFeatureId() { return "coolant_minus_ambient_c"; }
    @Override public String getName() { return "Coolant Minus Ambient Temperature"; }
    @Override public List<String> getInputs() { return List.of("coolant_temperature", "ambient_air_temperature"); }
    @Override public String getUnits() { return "degC"; }
    @Override public String getFormula() { return "coolant_temperature - ambient_air_temperature"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> coolant = buffer.getReadings("coolant_temperature", ts, 5000L);
        List<SensorReading> ambient = buffer.getReadings("ambient_air_temperature", ts, 5000L);
        if (coolant.isEmpty() || ambient.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing coolant_temperature or ambient_air_temperature");
        double delta = coolant.get(coolant.size() - 1).getValue() - ambient.get(ambient.size() - 1).getValue();
        return new AnalyticalObservation(getFeatureId(), delta, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-COOLANT-" + ts, "OBS-AMBIENT-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
