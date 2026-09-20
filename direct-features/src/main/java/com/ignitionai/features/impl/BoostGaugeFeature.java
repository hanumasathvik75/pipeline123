package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Boost/vacuum gauge reading: manifold absolute pressure minus barometric (ambient) pressure.
 * Positive means boost above atmospheric, negative means intake vacuum -- the same quantity a
 * mechanical boost gauge shows, derived here from two PIDs already in the signal catalogue. */
public class BoostGaugeFeature implements Feature {
    @Override public String getFeatureId() { return "boost_gauge_kpa"; }
    @Override public String getName() { return "Boost Gauge"; }
    @Override public List<String> getInputs() { return List.of("manifold_absolute_pressure", "barometric_pressure"); }
    @Override public String getUnits() { return "kPa"; }
    @Override public String getFormula() { return "manifold_absolute_pressure - barometric_pressure"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> map = buffer.getReadings("manifold_absolute_pressure", ts, 3000L);
        List<SensorReading> baro = buffer.getReadings("barometric_pressure", ts, 5000L);
        if (map.isEmpty() || baro.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing manifold_absolute_pressure or barometric_pressure");
        double delta = map.get(map.size() - 1).getValue() - baro.get(baro.size() - 1).getValue();
        return new AnalyticalObservation(getFeatureId(), delta, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-MAP-" + ts, "OBS-BARO-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
