package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.virtualsensors.index.DiagnosticIndexConfig;

import java.util.List;

/** Per-instant hard-acceleration flag, using the SAME threshold as the session-level Driving
 * Behavior diagnostic index (config/diagnostic-index-config.json, driving_behavior.hard_accel_kmh_per_s)
 * rather than a second, possibly-drifting copy of the number. */
public class AccelEventFlagFeature implements Feature {
    @Override public String getFeatureId() { return "accel_event_flag"; }
    @Override public String getName() { return "Hard Acceleration Event (Flag)"; }
    @Override public List<String> getInputs() { return List.of("vehicle_speed"); }
    @Override public String getUnits() { return "boolean"; }
    @Override public String getFormula() { return "d(vehicle_speed)/dt > driving_behavior.hard_accel_kmh_per_s over 1s"; }
    @Override public String getWindow() { return "1s"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> readings = buffer.getReadings("vehicle_speed", ts, 1000L);
        if (readings.size() < 2) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        SensorReading first = readings.get(0), last = readings.get(readings.size() - 1);
        double dtSeconds = (last.getTimestampMs() - first.getTimestampMs()) / 1000.0;
        if (dtSeconds <= 0) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Zero time span");
        double rateKmhPerS = (last.getValue() - first.getValue()) / dtSeconds;
        double threshold = DiagnosticIndexConfig.get().number("driving_behavior", "hard_accel_kmh_per_s");
        double flag = rateKmhPerS > threshold ? 1.0 : 0.0;
        return new AnalyticalObservation(getFeatureId(), flag, getUnits(), ts, QualityState.AVAILABLE,
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
