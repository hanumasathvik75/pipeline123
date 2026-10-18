package ai.ignition.android.recording;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class JsonlSessionRecorderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testBaseDir;
    private SessionRepository repository;

    @Before
    public void setUp() throws Exception {
        testBaseDir = tempFolder.newFolder("ignition_test");
        repository = new SessionRepository(testBaseDir);
    }

    /**
     * Requirement 1: Creating a new session.
     */
    @Test
    public void test1_createNewSession() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        assertNotNull("Writer must be initialized", writer);
        assertFalse("Writer must be open", writer.isClosed());
        assertEquals("Session ID must match", sessionId, writer.getSessionId());

        File sessionDir = repository.getSessionDir(sessionId);
        assertTrue("Session directory must exist", sessionDir.exists() && sessionDir.isDirectory());

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        assertTrue("session.jsonl must exist", jsonlFile.exists());

        File manifestFile = repository.getManifestFile(sessionId);
        assertTrue("manifest.json must exist", manifestFile.exists());

        SessionManifest manifest = repository.getSessionManifest(sessionId);
        assertNotNull("Initial manifest must be readable", manifest);
        assertEquals("Initial status must be RECORDING", SessionStatus.RECORDING, manifest.getStatus());
        assertEquals("Manifest session ID must match", sessionId, manifest.getSessionId());

        writer.close();
    }

    /**
     * Requirement 2: Writing one event.
     */
    @Test
    public void test2_writeOneEvent() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        SessionEvent observation = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(2450.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .source("BLUETOOTH_CLASSIC")
                .build();

        boolean written = writer.writeEvent(observation);
        assertTrue("Event must be accepted and written", written);
        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);
        assertEquals("File must contain 2 lines (SESSION_STARTED + OBD_OBSERVATION)", 2, lines.size());

        SessionEvent parsedObs = SessionEvent.fromJson(lines.get(1));
        assertEquals(EventType.OBD_OBSERVATION, parsedObs.getEventType());
        assertEquals("RPM", parsedObs.getSignalName());
        assertEquals(Double.valueOf(2450.0), parsedObs.getValue());
        assertEquals(ObservationStatus.VALID, parsedObs.getStatus());
    }

    /**
     * Requirement 3: Writing multiple events in order.
     */
    @Test
    public void test3_writeMultipleEventsInOrder() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(1800.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .build());

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("SPEED")
                .value(45.0)
                .unit("km/h")
                .status(ObservationStatus.VALID)
                .build());

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.PHASE_CHANGED)
                .phaseId("PHASE_CITY_CRUISE")
                .build());

        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);
        assertEquals("Should contain 4 events", 4, lines.size());

        long lastSeq = 0;
        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            assertTrue("Sequence must be monotonically increasing (" + event.getSequence() + " > " + lastSeq + ")",
                    event.getSequence() > lastSeq);
            lastSeq = event.getSequence();
        }
    }

    /**
     * Requirement 4: Validating that each JSONL line parses independently.
     */
    @Test
    public void test4_validateEachLineParsesIndependently() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("COOLANT_TEMP")
                .value(88.0)
                .unit("deg_c")
                .status(ObservationStatus.VALID)
                .build());

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.CONNECTION_LOST)
                .errorCategory("TIMEOUT")
                .errorMessage("Adapter connection lost")
                .build());

        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Must parse independently with zero context of prior or subsequent lines
            SessionEvent parsed = SessionEvent.fromJson(line);
            assertNotNull("Line " + (i + 1) + " must parse independently", parsed);
            assertEquals(sessionId, parsed.getSessionId());
            assertNotNull(parsed.getTimestamp());
            assertNotNull(parsed.getEventType());
        }
    }

    /**
     * Requirement 5: Correct newline handling.
     */
    @Test
    public void test5_correctNewlineHandling() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        // Include text that contains a newline in message to test proper escape serialization
        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.CAPTURE_ERROR)
                .errorCategory("PARSE_ERROR")
                .errorMessage("Line 1 error\nLine 2 context\r\nLine 3")
                .build());

        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        byte[] bytes = readAllBytes(jsonlFile);
        String rawContent = new String(bytes, StandardCharsets.UTF_8);

        // Entire file must end with a single newline
        assertTrue("File must end with newline", rawContent.endsWith("\n"));

        // Split by literal newline
        String[] lines = rawContent.split("\n");
        assertEquals("File must contain exactly 2 lines despite embedded newline in message", 2, lines.length);

        for (String line : lines) {
            assertFalse("No line should contain unescaped CR", line.contains("\r"));
            SessionEvent event = SessionEvent.fromJson(line);
            assertNotNull(event);
        }
    }

    /**
     * Requirement 6: Correct timestamp and session ID fields.
     */
    @Test
    public void test6_correctTimestampAndSessionIdFields() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        String before = Instant.now().minusSeconds(1).toString();
        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("SPEED")
                .value(60.0)
                .unit("km/h")
                .status(ObservationStatus.VALID)
                .build());
        String after = Instant.now().plusSeconds(1).toString();

        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);
        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            assertEquals(sessionId, event.getSessionId());

            Instant parsedTime = Instant.parse(event.getTimestamp());
            assertNotNull("Timestamp must be parseable ISO-8601 with timezone", parsedTime);
            assertTrue("Timestamp must be after start window", parsedTime.compareTo(Instant.parse(before)) >= 0);
            assertTrue("Timestamp must be before end window", parsedTime.compareTo(Instant.parse(after)) <= 0);
        }
    }

    /**
     * Requirement 7: Rejecting malformed events safely without crashing.
     */
    @Test
    public void test7_rejectMalformedEvents() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        // 1. Null event
        assertFalse("Null event must be rejected", writer.writeEvent(null));

        // 2. Mismatched session ID
        SessionEvent mismatchedSession = SessionEvent.builder()
                .sessionId("different-session-id")
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(1000.0)
                .status(ObservationStatus.VALID)
                .build();
        assertFalse("Mismatched session ID must be rejected", writer.writeEvent(mismatchedSession));

        // 3. OBD observation with missing signal name
        SessionEvent missingSignal = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .value(1000.0)
                .status(ObservationStatus.VALID)
                .build();
        assertFalse("Missing signal name must be rejected", writer.writeEvent(missingSignal));

        // 4. Missing value for VALID status
        SessionEvent missingVal = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .status(ObservationStatus.VALID)
                .build();
        assertFalse("VALID status without value must be rejected", writer.writeEvent(missingVal));

        // 5. Synthetic value for UNSUPPORTED status (zero-substitution rule)
        SessionEvent syntheticVal = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("ENGINE_LOAD")
                .value(0.0) // Must NOT substitute 0 for unavailable data!
                .status(ObservationStatus.UNSUPPORTED)
                .build();
        assertFalse("Synthetic value for UNSUPPORTED status must be rejected", writer.writeEvent(syntheticVal));

        // 6. PHASE_CHANGED without phaseId
        SessionEvent missingPhase = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.PHASE_CHANGED)
                .build();
        assertFalse("PHASE_CHANGED without phaseId must be rejected", writer.writeEvent(missingPhase));

        // Verify only initial SESSION_STARTED event exists in file
        writer.close();
        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);
        assertEquals("File must only contain the 1 initial valid event", 1, lines.size());
    }

    /**
     * Requirement 8: Flush and close behavior.
     */
    @Test
    public void test8_flushAndCloseBehavior() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        SessionEvent event = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(2200.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .build();

        writer.writeEvent(event);
        writer.flush();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        assertTrue("File must have content after flush", jsonlFile.length() > 0);

        writer.close();
        assertTrue("Writer must be marked closed", writer.isClosed());

        // Subsequent write to closed writer must be rejected cleanly
        try {
            writer.writeEvent(event);
            fail("Writing to closed writer should throw IllegalStateException");
        } catch (IllegalStateException expected) {
            // Success
        }

        // Multiple close calls must be safe (idempotent)
        writer.close();
    }

    /**
     * Requirement 9: Repeated finalization idempotence.
     */
    @Test
    public void test9_repeatedFinalization() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("RPM")
                .value(3000.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .build());

        // First finalization
        SessionManifest manifest1 = repository.finalizeSession(sessionId);
        assertNotNull(manifest1);
        assertEquals(SessionStatus.COMPLETE, manifest1.getStatus());

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        long lengthAfterFirst = jsonlFile.length();
        List<String> linesAfterFirst = readLines(jsonlFile);

        // Second finalization call
        SessionManifest manifest2 = repository.finalizeSession(sessionId);
        assertNotNull(manifest2);
        assertEquals(SessionStatus.COMPLETE, manifest2.getStatus());

        // Must not duplicate lines or change file
        long lengthAfterSecond = jsonlFile.length();
        List<String> linesAfterSecond = readLines(jsonlFile);

        assertEquals("File length must not change on second finalization", lengthAfterFirst, lengthAfterSecond);
        assertEquals("Line count must remain identical", linesAfterFirst.size(), linesAfterSecond.size());
    }

    /**
     * Requirement 10: Manifest record count and file size accuracy.
     */
    @Test
    public void test10_manifestRecordCountAndFileSize() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        for (int i = 1; i <= 5; i++) {
            writer.writeEvent(SessionEvent.builder()
                    .sessionId(sessionId)
                    .eventType(EventType.OBD_OBSERVATION)
                    .signalName("SPEED")
                    .value((double) (i * 10))
                    .unit("km/h")
                    .status(ObservationStatus.VALID)
                    .build());
        }

        SessionManifest manifest = repository.finalizeSession(sessionId);
        File jsonlFile = repository.getSessionJsonlFile(sessionId);

        // Events: 1 SESSION_STARTED + 5 OBD_OBSERVATION + 1 SESSION_STOPPED + 1 SESSION_FINALIZED = 8
        assertEquals(8, manifest.getRecordCount());
        assertEquals(8, manifest.getValidRecordCount());
        assertEquals(0, manifest.getInvalidRecordCount());
        assertEquals(jsonlFile.length(), manifest.getFileSizeBytes());
        assertTrue(manifest.isValidationPassed());
        assertEquals(SessionStatus.COMPLETE, manifest.getStatus());
        assertTrue(manifest.getDurationMs() >= 0);
    }

    /**
     * Requirement 11: Invalid JSONL detection without deleting corrupted evidence.
     */
    @Test
    public void test11_invalidJsonlDetection() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);
        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);

        // Inject corrupted / truncated line into JSONL file
        try (FileWriter fw = new FileWriter(jsonlFile, true)) {
            fw.write("{invalid_json_missing_quotes:\n");
            fw.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"" + sessionId + "\",\"sequence\":-99}\n");
        }

        ValidationResult validation = SessionValidator.validateFile(jsonlFile, sessionId);
        assertFalse("Validation must fail on corrupted lines", validation.isValid());
        assertEquals("Should detect 2 invalid lines", 2, validation.getInvalidRecordCount());
        assertTrue("Valid initial record should still be counted", validation.getValidRecordCount() >= 1);
        assertTrue("File must be preserved on disk", jsonlFile.exists());

        // Finalize session with corrupted file
        SessionManifest manifest = repository.finalizeSession(sessionId);
        assertEquals("Corrupted session must be marked INVALID, not COMPLETE", SessionStatus.INVALID, manifest.getStatus());
        assertFalse(manifest.isValidationPassed());
        assertTrue("Corrupted evidence file must remain intact", jsonlFile.exists());
    }

    /**
     * Requirement 12: File-write failure handling.
     */
    @Test
    public void test12_fileWriteFailureHandling() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        writer.close();

        // Make file read-only to simulate write failure
        boolean readOnlySuccess = jsonlFile.setReadOnly();
        if (readOnlySuccess) {
            try {
                JsonlSessionWriter failingWriter = new JsonlSessionWriter(jsonlFile, sessionId, true);
                failingWriter.writeEvent(SessionEvent.builder()
                        .sessionId(sessionId)
                        .eventType(EventType.OBD_OBSERVATION)
                        .signalName("RPM")
                        .value(1200.0)
                        .status(ObservationStatus.VALID)
                        .build());
                fail("Expected SessionWriteException on read-only file write");
            } catch (SessionWriteException | IOException expected) {
                // Verified: write failure is surfaced explicitly
                assertNotNull(expected.getMessage());
            } finally {
                jsonlFile.setWritable(true);
            }
        }
    }

    /**
     * Requirement 13: Interrupted-session detection.
     */
    @Test
    public void test13_interruptedSessionDetection() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("COOLANT_TEMP")
                .value(75.0)
                .unit("deg_c")
                .status(ObservationStatus.VALID)
                .build());

        // Simulate app crash / kill by closing file writer directly without finalization
        writer.close();

        // Instantiate new repository instance representing a fresh app launch
        SessionRepository newLaunchRepository = new SessionRepository(testBaseDir);
        List<SessionManifest> recovered = newLaunchRepository.detectAndRecoverInterruptedSessions();

        assertEquals("Must discover exactly 1 interrupted session", 1, recovered.size());
        SessionManifest recoveredManifest = recovered.get(0);
        assertEquals(sessionId, recoveredManifest.getSessionId());
        assertEquals(SessionStatus.INTERRUPTED, recoveredManifest.getStatus());
    }

    /**
     * Requirement 14: Preservation of partial data after interruption.
     */
    @Test
    public void test14_preservationOfPartialDataAfterInterruption() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        for (int i = 1; i <= 3; i++) {
            writer.writeEvent(SessionEvent.builder()
                    .sessionId(sessionId)
                    .eventType(EventType.OBD_OBSERVATION)
                    .signalName("RPM")
                    .value(1500.0 + (i * 100))
                    .unit("rpm")
                    .status(ObservationStatus.VALID)
                    .build());
        }
        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        long lengthBefore = jsonlFile.length();
        List<String> linesBefore = readLines(jsonlFile);

        SessionRepository recoveryRepo = new SessionRepository(testBaseDir);
        List<SessionManifest> recovered = recoveryRepo.detectAndRecoverInterruptedSessions();

        assertEquals(1, recovered.size());
        File recoveredJsonl = recoveryRepo.getSessionJsonlFile(sessionId);
        assertEquals("File size must not be truncated or deleted", lengthBefore, recoveredJsonl.length());

        List<String> linesAfter = readLines(recoveredJsonl);
        assertEquals("All partial lines must be preserved", linesBefore.size(), linesAfter.size());
        for (int i = 0; i < linesBefore.size(); i++) {
            assertEquals("Line " + i + " must match", linesBefore.get(i), linesAfter.get(i));
        }
    }

    /**
     * Requirement 15: Ensuring an incomplete session is never marked complete.
     */
    @Test
    public void test15_incompleteSessionNeverMarkedComplete() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);
        writer.close(); // Not finalized, just terminated

        SessionRepository recoveryRepo = new SessionRepository(testBaseDir);
        recoveryRepo.detectAndRecoverInterruptedSessions();

        SessionManifest manifest = recoveryRepo.getSessionManifest(sessionId);
        assertNotNull(manifest);
        assertNotEquals("Incomplete session must NEVER be marked COMPLETE", SessionStatus.COMPLETE, manifest.getStatus());
        assertEquals(SessionStatus.INTERRUPTED, manifest.getStatus());
    }

    /**
     * Requirement 16: Privacy and sensitive data sanitization (VIN and MAC).
     */
    @Test
    public void test16_privacyRedactionInEvents() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        // Attempt to log raw MAC and VIN in errorMessage
        SessionEvent errorWithSecrets = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.CAPTURE_ERROR)
                .errorCategory("AUTH_ERROR")
                .errorMessage("Adapter AA:BB:CC:DD:EE:FF failed on VIN 1HGCR2F84HA123456")
                .addDetail("device_address", "11:22:33:44:55:66")
                .build();

        writer.writeEvent(errorWithSecrets);
        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);
        String errorLine = lines.get(1);

        assertFalse("Line must not contain raw MAC address", errorLine.contains("DD:EE:FF"));
        assertFalse("Line must not contain full VIN", errorLine.contains("1HGCR2F84HA123456"));
        assertTrue("MAC must be masked", errorLine.contains("AA:BB:CC:**:**:**"));
        assertTrue("VIN must be redacted", errorLine.contains("1HG**********3456"));
    }

    /**
     * Requirement 17: Unavailable readings (UNSUPPORTED, NO_DATA, ERROR) without zero-substitution.
     */
    @Test
    public void test17_unavailableObservationsExplicitStatus() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        SessionEvent noDataEvent = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("OIL_TEMP")
                .status(ObservationStatus.NO_DATA)
                .build();

        SessionEvent unsupportedEvent = SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("FUEL_PRESSURE")
                .status(ObservationStatus.UNSUPPORTED)
                .build();

        writer.writeEvent(noDataEvent);
        writer.writeEvent(unsupportedEvent);
        writer.close();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        SessionEvent parsedNoData = SessionEvent.fromJson(lines.get(1));
        assertEquals(ObservationStatus.NO_DATA, parsedNoData.getStatus());
        assertNull("Unavailable reading value must be null (never 0.0)", parsedNoData.getValue());

        SessionEvent parsedUnsupported = SessionEvent.fromJson(lines.get(2));
        assertEquals(ObservationStatus.UNSUPPORTED, parsedUnsupported.getStatus());
        assertNull("Unsupported reading value must be null (never 0.0)", parsedUnsupported.getValue());
    }

    private static List<String> readLines(File file) throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static byte[] readAllBytes(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
        }
        return bos.toByteArray();
    }
}
