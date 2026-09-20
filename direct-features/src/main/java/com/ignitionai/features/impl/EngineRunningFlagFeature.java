package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Simple drive-state flag: 1.0 once RPM clears a running threshold, 0.0 otherwise (cranking or
 * key-off). 200rpm is comfortably below any warm idle speed and above starter-cranking transients. */
public class EngineRunningFlagFeature implements Feature {

    private static final double RUNNING_RPM_THRESHOLD = 200.0;

    @Override
    public String getFeatureId() { return "engine_running_flag"; }

    @Override
    public String getName() { return "Engine Running (Flag)"; }

    @Override
    public List<String> getInputs() { return List.of("engine_rpm"); }

    @Override
    public String getUnits() { return "boolean"; }

    @Override
    public String getFormula() { return "engine_rpm > " + RUNNING_RPM_THRESHOLD; }

    @Override
    public String getWindow() { return "instantaneous"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");

        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 1000L);
        if (readings.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No readings");

        SensorReading latest = readings.get(readings.size() - 1);
        double flag = latest.getValue() > RUNNING_RPM_THRESHOLD ? 1.0 : 0.0;

        return new AnalyticalObservation(getFeatureId(), flag, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-RPM-" + latest.getTimestampMs()), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
