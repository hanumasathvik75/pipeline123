package tools.pipeline_runner;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdgenerator.config.ScenarioType;
import com.ignitionai.obdgenerator.generator.GenerationResult;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;

import com.ignitionai.phase4.Phase4Observation;
import com.ignitionai.phase4.AnomalyState;
import com.ignitionai.expectedbehaviour.*;
import com.ignitionai.residual.ResidualCalculator;
import com.ignitionai.uncertainty.*;
import com.ignitionai.anomaly.*;
import com.ignitionai.episode.*;
import com.ignitionai.evidence.*;

import java.util.*;

public class Phase4PipelineRunner {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("=== Phase 4 End-to-End Tests ===");
        
        if (test_anomalyLifecycleAndEvidence()) passed++; else failed++;
        if (test_missingDataHandling()) passed++; else failed++;
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_anomalyLifecycleAndEvidence() {
        try {
            System.out.println("Running test_anomalyLifecycleAndEvidence...");
            // 1. Phase 2 Generation (Hard Acceleration with Fault)
            GeneratorConfig genConfig = new GeneratorConfig();
            genConfig.setVehicleId("VEH-P4-001");
            genConfig.setSupportedSignals(List.of("engine_rpm"));
            genConfig.setDurationMs(20000); // 20s
            genConfig.setRandomSeed(42);
            genConfig.setScenarioType(ScenarioType.HARD_ACCELERATION_WITH_FAULT);
            
            GenerationResult genResult = new ObdGenerator(genConfig).generate();
            
            // 2. Phase 3 Setup
            VehicleContextManager contextManager = new VehicleContextManager();
            VehicleContext context = contextManager.initializeContext("VEH-P4-001", 0L);
            Configuration config = new Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
            context = new VehicleContext("1.0", 0L, context.getIdentity(), config, context.getOperatingConditions(), context.getEvidenceQuality());
            WindowBuffer buffer = new WindowBuffer();
            
            // 3. Phase 4 Setup
            ExpectedBehaviourModelPlugin expectedModel = new RollingMeanModel(5000); // 5s rolling mean
            UncertaintyModelPlugin uncertaintyModel = new ConstantUncertaintyModel(150.0); // 150 RPM uncertainty
            AnomalyDetectorPlugin detector = new RollingWindowDetector(1.0, 2000, 3000); // threshold 1.0, 2s persistence, 3s recovery
            
            EpisodeStore episodeStore = new EpisodeStore();
            EvidenceStore evidenceStore = new EvidenceStore();
            SignalAnomalyTracker tracker = new SignalAnomalyTracker("engine_rpm", "ses-1");
            
            Map<String, Double> latestObs = new HashMap<>();
            
            int anomaliesDetected = 0;
            
            // Run Pipeline
            for (ObdMessage msg : genResult.getPublicStream()) {
                if (msg.getSensorReadings() == null) continue;
                
                for (var reading : msg.getSensorReadings()) {
                    Long ts = reading.getMonotonicMs();
                    Double value = reading.getValue();
                    
                    buffer.addReading(new com.ignitionai.features.SensorReading("engine_rpm", value, ts, reading.getUnit()));
                    latestObs.put("engine_rpm", value);
                    context = contextManager.transitionContext(context, latestObs, ts);
                    
                    AnalyticalObservation obs = new AnalyticalObservation(
                        "engine_rpm", value, reading.getUnit(), ts, QualityState.AVAILABLE, 
                        List.of("MSG-" + msg.getMessageId()), context.getContextVersion(), "1.0", "0", "adapter"
                    );
                    
                    // Phase 4 Logic
                    Double expected = expectedModel.calculateExpectedValue(obs, context, buffer);
                    
                    Double residual = ResidualCalculator.calculateResidual(value, expected);
                    Double uncertainty = expected != null ? uncertaintyModel.calculateUncertainty(obs, expected, context) : null;
                    Double normRes = ResidualCalculator.calculateNormalizedResidual(residual, uncertainty);
                    
                    Phase4Observation p4Obs = new Phase4Observation(
                        "VEH-P4-001", "ses-1", "engine_rpm", obs.getSourceObservationReferences(),
                        value, expected, residual, uncertainty, normRes, null, null, ts,
                        context.getContextVersion(), "1.0", "1.0", new ArrayList<>()
                    );
                    
                    Double score = detector.evaluate(p4Obs, tracker);
                    p4Obs = p4Obs.withAnomalyResult(score, tracker.getCurrentState());
                    
                    if (tracker.getCurrentState() != AnomalyState.NOMINAL) {
                        anomaliesDetected++;
                        EpisodeRecord ep = episodeStore.getOrCreateActiveEpisode("VEH-P4-001", "ses-1", "engine_rpm", ts, score, tracker.getCurrentState());
                        episodeStore.updateEpisode(ep, score, tracker.getCurrentState(), ts);
                        
                        EvidenceRecord ev = new EvidenceRecord(ep.getEpisodeId(), ts, p4Obs);
                        evidenceStore.addEvidence(ev);
                    }
                }
            }
            
            List<EpisodeRecord> allEpisodes = episodeStore.getAllEpisodes();
            if (allEpisodes.isEmpty()) throw new Exception("No episodes generated despite fault injection");
            
            EpisodeRecord mainEpisode = allEpisodes.get(0);
            if (mainEpisode.getMaxAnomalyScore() < 0.5) throw new Exception("Anomaly score did not escalate properly");
            
            List<EvidenceRecord> evidence = evidenceStore.getEvidenceForEpisode(mainEpisode.getEpisodeId());
            if (evidence.isEmpty()) throw new Exception("No evidence gathered for episode");
            
            if (!evidence.get(0).getObservation().getSourceObservationReferences().get(0).startsWith("MSG-")) {
                throw new Exception("Evidence lost Phase 2 message traceability");
            }

            System.out.println("  PASS  test_anomalyLifecycleAndEvidence");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("  FAIL  test_anomalyLifecycleAndEvidence: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_missingDataHandling() {
        try {
            System.out.println("Running test_missingDataHandling...");
            AnomalyDetectorPlugin detector = new ThresholdAnomalyDetector(1.0);
            SignalAnomalyTracker tracker = new SignalAnomalyTracker("test_sig", "ses-1");
            
            Phase4Observation missingObs = new Phase4Observation(
                "VEH-1", "ses-1", "test_sig", List.of(), null, 100.0, null, 10.0, null, null, null, 1000L,
                "v1", "v1", "v1", List.of()
            );
            
            detector.evaluate(missingObs, tracker);
            
            if (tracker.getConsecutiveMissing() != 1) {
                throw new Exception("Detector did not increment missing counter");
            }
            if (tracker.getCurrentState() != AnomalyState.NOMINAL) {
                throw new Exception("Detector changed state on missing data");
            }
            
            System.out.println("  PASS  test_missingDataHandling");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_missingDataHandling: " + e.getMessage());
            return false;
        }
    }
}
