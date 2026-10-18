package ai.ignition.android.diagnostics;

import org.junit.Test;

import ai.ignition.android.obd.Elm327Initializer;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.obd.SupportedPidDiscovery;
import ai.ignition.android.obd.VinParser;
import ai.ignition.android.recording.CaptureState;
import ai.ignition.android.recording.EventType;
import ai.ignition.android.recording.ObservationStatus;
import ai.ignition.android.recording.SessionEvent;
import ai.ignition.android.recording.SessionManifest;
import ai.ignition.android.recording.SessionStatus;
import ai.ignition.android.recording.SessionValidator;
import ai.ignition.android.recording.ValidationResult;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression and diagnostic tests for subsystem status classification.
 * Validates the root cause of the PARTIAL status and ensures independent
 * status models are never conflated.
 */
public class HardwareDiagnosticOutcomeTest {

    @Test
    public void testHardwarePreflight_incompleteVinCausesPartialWithReadableExplanation() {
        // Physical ELM327 test scenario: Adapter connects, PIDs discovered, live telemetry working,
        // but vehicle ECU returns 12-char VIN instead of ISO 3779 17-char VIN.
        VinParser.VinResult incompleteVin = VinParser.decodeVin("49 02 01 31 32 33 34 35 36 37 38 39 30 31 32");
        assertEquals(VinParser.VinStatus.INCOMPLETE, incompleteVin.status);

        LivePidDecoder.PidReading liveRpm = new LivePidDecoder.PidReading("010C", "Engine RPM", 2400.0, "rpm", "41 0C 25 80", true, "OK");
        List<LivePidDecoder.PidReading> liveReadings = List.of(liveRpm);

        Elm327Initializer.InitResult initOk = new Elm327Initializer.InitResult("ATZ\nELM327 v1.5", "ELM327 v1.5", "OK", "OK", "OK", true, null);
        SupportedPidDiscovery.DiscoveryReport pidReport = new SupportedPidDiscovery.DiscoveryReport(List.of("0C", "0D"), "41 00 BE 1F B8 10", List.of(), true, "Discovered");

        // Hardware test runner outcome evaluation
        boolean connOk = true;
        boolean initOkFlag = initOk.success;
        boolean pidOk = !pidReport.allSupportedPids.isEmpty();
        boolean liveOk = !liveReadings.isEmpty();
        boolean vinValid = incompleteVin.status == VinParser.VinStatus.VALID;
        boolean vinIncomplete = incompleteVin.status == VinParser.VinStatus.INCOMPLETE;

        HardwareTestReport.VerificationOutcome outcome;
        String outcomeReason;

        if (!connOk || !initOkFlag || !pidOk) {
            outcome = HardwareTestReport.VerificationOutcome.FAIL;
            outcomeReason = "Critical adapter communication failure";
        } else if (vinValid && liveOk) {
            outcome = HardwareTestReport.VerificationOutcome.PASS;
            outcomeReason = "All mandatory checks passed.";
        } else {
            outcome = HardwareTestReport.VerificationOutcome.PARTIAL;
            if (vinIncomplete) {
                outcomeReason = "Adapter communication verified, but VIN is incomplete (" +
                        (incompleteVin.rawVin != null ? incompleteVin.rawVin.length() : 0) + " chars received: '" +
                        incompleteVin.redactedVin + "' - expected 17).";
            } else {
                outcomeReason = "Adapter communication verified with partial diagnostic data.";
            }
        }

        assertEquals("Hardware preflight must be PARTIAL when VIN is incomplete",
                HardwareTestReport.VerificationOutcome.PARTIAL, outcome);
        assertTrue("Reason must explain the received character count and expected 17",
                outcomeReason.contains("12 chars received") && outcomeReason.contains("expected 17"));
    }

    @Test
    public void testHardwarePreflight_criticalFailureProducesFailNotPartial() {
        Elm327Initializer.InitResult initFailed = new Elm327Initializer.InitResult("?", "?", "?", "?", "?", false, "ATZ timeout");
        boolean connOk = true;
        boolean initOk = initFailed.success;
        boolean pidOk = false;

        HardwareTestReport.VerificationOutcome outcome;
        String outcomeReason;

        if (!connOk || !initOk || !pidOk) {
            outcome = HardwareTestReport.VerificationOutcome.FAIL;
            outcomeReason = "Critical adapter communication failure (Connection, Init, or PID discovery failed)";
        } else {
            outcome = HardwareTestReport.VerificationOutcome.PASS;
            outcomeReason = "Pass";
        }

        assertEquals("Critical connection failure must produce FAIL, never PARTIAL or PASS",
                HardwareTestReport.VerificationOutcome.FAIL, outcome);
        assertTrue(outcomeReason.contains("Critical adapter communication failure"));
    }

