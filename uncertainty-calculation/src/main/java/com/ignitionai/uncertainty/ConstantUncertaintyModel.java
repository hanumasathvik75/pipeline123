package com.ignitionai.uncertainty;

import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.VehicleContext;

public class ConstantUncertaintyModel implements UncertaintyModelPlugin {
    
    private final double baseUncertainty;
    
    public ConstantUncertaintyModel(double baseUncertainty) {
        this.baseUncertainty = baseUncertainty;
    }

    @Override
    public String getModelId() {
        return "constant_uncertainty";
    }

    @Override
    public String getModelVersion() {
        return "1.0";
    }

    @Override
    public Double calculateUncertainty(AnalyticalObservation observation, Double expectedValue, VehicleContext context) {
        // A more advanced model would use regime or signal ID to scale uncertainty.
        // For MVP, we use a simple mapping.
        String signal = observation.getObservationId();
        if ("engine_rpm".equals(signal)) return Math.max(50.0, baseUncertainty);
        if ("vehicle_speed_kph".equals(signal)) return Math.max(2.0, baseUncertainty);
        if ("engine_coolant_temperature".equals(signal)) return Math.max(5.0, baseUncertainty);
        
        return baseUncertainty;
    }
}
