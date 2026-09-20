package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Plausibility check: how far the throttle plate position is from the driver's pedal position.
 * A persistently large gap (beyond normal drive-by-wire mapping/lag) can indicate a throttle body
 * or actuator problem -- this Feature reports the raw gap, it does not itself flag anything. */
public class PedalThrottleGapFeature implements Feature {
    @Override public String getFeatureId() { return "pedal_throttle_gap_pct"; }
    @Override public String getName() { return "Pedal-to-Throttle Gap"; }
    @Override public List<String> getInputs() { return List.of("accelerator_pedal_position", "throttle_position"); }
    @Override public String getUnits() { return "%"; }
    @Override public String getFormula() { return "abs(accelerator_pedal_position - throttle_position)"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> pedal = buffer.getReadings("accelerator_pedal_position", ts, 1000L);
        List<SensorReading> throttle = buffer.getReadings("throttle_position", ts, 1000L);
        if (pedal.isEmpty() || throttle.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing accelerator_pedal_position or throttle_position");
        double gap = Math.abs(pedal.get(pedal.size() - 1).getValue() - throttle.get(throttle.size() - 1).getValue());
        return new AnalyticalObservation(getFeatureId(), gap, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-PEDAL-" + ts, "OBS-THROTTLE-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
