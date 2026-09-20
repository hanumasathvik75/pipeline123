package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Actuator-response check: gap between the ECU's commanded throttle position and the throttle
 * plate's actual position. A persistent gap suggests the throttle body isn't tracking its command
 * (sticking, carbon buildup, actuator wear). Reports the raw gap, no flagging logic here. */
public class ThrottleCommandGapFeature implements Feature {
    @Override public String getFeatureId() { return "throttle_command_gap_pct"; }
    @Override public String getName() { return "Commanded-to-Actual Throttle Gap"; }
    @Override public List<String> getInputs() { return List.of("commanded_throttle_position", "throttle_position"); }
    @Override public String getUnits() { return "%"; }
    @Override public String getFormula() { return "abs(commanded_throttle_position - throttle_position)"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> commanded = buffer.getReadings("commanded_throttle_position", ts, 1000L);
        List<SensorReading> actual = buffer.getReadings("throttle_position", ts, 1000L);
        if (commanded.isEmpty() || actual.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing commanded_throttle_position or throttle_position");
        double gap = Math.abs(commanded.get(commanded.size() - 1).getValue() - actual.get(actual.size() - 1).getValue());
        return new AnalyticalObservation(getFeatureId(), gap, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-CMDTHROTTLE-" + ts, "OBS-THROTTLE-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
