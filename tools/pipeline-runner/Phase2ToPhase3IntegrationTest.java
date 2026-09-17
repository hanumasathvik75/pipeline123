package tools.pipeline_runner;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdgenerator.config.ScenarioType;
import com.ignitionai.obdgenerator.generator.GenerationResult;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;

import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.obd.AnalyticalObservation;

import java.io.File;
import java.util.*;

public class Phase2ToPhase3IntegrationTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("=== Phase 2 -> Phase 3 End-to-End Integration Tests ===");
        
        if (test_fullEndToEndPipeline()) passed++; else failed++;
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_fullEndToEndPipeline() {
        try {
            // 1. Setup Phase 2 Generator (Deterministic City Driving)
            GeneratorConfig genConfig = new GeneratorConfig();
            genConfig.setVehicleId("VEH-INTEG-001");
            genConfig.setSupportedSignals(List.of("engine_rpm", "vehicle_speed_kph", "engine_coolant_temperature", "ambient_air_temperature"));
            genConfig.setDurationMs(30000); // 30 seconds
            genConfig.setRandomSeed(12345);
            genConfig.setScenarioType(ScenarioType.CITY_DRIVING);
            // Simulate some missingness for testing Quality mapping
            genConfig.setMissingnessProbability(0.01); 
            
            ObdGenerator generator = new ObdGenerator(genConfig);
            GenerationResult genResult = generator.generate();
            
            if (genResult.getPublicStream().isEmpty()) {
                throw new Exception("Generator produced no data");
            }
            
            // 2. Initialize Phase 3 Architecture
            VehicleContextManager contextManager = new VehicleContextManager();
            VehicleContext contextOrig = contextManager.initializeContext("VEH-INTEG-001", 0L);
            Configuration config = new Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
            VehicleContext context = new VehicleContext("1.0", 0L, contextOrig.getIdentity(), config, contextOrig.getOperatingConditions(), contextOrig.getEvidenceQuality());

            WindowBuffer buffer = new WindowBuffer();
            FeatureCatalogue featureCatalogue = new FeatureCatalogue();

            ManifestLoader loader = new ManifestLoader();
            VirtualSensorRuntime sensorRuntime = new VirtualSensorRuntime();
            sensorRuntime.loadSensors(loader.loadManifests(new File("virtual-sensors/src/main/resources/sensors")));

            Map<String, Double> latestObs = new HashMap<>();
            Map<String, AnalyticalObservation> rawObs = new HashMap<>();
            
            long lastTs = 0;
            
            // 3. Process the Generated Phase 2 Stream through the Integration Adapter
            for (ObdMessage msg : genResult.getPublicStream()) {
                context = IntegrationAdapter.ingestMessage(msg, buffer, context, contextManager, latestObs, rawObs);
                
                // Track max TS
                if (msg.getSensorReadings() != null) {
                    for (var r : msg.getSensorReadings()) {
                        if (r.getMonotonicMs() > lastTs) lastTs = r.getMonotonicMs();
                    }
                }
            }
            
            // 4. Run Phase 3 Features and Virtual Sensors on the gathered buffer
            Map<String, AnalyticalObservation> featureOutputs = new HashMap<>(rawObs);
            for (Feature f : featureCatalogue.getAllFeatures()) {
                AnalyticalObservation val = f.calculate(buffer, context);
                if (val.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE) {
                    featureOutputs.put(f.getFeatureId(), val);
                }
            }
            
            int validVirtualSensors = 0;
            for (SensorDefinition def : sensorRuntime.getRegisteredSensors().values()) {
                AnalyticalObservation out = sensorRuntime.evaluate(def.getSensorId(), featureOutputs, context, lastTs);
                if (out.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE) {
                    validVirtualSensors++;
                    // Assert traceability back to source
                    if (out.getSourceObservationReferences().isEmpty()) {
                        throw new Exception("Virtual sensor " + def.getSensorId() + " lost provenance references");
                    }
                }
            }
            
            // Validate Results
            if (rawObs.isEmpty()) throw new Exception("IntegrationAdapter produced no raw observations");
            if (featureOutputs.size() <= rawObs.size()) throw new Exception("No features were computed successfully");
            
            // Verify context version propagation
            AnalyticalObservation sampleRaw = rawObs.values().iterator().next();
            if (sampleRaw.getContextVersion() == null || sampleRaw.getContextVersion().isEmpty()) {
                throw new Exception("Context version was not propagated to analytical observation");
            }
            
            // Verify Phase 2 traceability
            if (!sampleRaw.getSourceObservationReferences().get(0).startsWith("MSG-")) {
                throw new Exception("Source reference does not trace back to Phase 2 message ID");
            }
            
            System.out.println("  PASS  test_fullEndToEndPipeline (Processed " + genResult.getPublicStream().size() + " messages, output " + validVirtualSensors + " virtual sensors)");
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("  FAIL  test_fullEndToEndPipeline: " + e.getMessage());
            return false;
        }
    }
}
