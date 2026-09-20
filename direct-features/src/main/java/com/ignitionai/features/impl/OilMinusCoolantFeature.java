package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Thermal delta: oil temperature minus coolant temperature. Oil normally runs a little hotter
 * than coolant once warm; a persistently large or negative gap is a plausibility check on the
 * oil-temperature sensor itself, not a fault index (no threshold/flagging logic here). */
public class OilMinusCoolantFeature implements Feature {

    @Override
    public String getFeatureId() { return "oil_minus_coolant_c"; }

    @Override
    public String getName() { return "Oil Minus Coolant Temperature"; }

    @Override
    public List<String> getInputs() { return List.of("oil_temperature", "coolant_temperature"); }

    @Override
    public String getUnits() { return "degC"; }

    @Override
    public String getFormula() { return "oil_temperature - coolant_temperature"; }

    @Override
    public String getWindow() { return "instantaneous"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");

        List<SensorReading> oil = buffer.getReadings("oil_temperature", ts, 5000L);
        List<SensorReading> coolant = buffer.getReadings("coolant_temperature", ts, 5000L);
        if (oil.isEmpty() || coolant.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing oil_temperature or coolant_temperature");

        double delta = oil.get(oil.size() - 1).getValue() - coolant.get(coolant.size() - 1).getValue();

        return new AnalyticalObservation(getFeatureId(), delta, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-OIL-" + ts, "OBS-COOLANT-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
