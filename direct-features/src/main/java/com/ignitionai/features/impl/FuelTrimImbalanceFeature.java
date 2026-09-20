package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

/** Bank-to-bank fuel trim imbalance: |combined bank1 trim - combined bank2 trim|. A real, useful
 * plausibility check independent of the Fuel diagnostic index (which only looks at bank1) --
 * persistent bank imbalance points at a bank-specific problem (one bank's injectors, one O2
 * sensor, an intake leak on one side) rather than a whole-engine fuel-system issue. */
public class FuelTrimImbalanceFeature implements Feature {
    @Override public String getFeatureId() { return "fuel_trim_imbalance_pct"; }
    @Override public String getName() { return "Bank-to-Bank Fuel Trim Imbalance"; }
    @Override public List<String> getInputs() { return List.of("short_term_fuel_trim_b1", "long_term_fuel_trim_b1", "short_term_fuel_trim_b2", "long_term_fuel_trim_b2"); }
    @Override public String getUnits() { return "%"; }
    @Override public String getFormula() { return "abs((stft_b1+ltft_b1) - (stft_b2+ltft_b2))"; }
    @Override public String getWindow() { return "instantaneous"; }
    @Override public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");
        List<SensorReading> stft1 = buffer.getReadings("short_term_fuel_trim_b1", ts, 5000L);
        List<SensorReading> ltft1 = buffer.getReadings("long_term_fuel_trim_b1", ts, 5000L);
        List<SensorReading> stft2 = buffer.getReadings("short_term_fuel_trim_b2", ts, 5000L);
        List<SensorReading> ltft2 = buffer.getReadings("long_term_fuel_trim_b2", ts, 5000L);
        if (stft1.isEmpty() || ltft1.isEmpty() || stft2.isEmpty() || ltft2.isEmpty())
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Missing one or more bank1/bank2 fuel trim signals");
        double bank1 = last(stft1) + last(ltft1), bank2 = last(stft2) + last(ltft2);
        double imbalance = Math.abs(bank1 - bank2);
        return new AnalyticalObservation(getFeatureId(), imbalance, getUnits(), ts, QualityState.AVAILABLE,
                List.of("OBS-STFT1-" + ts, "OBS-LTFT1-" + ts, "OBS-STFT2-" + ts, "OBS-LTFT2-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }

    private static double last(List<SensorReading> readings) { return readings.get(readings.size() - 1).getValue(); }
}
