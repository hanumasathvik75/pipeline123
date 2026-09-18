package com.ignitionai.virtualsensors;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.ignitionai.context.VehicleContextManager;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;

public class VirtualSensorsTest {
    public static void main(String[] args) {
        System.out.println("=== virtual-sensors Tests ===");
        
        testPhase3ExitDemonstration();

        System.out.println("\n=== Results: 1 passed, 0 failed ===");
    }

    private static void testPhase3ExitDemonstration() {
        System.out.println("  [Demo] Phase 3 Exit Criteria Execution...");
        
        ManifestLoader loader = new ManifestLoader();
        File dir = new File("virtual-sensors/src/main/resources/sensors");
        List<SensorDefinition> defs = loader.loadManifests(dir);
        
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        runtime.loadSensors(defs);
        
        SensorDefinition thermalProxy = runtime.getRegisteredSensors().get("thermal_response_proxy");
        assert thermalProxy != null;
        assert thermalProxy.getUnits().equals("Cel");
        assert thermalProxy.getInputSignalIds().contains("engine_coolant_temperature");
        System.out.println("    -> Discovered and enumerated metadata successfully.");

        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctxOrig = manager.initializeContext("V1", 1000L);
        // Ensure applicator passes
        com.ignitionai.context.Configuration config = new com.ignitionai.context.Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
        VehicleContext ctx = new VehicleContext("1.0", 1000L, ctxOrig.getIdentity(), config, ctxOrig.getOperatingConditions(), ctxOrig.getEvidenceQuality());

        Map<String, AnalyticalObservation> inputs = new HashMap<>();
        inputs.put("engine_coolant_temperature", new AnalyticalObservation("engine_coolant_temperature", 85.0, "degC", 1000L, QualityState.AVAILABLE, List.of(), "v1", "v1", "0", "sim"));
        inputs.put("ambient_air_temperature", new AnalyticalObservation("ambient_air_temperature", 25.0, "degC", 1000L, QualityState.AVAILABLE, List.of(), "v1", "v1", "0", "sim"));
        
        AnalyticalObservation outSuccess = runtime.evaluate("thermal_response_proxy", inputs, ctx, 1000L);
        assert outSuccess.getQualityState() == QualityState.AVAILABLE;
        assert Math.abs(outSuccess.getValue() - 60.0) < 0.001;
        System.out.println("    -> Evaluated successfully with valid inputs.");

        inputs.remove("ambient_air_temperature");
        AnalyticalObservation outMissing = runtime.evaluate("thermal_response_proxy", inputs, ctx, 1000L);
        assert outMissing.getQualityState() == QualityState.MISSING_INPUTS;
        assert outMissing.getValue() == null;
        assert outMissing.getProvenance().contains("ambient_air_temperature");
        System.out.println("    -> Explicit unavailable behavior (MISSING_INPUTS) observed.");

        System.out.println("  PASS  testPhase3ExitDemonstration");
    }
}
