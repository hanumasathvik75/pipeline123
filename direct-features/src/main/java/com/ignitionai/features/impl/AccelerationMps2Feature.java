package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Two-point finite-difference acceleration from vehicle_speed over the last ~2s: (v2-v1)/(t2-t1),
 * converting km/h to m/s first. Simple by design -- a smoothed/regression estimate is a later
 * refinement, not needed for an instantaneous accel/decel-event proxy. */
public class AccelerationMps2Feature implements Feature {
    @Override public String getFeatureId() { return "acceleration_mps2"; }
    @Override public String getName() { return "Longitudinal Acceleration (m/s^2)"; }
    @Override public List<String> getInputs() { return List.of("vehicle_speed"); }
    @Override public String getUnits() { return "m/s^2"; }
    @Override public String getFormula() { return "d(vehicle_speed_mps)/dt over the last ~2s"; }
    @Override public String getWindow() { return "2s"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> readings = buffer.getReadings("vehicle_speed", ts, 2000L);
        if (readings.size() < 2) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        SensorReading first = readings.get(0), last = readings.get(readings.size() - 1);
        double dtSeconds = (last.getTimestampMs() - first.getTimestampMs()) / 1000.0;
        if (dtSeconds <= 0) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Zero time span");
        double accel = ((last.getValue() - first.getValue()) / 3.6) / dtSeconds;
        return new AnalyticalObservation(getFeatureId(), accel, getUnits(), ts, QualityState.AVAILABLE,
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
