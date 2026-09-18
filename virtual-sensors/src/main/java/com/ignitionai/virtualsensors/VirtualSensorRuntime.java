package com.ignitionai.virtualsensors;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public class VirtualSensorRuntime {
    private Map<String, SensorDefinition> registeredSensors = new HashMap<>();
    private Map<String, VirtualSensorPlugin> plugins = new HashMap<>();

    public void loadSensors(List<SensorDefinition> definitions) {
        for (SensorDefinition def : definitions) {
            if (registeredSensors.containsKey(def.getSensorId())) {
                System.err.println("Duplicate sensor ID detected: " + def.getSensorId());
                continue;
            }
            registeredSensors.put(def.getSensorId(), def);
        }
        validateDAG();
    }

    public void registerPlugin(VirtualSensorPlugin plugin) {
        plugins.put(plugin.getSensorId(), plugin);
    }

    private void validateDAG() {
        List<String> toRemove = new ArrayList<>();
        for (String id : registeredSensors.keySet()) {
            Set<String> visited = new HashSet<>();
            Set<String> recStack = new HashSet<>();
            if (isCyclic(id, visited, recStack)) {
                System.err.println("Cycle detected in dependency graph involving: " + id + ". Quarantining sensor.");
                toRemove.add(id);
            }
        }
        for (String id : toRemove) {
            registeredSensors.remove(id);
        }
    }

    private boolean isCyclic(String current, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(current)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);
        recStack.add(current);

        SensorDefinition def = registeredSensors.get(current);
        if (def != null && def.getInputFeatureIds() != null) {
            for (String dep : def.getInputFeatureIds()) {
                if (registeredSensors.containsKey(dep)) {
                    if (isCyclic(dep, visited, recStack)) return true;
                }
            }
        }
        
        recStack.remove(current);
        return false;
    }

    public AnalyticalObservation evaluate(String sensorId, Map<String, AnalyticalObservation> featureValues, VehicleContext context, Long timestampMs) {
        SensorDefinition def = registeredSensors.get(sensorId);
        String cv = context != null ? context.getContextVersion() : null;
        if (def == null) return AnalyticalObservation.unavailable(sensorId, QualityState.UNSUPPORTED, null, timestampMs, "Sensor not found in registry");

        // Validate vehicle applicability
        if (def.getVehicleApplicability() != null && def.getVehicleApplicability().containsKey("engine_family")) {
            String requiredEngine = def.getVehicleApplicability().get("engine_family");
            if (context != null && context.getConfiguration() != null) {
                String configEngine = context.getConfiguration().getEngineFamily();
                if (configEngine != null && !requiredEngine.equals(configEngine)) {
                    return AnalyticalObservation.unavailable(sensorId, QualityState.NOT_APPLICABLE, def.getSensorVersion(), timestampMs, "Engine family mismatch: " + configEngine);
                }
            }
        }

        // Validate preconditions
        if (def.getOperatingPreconditions() != null) {
            String requiredRegime = def.getOperatingPreconditions().get("engine_running");
            if ("true".equals(requiredRegime) && (context == null || context.getOperatingConditions() == null || !Boolean.TRUE.equals(context.getOperatingConditions().getEngineRunningState()))) {
                return AnalyticalObservation.unavailable(sensorId, QualityState.NOT_APPLICABLE, def.getSensorVersion(), timestampMs, "Precondition failed: engine must be running");
            }
        }

        // Validate dependencies and gather sources
        Map<String, AnalyticalObservation> inputObs = new HashMap<>();
        List<String> sources = new ArrayList<>();
        if (def.getInputSignalIds() != null) {
            for (String dep : def.getInputSignalIds()) {
                AnalyticalObservation reading = featureValues.get(dep);
                if (reading == null) reading = featureValues.get(com.ignitionai.obd.SignalIds.canonical(dep));
                if (reading == null || reading.getQualityState() != QualityState.AVAILABLE) {
                    return AnalyticalObservation.unavailable(sensorId, QualityState.MISSING_INPUTS, def.getSensorVersion(), timestampMs, "Missing required input: " + dep);
                }
                
                // Unit Check (simplified, expecting exactly matching unit if both are defined)
                // Assuming we would look up expected unit in a registry in production
                
                inputObs.put(dep, reading);
                sources.addAll(reading.getSourceObservationReferences());
                if (reading.getSourceObservationReferences().isEmpty()) sources.add(dep + "@" + reading.getTimestampMs());
            }
        }

        try {
            // Plugin evaluation
            if ("plugin".equals(def.getImplementationType())) {
                VirtualSensorPlugin plugin = plugins.get(sensorId);
                if (plugin == null) return AnalyticalObservation.unavailable(sensorId, QualityState.UNSUPPORTED, def.getSensorVersion(), timestampMs, "Missing plugin implementation");
                return plugin.execute(inputObs, context);
            }

            // Formula evaluation
            String formula = def.getFormulaOrModelReference();
            if (formula != null) {
                Double result = MathExpressionEvaluator.evaluate(formula, inputObs);
                if (result != null) {
                    return new AnalyticalObservation(sensorId, result, def.getUnits(), timestampMs, QualityState.AVAILABLE, sources, cv, def.getSensorVersion(), "uncertainty unavailable", "formula evaluation");
                }
            }
            return AnalyticalObservation.unavailable(sensorId, QualityState.NOT_APPLICABLE, def.getSensorVersion(), timestampMs, "Formula could not be evaluated: " + formula);
        } catch (Exception e) {
            return AnalyticalObservation.unavailable(sensorId, QualityState.EVALUATION_ERROR, def.getSensorVersion(), timestampMs, "Evaluation failed: " + e.getMessage());
        }
    }

    public Map<String, SensorDefinition> getRegisteredSensors() {
        return registeredSensors;
    }
}
