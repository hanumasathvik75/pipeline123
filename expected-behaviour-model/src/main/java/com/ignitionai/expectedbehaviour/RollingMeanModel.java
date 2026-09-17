package com.ignitionai.expectedbehaviour;

import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.features.SensorReading;

import java.util.List;

public class RollingMeanModel implements ExpectedBehaviourModelPlugin {
    
    private final long windowSizeMs;

    public RollingMeanModel(long windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
    }

    @Override
    public String getModelId() {
        return "rolling_mean_" + (windowSizeMs / 1000) + "s";
    }

    @Override
    public String getModelVersion() {
        return "1.0";
    }
    
    @Override
    public Double calculateExpectedValue(AnalyticalObservation observation, VehicleContext context, WindowBuffer buffer) {
        String signal = observation.getObservationId();
        List<SensorReading> readings = buffer.getReadings(signal, observation.getTimestampMs(), windowSizeMs);
        
        if (readings.isEmpty()) {
            return null;
        }
        
        double sum = 0;
        int count = 0;
        for (SensorReading r : readings) {
            if (r.getTimestampMs() < observation.getTimestampMs()) {
                sum += r.getValue();
                count++;
            }
        }
        
        if (count == 0) {
            return null;
        }
        
        return sum / count;
    }
}
