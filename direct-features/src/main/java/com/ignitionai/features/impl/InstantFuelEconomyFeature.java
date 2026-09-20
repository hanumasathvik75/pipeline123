package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Instant fuel economy: vehicle_speed (km/h) divided by fuel_rate (L/h) gives km/L directly,
 * since both are already per-hour rates. Unavailable while stationary or fuel rate is ~0 to avoid
 * a division blow-up rather than reporting a meaningless huge number. */
public class InstantFuelEconomyFeature implements Feature {
    private static final double MIN_FUEL_RATE_LPH = 0.05;

    @Override public String getFeatureId() { return "instant_fuel_economy_kml"; }
    @Override public String getName() { return "Instant Fuel Economy"; }
    @Override public List<String> getInputs() { return List.of("vehicle_speed", "fuel_rate_lph"); }
    @Override public String getUnits() { return "km/L"; }
    @Override public String getFormula() { return "vehicle_speed / fuel_rate_lph"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> speed = buffer.getReadings("vehicle_speed", ts, 1000L);
        List<SensorReading> fuelRate = buffer.getReadings("fuel_rate_lph", ts, 1000L);
        if (speed.isEmpty() || fuelRate.isEmpty()) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing vehicle_speed or fuel_rate_lph");
        double rate = fuelRate.get(fuelRate.size() - 1).getValue();
        if (rate < MIN_FUEL_RATE_LPH) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.NOT_APPLICABLE, getFeatureVersion(), ts, "Fuel rate too low to compute economy (idle/coasting)");
        double kmPerL = speed.get(speed.size() - 1).getValue() / rate;
        return new AnalyticalObservation(getFeatureId(), kmPerL, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-SPEED-" + ts, "OBS-FUELRATE-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
