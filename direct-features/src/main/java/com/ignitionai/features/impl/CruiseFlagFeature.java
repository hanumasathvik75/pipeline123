package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Drive-state flag: 1.0 when vehicle_speed has been meaningfully above zero and steady (low
 * variance) over the last 5s -- a simple, self-contained cruise proxy independent of the
 * Diagnostic Index's own throttle/RPM-based Load Response analysis. */
public class CruiseFlagFeature implements Feature {
    private static final double MIN_CRUISE_SPEED_KMH = 15.0;
    private static final double MAX_SPEED_STDDEV_KMH = 3.0;

    @Override public String getFeatureId() { return "cruise_flag"; }
    @Override public String getName() { return "Cruise (Flag)"; }
    @Override public List<String> getInputs() { return List.of("vehicle_speed"); }
    @Override public String getUnits() { return "boolean"; }
    @Override public String getFormula() { return "mean(vehicle_speed) > " + MIN_CRUISE_SPEED_KMH + " and stddev(vehicle_speed) < " + MAX_SPEED_STDDEV_KMH + " over 5s"; }
    @Override public String getWindow() { return "5s"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> readings = buffer.getReadings("vehicle_speed", ts, 5000L);
        if (readings.size() < 3) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        double mean = 0; for (SensorReading r : readings) mean += r.getValue(); mean /= readings.size();
        double variance = 0; for (SensorReading r : readings) variance += Math.pow(r.getValue() - mean, 2); variance /= readings.size();
        double flag = (mean > MIN_CRUISE_SPEED_KMH && Math.sqrt(variance) < MAX_SPEED_STDDEV_KMH) ? 1.0 : 0.0;
        return new AnalyticalObservation(getFeatureId(), flag, getUnits(), ts, QualityState.AVAILABLE,
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
