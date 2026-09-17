package com.ignitionai.uncertainty;

import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.VehicleContext;

public interface UncertaintyModelPlugin {
    String getModelId();
    String getModelVersion();
    
    /**
     * Calculates the uncertainty bounds (+/- value) for a given observation and expected value.
     */
    Double calculateUncertainty(AnalyticalObservation observation, Double expectedValue, VehicleContext context);
}
