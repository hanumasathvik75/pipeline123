package com.ignitionai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleContextManager {
    
    public VehicleContext initializeContext(String vehicleId, Long timestampMs) {
        Identity identity = new Identity(vehicleId, null, null, null, Identity.IdentityStatus.UNKNOWN, "manager", 0.0);
        Configuration configuration = new Configuration(null, null, null, null, null, null, null, null, null, null, DataState.UNKNOWN);
        OperatingConditions conditions = new OperatingConditions(null, null, null, null, null, null, null, null, OperatingRegime.UNKNOWN, null, new ArrayList<>(), timestampMs, 0L, DataState.UNKNOWN);
        EvidenceQuality quality = new EvidenceQuality(null, null, null, null, null, 0, 0, false, null, null, 0L, "1.0");
        
        return new VehicleContext("1.0", timestampMs, identity, configuration, conditions, quality);
    }

    public VehicleContext transitionContext(VehicleContext previousContext, Map<String, Double> newObservations, Long currentTimestampMs) {
        if (previousContext.getContextTimestampMs() != null && currentTimestampMs < previousContext.getContextTimestampMs()) {
            throw new IllegalArgumentException("Timestamps must be monotonic. Current: " + currentTimestampMs + " Previous: " + previousContext.getContextTimestampMs());
        }

        // Resolve operating regime
        OperatingRegime currentRegime = determineRegime(newObservations);
        OperatingRegime previousRegime = previousContext.getOperatingConditions().getOperatingRegime();
        OperatingRegime transition = (currentRegime != previousRegime) ? currentRegime : null;

        List<OperatingRegime> history = new ArrayList<>(previousContext.getOperatingConditions().getTransitionHistory());
        if (transition != null) {
            history.add(transition);
        }

        Boolean engineRunning = newObservations.containsKey("engine_rpm") && newObservations.get("engine_rpm") > 0;
        
        Long elapsed = 0L;
        Long regimeStartMs = currentTimestampMs;
        if (previousContext.getContextTimestampMs() != null) {
            elapsed = previousContext.getOperatingConditions().getElapsedSessionTimeMs() + (currentTimestampMs - previousContext.getContextTimestampMs());
            regimeStartMs = (transition != null) ? currentTimestampMs : previousContext.getOperatingConditions().getCurrentRegimeStartTimeMs();
        }

        OperatingConditions newConditions = new OperatingConditions(
            engineRunning,
            newObservations.get("engine_rpm"),
            newObservations.getOrDefault("vehicle_speed", newObservations.get("vehicle_speed_kph")),
            newObservations.get("calculated_engine_load"),
            newObservations.get("throttle_position"),
            newObservations.getOrDefault("coolant_temperature", newObservations.get("engine_coolant_temperature")),
            newObservations.get("intake_air_temperature"),
            newObservations.get("ambient_air_temperature"),
            currentRegime,
            transition,
            history,
            regimeStartMs,
            elapsed,
            DataState.KNOWN
        );

        return new VehicleContext(
            previousContext.getContextVersion(),
            currentTimestampMs,
            previousContext.getIdentity(),
            previousContext.getConfiguration(),
            newConditions,
            previousContext.getEvidenceQuality()
        );
    }

    private OperatingRegime determineRegime(Map<String, Double> obs) {
        Double rpm = obs.get("engine_rpm");
        Double speed = obs.getOrDefault("vehicle_speed", obs.get("vehicle_speed_kph"));
        
        if (rpm == null) return OperatingRegime.UNKNOWN;
        if (rpm == 0) return OperatingRegime.ENGINE_OFF;
        if (speed != null && speed == 0 && rpm > 0) return OperatingRegime.WARM_IDLE;
        if (speed != null && speed > 0) return OperatingRegime.STEADY_CRUISE;
        return OperatingRegime.UNKNOWN;
    }
}
