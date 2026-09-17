package com.ignitionai.expectedbehaviour;

import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;
import com.ignitionai.features.WindowBuffer;

public class DeterministicBaselineModel implements ExpectedBehaviourModelPlugin {
    
    @Override
    public String getModelId() {
        return "deterministic_baseline";
    }

    @Override
    public String getModelVersion() {
        return "1.0";
    }
    
    @Override
    public Double calculateExpectedValue(AnalyticalObservation observation, VehicleContext context, WindowBuffer buffer) {
        String signal = observation.getObservationId();
        OperatingRegime regime = context.getOperatingConditions().getOperatingRegime();
        
        // Very basic mapping for Phase 4 MVP
        if (regime == OperatingRegime.WARM_IDLE) {
            switch (signal) {
                case "engine_rpm": return 800.0;
                case "vehicle_speed_kph": return 0.0;
                case "engine_coolant_temperature": return 90.0;
            }
        }
        else if (regime == OperatingRegime.STEADY_CRUISE) {
            switch (signal) {
                case "engine_rpm": return 2000.0;
                case "vehicle_speed_kph": return 100.0;
                case "engine_coolant_temperature": return 95.0;
            }
        }
        else if (regime == OperatingRegime.ENGINE_OFF) {
            switch (signal) {
                case "engine_rpm": return 0.0;
                case "vehicle_speed_kph": return 0.0;
            }
        }
        
        return null; // Model doesn't have an expected value for this state
    }
}
