package com.ignitionai.obdgenerator;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdgenerator.generator.GenerationResult;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.List;
import java.util.Map;

public class GeneratorTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== obd-generator Tests ===");
        
        if (test_generatorDeterminism()) passed++; else failed++;
        if (test_newScenarioViaConfig()) passed++; else failed++;
        if (test_faultOnsetTiming()) passed++; else failed++;
        if (test_groundTruthConsistency()) passed++; else failed++;
        if (test_missingnessAndDrift()) passed++; else failed++;

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_generatorDeterminism() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setVehicleId("VEH-TATA-NEXON-0001");
            config.setSupportedSignals(List.of("engine_rpm"));
            config.setSamplingRatesHz(Map.of("engine_rpm", 10.0));
            config.setDurationMs(1000);
            config.setRandomSeed(12345);
            
            ObdGenerator gen1 = new ObdGenerator(config);
            GenerationResult res1 = gen1.generate();
            
            ObdGenerator gen2 = new ObdGenerator(config);
            GenerationResult res2 = gen2.generate();
            
            if (res1.getPublicStream().size() == res2.getPublicStream().size()) {
                double val1 = res1.getPublicStream().get(0).getSensorReadings().get(0).getValue();
                double val2 = res2.getPublicStream().get(0).getSensorReadings().get(0).getValue();
                if (Double.compare(val1, val2) == 0) {
                    System.out.println("  PASS  test_generatorDeterminism");
                    return true;
                }
            }
            System.out.println("  FAIL  test_generatorDeterminism");
            return false;
        } catch (Exception e) {
            System.out.println("  FAIL  test_generatorDeterminism: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_newScenarioViaConfig() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setSupportedSignals(List.of("vehicle_speed"));
            config.setDurationMs(500);
            config.setRandomSeed(999);
            config.setMissingnessProbability(1.0); // 100% missing data
            
            ObdGenerator gen = new ObdGenerator(config);
            GenerationResult res = gen.generate();
            
            if (res.getPublicStream().isEmpty()) { // since all missing, empty output
                System.out.println("  PASS  test_newScenarioViaConfig");
                return true;
            } else {
                System.out.println("  FAIL  test_newScenarioViaConfig: Expected empty stream");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_newScenarioViaConfig: " + e.getMessage());
            return false;
        }
    }
    private static boolean test_faultOnsetTiming() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setVehicleId("TEST-VEH-1");
            config.setSupportedSignals(List.of("engine_rpm"));
            config.setDurationMs(20000);
            config.setRandomSeed(123);
            config.setScenarioType(com.ignitionai.obdgenerator.config.ScenarioType.HARD_ACCELERATION_WITH_FAULT);
            
            ObdGenerator gen = new ObdGenerator(config);
            GenerationResult res = gen.generate();
            
            boolean faultFoundBefore15s = false;
            boolean faultFoundAfter15s = false;
            
            for (ObdMessage msg : res.getPublicStream()) {
                long t = msg.getEmittedAt().toEpochMilli() - java.time.Instant.parse("2026-09-17T10:00:00Z").toEpochMilli();
                boolean hasFault = msg.getDtcObservations() != null && msg.getDtcObservations().stream().anyMatch(d -> d.getCode().equals("P0301"));
                if (t < 15000 && hasFault) faultFoundBefore15s = true;
                if (t >= 15000 && hasFault) faultFoundAfter15s = true;
            }
            
            if (!faultFoundBefore15s && faultFoundAfter15s) {
                System.out.println("  PASS  test_faultOnsetTiming");
                return true;
            } else {
                throw new Exception("Fault timing wrong. Before 15s: " + faultFoundBefore15s + ", After 15s: " + faultFoundAfter15s);
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_faultOnsetTiming: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_groundTruthConsistency() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setVehicleId("TEST-VEH-2");
            config.setSupportedSignals(List.of("engine_rpm"));
            config.setDurationMs(2000);
            config.setRandomSeed(456);
            config.setScenarioType(com.ignitionai.obdgenerator.config.ScenarioType.CITY_DRIVING);
            
            ObdGenerator gen = new ObdGenerator(config);
            GenerationResult res = gen.generate();
            
            for (int i = 0; i < res.getPublicStream().size(); i++) {
                ObdMessage msg = res.getPublicStream().get(i);
                com.ignitionai.obdgenerator.generator.GroundTruthRecord truth = res.getGroundTruth().get(i);
                
                double publicVal = msg.getSensorReadings().get(0).getValue();
                double trueVal = truth.getTrueValues().get("engine_rpm");
                
                if (Math.abs(publicVal - trueVal) > 0.001) throw new Exception("Public value differs from true value with no noise");
            }
            
            System.out.println("  PASS  test_groundTruthConsistency");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_groundTruthConsistency: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_missingnessAndDrift() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setVehicleId("TEST-VEH-3");
            config.setSupportedSignals(List.of("engine_rpm"));
            config.setDurationMs(10000);
            config.setRandomSeed(789);
            config.setDriftPerSecond(Map.of("engine_rpm", 5.0)); // 5 rpm per second drift
            
            ObdGenerator gen = new ObdGenerator(config);
            GenerationResult res = gen.generate();
            
            double firstVal = res.getPublicStream().get(0).getSensorReadings().get(0).getValue();
            double lastVal = res.getPublicStream().get(res.getPublicStream().size()-1).getSensorReadings().get(0).getValue();
            
            if (lastVal > firstVal + 40.0) { // roughly 50 drift over 10s
                System.out.println("  PASS  test_missingnessAndDrift");
                return true;
            } else {
                throw new Exception("Drift not applied correctly. First: " + firstVal + " Last: " + lastVal);
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_missingnessAndDrift: " + e.getMessage());
            return false;
        }
    }
}
