package tools.pipeline_runner;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.SensorReading;
import com.ignitionai.obdinput.schema.QualityStatus;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.VehicleContextManager;

import java.util.List;
import java.util.Map;

public class IntegrationAdapter {

    public static VehicleContext ingestMessage(
            ObdMessage msg,
            WindowBuffer buffer,
            VehicleContext currentContext,
            VehicleContextManager contextManager,
            Map<String, Double> latestObs,
            Map<String, AnalyticalObservation> rawObs) {
            
        VehicleContext context = currentContext;

        if (msg.getSensorReadings() != null) {
            for (SensorReading reading : msg.getSensorReadings()) {
                String signalId = reading.getSignalId();
                Double value = reading.getValue();
                Long ts = reading.getMonotonicMs();
                String unit = reading.getUnit();
                
                // Add to Feature window buffer
                buffer.addReading(new com.ignitionai.features.SensorReading(signalId, value, ts, unit));
                
                // Update latest observations for context transitions
                latestObs.put(signalId, value);
                
                // Map Phase 2 QualityStatus to Phase 3 QualityState
                QualityState state = QualityState.AVAILABLE;
                if (reading.getQuality() != null) {
                    QualityStatus qs = reading.getQuality().getStatus();
                    if (qs == QualityStatus.VALID) state = QualityState.AVAILABLE;
                    else if (qs == QualityStatus.MISSING) state = QualityState.MISSING_INPUTS;
                    else if (qs == QualityStatus.UNSUPPORTED) state = QualityState.UNSUPPORTED;
                    else if (qs == QualityStatus.COMMUNICATION_ERROR) state = QualityState.EVALUATION_ERROR;
                    else state = QualityState.INSUFFICIENT_DATA;
                }
                
                String prov = "adapter";
                if (msg.getRawProvenance() != null && msg.getRawProvenance().getAdapterId() != null) {
                    prov = msg.getRawProvenance().getAdapterId();
                }
                
                // Create Raw AnalyticalObservation for direct use in Virtual Sensors
                AnalyticalObservation raw = new AnalyticalObservation(
                    signalId, value, unit, ts, state, 
                    List.of(msg.getMessageId()), 
                    context.getContextVersion(), 
                    "1.0", "0", prov
                );
                rawObs.put(signalId, raw);
                
                // Transition context per reading (simulating real-time)
                context = contextManager.transitionContext(context, latestObs, ts);
            }
        }
        
        return context;
    }
}