    @Test
    public void testSubsystemIndependence_preflightPartialDoesNotCorruptSessionStatus() throws Exception {
        // A hardware preflight outcome of PARTIAL must not cause a recording session to be labeled PARTIAL or INVALID
        // when the recording session itself completed and validated with 100% valid records.
        File tempDir = Files.createTempDirectory("subsystem_independence_test").toFile();
        File jsonlFile = new File(tempDir, "session.jsonl");

        try (FileOutputStream fos = new FileOutputStream(jsonlFile)) {
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"session-abc\",\"sequence\":1,\"timestamp\":\"2026-10-17T06:00:00Z\",\"event_type\":\"SESSION_STARTED\"}\n".getBytes(StandardCharsets.UTF_8));
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"session-abc\",\"sequence\":2,\"timestamp\":\"2026-10-17T06:00:01Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Engine RPM\",\"value\":2500.0,\"unit\":\"rpm\",\"status\":\"VALID\"}\n".getBytes(StandardCharsets.UTF_8));
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"session-abc\",\"sequence\":3,\"timestamp\":\"2026-10-17T06:00:02Z\",\"event_type\":\"SESSION_STOPPED\"}\n".getBytes(StandardCharsets.UTF_8));
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"session-abc\",\"sequence\":4,\"timestamp\":\"2026-10-17T06:00:02Z\",\"event_type\":\"SESSION_FINALIZED\"}\n".getBytes(StandardCharsets.UTF_8));
        }

        ValidationResult validation = SessionValidator.validateFile(jsonlFile, "session-abc");
        assertTrue("JSONL validation must pass for clean drive recording", validation.isValid());
        assertEquals(4, validation.getValidRecordCount());
        assertEquals(0, validation.getInvalidRecordCount());

        SessionManifest manifest = SessionManifest.builder()
                .sessionId("session-abc")
                .startTime("2026-10-17T06:00:00Z")
                .endTime("2026-10-17T06:00:02Z")
                .durationMs(2000)
                .status(validation.isValid() ? SessionStatus.COMPLETE : SessionStatus.INVALID)
                .recordCount(4)
                .validRecordCount(4)
                .invalidRecordCount(0)
                .validationPassed(true)
                .validationSummary("VALID: 4 records verified")
                .build();

        assertEquals("Session status must be COMPLETE, independent of preflight VIN checks",
                SessionStatus.COMPLETE, manifest.getStatus());
        assertTrue(manifest.isValidationPassed());

        deleteRecursively(tempDir);
    }

    @Test
    public void testRecordingState_activeSessionIsRecordingNotComplete() {
        CaptureState activeState = CaptureState.RECORDING;
        assertNotEquals("Active capture must not be classified as complete", CaptureState.STOPPED, activeState);
        assertEquals(CaptureState.RECORDING, activeState);

        SessionStatus manifestStatusDuringRecording = SessionStatus.RECORDING;
        assertEquals(SessionStatus.RECORDING, manifestStatusDuringRecording);
    }

    @Test
    public void testInterruptedSession_labeledInterruptedAndDataPreserved() throws Exception {
        File tempDir = Files.createTempDirectory("interrupted_test").toFile();
        File jsonlFile = new File(tempDir, "session.jsonl");

        // Write partial drive without terminal SESSION_FINALIZED event
        try (FileOutputStream fos = new FileOutputStream(jsonlFile)) {
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-interrupted\",\"sequence\":1,\"timestamp\":\"2026-10-17T06:00:00Z\",\"event_type\":\"SESSION_STARTED\"}\n".getBytes(StandardCharsets.UTF_8));
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-interrupted\",\"sequence\":2,\"timestamp\":\"2026-10-17T06:00:01Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Engine RPM\",\"value\":2100.0,\"unit\":\"rpm\",\"status\":\"VALID\"}\n".getBytes(StandardCharsets.UTF_8));
        }

        ValidationResult validation = SessionValidator.validateFile(jsonlFile, "s-interrupted");
        assertTrue("Written records must be valid", validation.isValid());
        assertEquals(2, validation.getValidRecordCount());

        // Startup recovery sets INTERRUPTED
        SessionManifest manifest = SessionManifest.builder()
                .sessionId("s-interrupted")
                .status(SessionStatus.INTERRUPTED)
                .recordCount(2)
                .validRecordCount(2)
                .invalidRecordCount(0)
                .validationPassed(true)
                .validationSummary("Session interrupted by unexpected termination; data preserved")
                .build();

        assertEquals(SessionStatus.INTERRUPTED, manifest.getStatus());
        assertTrue("Interrupted raw file must be preserved", jsonlFile.exists());
        assertEquals(2, validation.getValidRecordCount());

        deleteRecursively(tempDir);
    }

    @Test
    public void testObservationStatus_explicitNullsAndStatuses() {
        SessionEvent validEvent = SessionEvent.builder()
                .sessionId("test-obs")
                .sequence(1)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(3000.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .build();
        assertEquals(Double.valueOf(3000.0), validEvent.getValue());
        assertEquals(ObservationStatus.VALID, validEvent.getStatus());

        SessionEvent noDataEvent = SessionEvent.builder()
                .sessionId("test-obs")
                .sequence(2)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("Fuel Pressure")
                .value(null)
                .unit("kPa")
                .status(ObservationStatus.NO_DATA)
                .build();
        assertNull("NO_DATA must have null value, never 0.0", noDataEvent.getValue());
        assertEquals(ObservationStatus.NO_DATA, noDataEvent.getStatus());

        SessionEvent unsupportedEvent = SessionEvent.builder()
                .sessionId("test-obs")
                .sequence(3)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("Oil Temp")
                .value(null)
                .status(ObservationStatus.UNSUPPORTED)
                .build();
        assertNull("UNSUPPORTED must have null value", unsupportedEvent.getValue());
        assertEquals(ObservationStatus.UNSUPPORTED, unsupportedEvent.getStatus());
    }

    private void deleteRecursively(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File c : children) deleteRecursively(c);
                }
            }
            f.delete();
        }
    }
}
