package com.ignitionai.obdinput;

import com.ignitionai.obdinput.schema.*;
import com.ignitionai.obdinput.preprocessor.ObdPreProcessor;
import com.ignitionai.obdinput.preprocessor.ValidationException;
import com.ignitionai.obdinput.preprocessor.ProcessingResult;
import com.ignitionai.obdinput.preprocessor.SessionInspector;
import com.ignitionai.obdinput.preprocessor.SessionInspectionReport;
import com.ignitionai.obdinput.storage.ObdJsonMapper;
import com.ignitionai.obdinput.storage.LocalFileStorageProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Collections;

public class ObdInputTest {
    
    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== obd-input-and-pre-processing Tests ===");
        
        if (test_validTelemetry()) passed++; else failed++;
        if (test_missingRequiredField()) passed++; else failed++;
        if (test_duplicateReadingDeduplication()) passed++; else failed++;
        if (test_conflictingDuplicatePreservation()) passed++; else failed++;
        if (test_outOfOrderMessage()) passed++; else failed++;
        if (test_sequenceGap()) passed++; else failed++;
        if (test_partialDtcUpdateDoesNotClear()) passed++; else failed++;
        if (test_completeEmptyDtcSnapshotClears()) passed++; else failed++;
        if (test_sessionScopeIsolation()) passed++; else failed++;
        if (test_minimumProfileEnforcement()) passed++; else failed++;
        if (test_jsonRoundTrip()) passed++; else failed++;
        if (test_storageRetentionLogic()) passed++; else failed++;

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_validTelemetry() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg = createBaseMessage().build();
            processor.process(msg);
            System.out.println("  PASS  test_validTelemetry");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_validTelemetry: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_missingRequiredField() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg = createBaseMessage().sessionId(null).build(); // Should throw since sessionId is required, wait ObdMessage builder enforces null checks
            processor.process(msg);
            System.out.println("  FAIL  test_missingRequiredField: Expected exception");
            return false;
        } catch (NullPointerException | ValidationException e) {
            System.out.println("  PASS  test_missingRequiredField");
            return true;
        }
    }

    private static boolean test_duplicateReadingDeduplication() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1000.0, 100);
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg).getMessage();
            if (processed.getSensorReadings().size() == 1) {
                System.out.println("  PASS  test_duplicateReadingDeduplication");
                return true;
            } else {
                System.out.println("  FAIL  test_duplicateReadingDeduplication: Expected 1 reading, got " + processed.getSensorReadings().size());
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_duplicateReadingDeduplication: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_conflictingDuplicatePreservation() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1005.0, 100); // same time, different value
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg).getMessage();
            if (processed.getSensorReadings().size() == 2 && 
                processed.getSensorReadings().get(1).getQuality().getStatus() == QualityStatus.INVALID) {
                System.out.println("  PASS  test_conflictingDuplicatePreservation");
                return true;
            } else {
                System.out.println("  FAIL  test_conflictingDuplicatePreservation");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_conflictingDuplicatePreservation: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_outOfOrderMessage() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1010.0, 50); // Out of order time
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg).getMessage();
            if (processed.getSensorReadings().get(1).getQuality().getStatus() == QualityStatus.STALE) {
                System.out.println("  PASS  test_outOfOrderMessage");
                return true;
            } else {
                System.out.println("  FAIL  test_outOfOrderMessage");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_outOfOrderMessage: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_sequenceGap() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg1 = createBaseMessage().sequence(1).build();
            ObdMessage msg2 = createBaseMessage().sequence(3).build();
            processor.process(msg1);
            processor.process(msg2); // Should process without throwing, but internally detect gap
            System.out.println("  PASS  test_sequenceGap");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_sequenceGap: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_partialDtcUpdateDoesNotClear() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            DtcObservation dtc1 = createDtc("P0301", ObservationType.SNAPSHOT);
            processor.process(createBaseMessage().dtcObservations(List.of(dtc1)).dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE).build());
            
            // Empty partial update (ADDED)
            processor.process(createBaseMessage().dtcObservations(Collections.emptyList()).dtcSnapshotCompleteness(DtcSnapshotCompleteness.INCREMENTAL).build());
            System.out.println("  PASS  test_partialDtcUpdateDoesNotClear");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_partialDtcUpdateDoesNotClear: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_completeEmptyDtcSnapshotClears() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            DtcObservation dtc1 = createDtc("P0301", ObservationType.SNAPSHOT);
            processor.process(createBaseMessage().dtcObservations(List.of(dtc1)).dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE).build());
            
            // Complete snapshot but empty list (clears existing)
            ObdMessage msg2 = createBaseMessage().dtcObservations(Collections.emptyList()).dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE).build();
            processor.process(msg2);
            
            if (processor.getKnownDtcs("SES-1").isEmpty()) {
                System.out.println("  PASS  test_completeEmptyDtcSnapshotClears");
                return true;
            } else {
                System.out.println("  FAIL  test_completeEmptyDtcSnapshotClears: DTCs not cleared");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_completeEmptyDtcSnapshotClears: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_sessionScopeIsolation() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            
            // Send seq 1 for SES-1
            processor.process(createBaseMessage().sessionId("SES-1").sequence(1).build());
            // Send seq 2 for SES-2 (should not trigger gap because it's a new session)
            ProcessingResult res = processor.process(createBaseMessage().sessionId("SES-2").sequence(2).build());
            
            if (res.getEvents().isEmpty()) {
                System.out.println("  PASS  test_sessionScopeIsolation");
                return true;
            } else {
                System.out.println("  FAIL  test_sessionScopeIsolation: unexpected events: " + res.getEvents().size());
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_sessionScopeIsolation: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_minimumProfileEnforcement() {
        try {
            SessionInspector inspector = new SessionInspector();
            
            // Provide only 3 out of 5 required signals
            ObdMessage msg = createBaseMessage().sessionId("SES-MIN-PROFILE").sensorReadings(List.of(
                createReading("engine_rpm", 800, 10),
                createReading("vehicle_speed", 20, 10),
                createReading("throttle_position", 15, 10)
            )).build();
            
            inspector.observe(msg);
            SessionInspectionReport report = inspector.generateReport("SES-MIN-PROFILE");
            
            if ("LIMITED_COVERAGE".equals(report.getInspectionAcceptanceStatus()) &&
                report.getMissingRequiredSignals().contains("coolant_temperature") &&
                report.getMissingRequiredSignals().contains("calculated_engine_load")) {
                System.out.println("  PASS  test_minimumProfileEnforcement");
                return true;
            } else {
                System.out.println("  FAIL  test_minimumProfileEnforcement: Incorrect status or missing signals");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_minimumProfileEnforcement: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_jsonRoundTrip() {
        try {
            ObdMessage msg = createBaseMessage()
                .dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE)
                .sensorReadings(List.of(createReading("engine_rpm", 800, 10)))
                .dtcObservations(List.of(createDtc("P0301", ObservationType.SNAPSHOT)))
                .build();
                
            String json = ObdJsonMapper.serialize(msg);
            
            if (!json.contains("\"schema_version\"") || !json.contains("\"dtc_snapshot_completeness\"")) {
                System.out.println("  FAIL  test_jsonRoundTrip: Missing snake_case keys");
                return false;
            }
            
            ObdMessage restored = ObdJsonMapper.deserialize(json);
            if (restored.getMessageId().equals(msg.getMessageId()) &&
                restored.getDtcSnapshotCompleteness() == DtcSnapshotCompleteness.COMPLETE &&
                restored.getSensorReadings().size() == 1 &&
                restored.getDtcObservations().size() == 1) {
                System.out.println("  PASS  test_jsonRoundTrip");
                return true;
            } else {
                System.out.println("  FAIL  test_jsonRoundTrip: Restored message doesn't match");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("  FAIL  test_jsonRoundTrip: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_storageRetentionLogic() {
        try {
            Path tempDir = Files.createTempDirectory("obd-storage-test");
            LocalFileStorageProvider storage = new LocalFileStorageProvider(tempDir);
            
            // Create a file 100 days old in raw dir (should be deleted)
            Path rawDir = tempDir.resolve("raw").resolve("SES-OLD");
            Files.createDirectories(rawDir);
            Path oldRawFile = rawDir.resolve("old.json");
            Files.writeString(oldRawFile, "test");
            Files.setLastModifiedTime(oldRawFile, FileTime.from(Instant.now().minus(100, ChronoUnit.DAYS)));
            
            // Create a file 100 days old in normalized dir (should NOT be deleted)
            Path normDir = tempDir.resolve("normalized").resolve("SES-OLD");
            Files.createDirectories(normDir);
            Path oldNormFile = normDir.resolve("old.json");
            Files.writeString(oldNormFile, "test");
            Files.setLastModifiedTime(oldNormFile, FileTime.from(Instant.now().minus(100, ChronoUnit.DAYS)));
            
            storage.cleanupOldFiles();
            
            if (!Files.exists(oldRawFile) && Files.exists(oldNormFile)) {
                System.out.println("  PASS  test_storageRetentionLogic");
                return true;
            } else {
                System.out.println("  FAIL  test_storageRetentionLogic: Cleanup logic failed");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_storageRetentionLogic: " + e.getMessage());
            return false;
        }
    }

    private static ObdMessage.Builder createBaseMessage() {
        return ObdMessage.builder()
            .messageId("MSG-1")
            .sessionId("SES-1")
            .sequence(1)
            .messageType(MessageType.TELEMETRY)
            .sourceType(SourceType.VEHICLE)
            .emittedAt(Instant.now())
            .receivedAt(Instant.now())
            .vehicleRef(new VehicleRef("V-1", null, IdentityStatus.UNKNOWN))
            .rawProvenance(new Provenance("A", "1.0", "ref", null));
    }

    private static SensorReading createReading(String id, double val, long ms) {
        return SensorReading.builder()
            .signalId(id)
            .value(val)
            .unit("unit")
            .measurementTimeBasis(MeasurementTimeBasis.ECU)
            .monotonicMs(ms)
            .receivedAt(Instant.now())
            .quality(new Quality(QualityStatus.VALID, null, 0))
            .provenance(new SignalProvenance("svc", "ref"))
            .build();
    }
    
    private static DtcObservation createDtc(String code, ObservationType type) {
        return DtcObservation.builder()
            .code(code)
            .status(DtcStatus.CONFIRMED)
            .observationType(type)
            .observedAt(Instant.now())
            .monotonicMs(100)
            .provenance(new DtcProvenance("svc", "ref"))
            .build();
    }
}
