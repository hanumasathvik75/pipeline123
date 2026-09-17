package com.ignitionai.expectedbehaviour;

import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.features.WindowBuffer;

public interface ExpectedBehaviourModelPlugin {
    String getModelId();
    String getModelVersion();
    
    /**
     * Calculates the expected value for a given signal observation.
     * Returns null if no expected value can be formulated (e.g. insufficient data, missing inputs).
     */
    Double calculateExpectedValue(AnalyticalObservation observation, VehicleContext context, WindowBuffer buffer);
}
