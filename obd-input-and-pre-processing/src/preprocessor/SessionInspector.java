package com.ignitionai.obdinput.preprocessor;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.SensorReading;
import java.util.*;

public class SessionInspector {
    
    private static final Set<String> REQUIRED_PROFILE = Set.of(
        "engine_rpm",
        "vehicle_speed",
        "coolant_temperature",
        "calculated_engine_load",
        "throttle_position"
    );

    private final Map<String, Set<String>> sessionObservedSignals = new HashMap<>();

    public void observe(ObdMessage message) {
        String session = message.getSessionId();
        Set<String> observed = sessionObservedSignals.computeIfAbsent(session, k -> new HashSet<>());
        for (SensorReading reading : message.getSensorReadings()) {
            observed.add(reading.getSignalId());
        }
    }

    public SessionInspectionReport generateReport(String sessionId) {
        Set<String> observed = sessionObservedSignals.getOrDefault(sessionId, Collections.emptySet());
        
        List<String> missing = new ArrayList<>();
        for (String req : REQUIRED_PROFILE) {
            if (!observed.contains(req)) {
                missing.add(req);
            }
        }
        
        int providedRequired = REQUIRED_PROFILE.size() - missing.size();
        double coverage = (double) providedRequired / REQUIRED_PROFILE.size();
        
        String status;
        if (coverage == 1.0) {
            status = "ACCEPTED";
        } else if (coverage > 0.0) {
            status = "LIMITED_COVERAGE";
        } else {
            status = "INSUFFICIENT_DATA";
        }
        
        return new SessionInspectionReport(sessionId, coverage, missing, status);
    }
}
