package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Pure unit conversion: engine_rpm to angular velocity (rad/s), 2*pi/60 per RPM. */
public class RpmRadPerSecFeature implements Feature {
    @Override public String getFeatureId() { return "rpm_rad_per_sec"; }
    @Override public String getName() { return "Engine Angular Velocity (rad/s)"; }
    @Override public List<String> getInputs() { return List.of("engine_rpm"); }
    @Override public String getUnits() { return "rad/s"; }
    @Override public String getFormula() { return "engine_rpm * 2 * PI / 60"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 1000L);
        if (readings.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No readings");
        SensorReading latest = readings.get(readings.size() - 1);
        double radPerSec = latest.getValue() * 2 * Math.PI / 60.0;
        return new AnalyticalObservation(getFeatureId(), radPerSec, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-RPM-" + latest.getTimestampMs()), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
