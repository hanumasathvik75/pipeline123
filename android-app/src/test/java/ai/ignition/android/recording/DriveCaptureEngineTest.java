package ai.ignition.android.recording;

import ai.ignition.android.obd.ObdTransport;
import ai.ignition.android.obd.TransportType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Unit tests verifying real OBD observation pipeline integration, incremental writing,
 * unavailable data preservation without zero-substitution, disconnect detection,
 * bounded reconnection, and session finalization.
 */
public class DriveCaptureEngineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testBaseDir;
    private SessionRepository repository;

    @Before
    public void setUp() throws Exception {
        testBaseDir = tempFolder.newFolder("drive_capture_test");
        repository = new SessionRepository(testBaseDir);
    }

    /**
     * Test double for ObdTransport allowing controlled responses and failure simulation.
     */
    public static class FakeObdTransport implements ObdTransport {
        private boolean connected = true;
        private boolean closed = false;
        private final Map<String, String> responses = new LinkedHashMap<>();
        private volatile IOException failureToThrow = null;
        private final AtomicInteger exchangeCount = new AtomicInteger(0);

        public void setResponse(String command, String response) {
            responses.put(command, response);
        }

        public void setFailure(IOException e) {
            this.failureToThrow = e;
        }

        @Override
        public void connect(Duration timeout) throws IOException {
            if (failureToThrow != null) throw failureToThrow;
            connected = true;
            closed = false;
        }

        @Override
        public String exchange(String command, Duration timeout) throws IOException {
            exchangeCount.incrementAndGet();
            if (failureToThrow != null) {
                throw failureToThrow;
            }
            for (Map.Entry<String, String> entry : responses.entrySet()) {
                if (command.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "41 " + command + " 00";
        }

        @Override
        public boolean isConnected() {
            return connected && !closed;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.CLASSIC_SPP_CONFIRMED;
        }

        @Override
        public String getDeviceDescription() {
            return "TestAdapter [00:11:22:33:44:55]";
        }

        @Override
        public void close() {
            closed = true;
            connected = false;
        }

        public int getExchangeCount() {
            return exchangeCount.get();
        }
    }

    /**
     * Test 1: Real OBD observations are decoded and appended incrementally to session.jsonl.
     * Sequence numbers must be strictly monotonic and final manifest generated.
     */
    @Test
    public void testRealObservationRecordingIncremental() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        FakeObdTransport transport = new FakeObdTransport();
        // RPM: ((0x1A * 256) + 0xF8) / 4 = (6656 + 248) / 4 = 1726.0 rpm
        transport.setResponse("010C", "41 0C 1A F8");
        // Speed: 0x3C = 60 km/h
        transport.setResponse("010D", "41 0D 3C");
        // Coolant: 0x78 - 40 = 120 - 40 = 80 °C
        transport.setResponse("0105", "41 05 78");
        // Load: (0x64 * 100) / 255 = 39.2%
        transport.setResponse("0104", "41 04 64");

        CountDownLatch obsLatch = new CountDownLatch(4);
        CountDownLatch finalizedLatch = new CountDownLatch(1);
        AtomicReference<SessionManifest> finalizedManifest = new AtomicReference<>();

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Arrays.asList("0C", "0D", "05", "04"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) {
                        finalizedManifest.set(manifest);
                        finalizedLatch.countDown();
                    }
                },
                5, 100, 5000, 10 // Rapid 10ms polling for unit test
        );

        engine.startCapture(transport);

        // Await observations
        assertTrue("Must record observations within deadline", obsLatch.await(3, TimeUnit.SECONDS));
        assertTrue("Engine must be in RECORDING state", engine.isRunning());

        // Stop capture
        engine.stopCapture();
        assertTrue("Must finalize session within deadline", finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        assertTrue("session.jsonl must exist", jsonlFile.exists());

        List<String> lines = readLines(jsonlFile);
        assertTrue("Must contain at least 6 records (start + 4 obs + stop + finalize)", lines.size() >= 6);

        // Verify independent line parsing and sequence monotonicity
        long lastSeq = 0;
        boolean foundRpm = false;
        boolean foundSpeed = false;

        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            assertEquals("Session ID must be uniform", sessionId, event.getSessionId());
            assertTrue("Sequence must increase strictly monotonically: " + event.getSequence() + " > " + lastSeq,
                    event.getSequence() > lastSeq);
            lastSeq = event.getSequence();

            if (event.getEventType() == EventType.OBD_OBSERVATION) {
                if ("Engine RPM".equals(event.getSignalName())) {
                    foundRpm = true;
                    assertEquals(Double.valueOf(1726.0), event.getValue());
                    assertEquals(ObservationStatus.VALID, event.getStatus());
                    assertEquals("rpm", event.getUnit());
                } else if ("Vehicle Speed".equals(event.getSignalName())) {
                    foundSpeed = true;
                    assertEquals(Double.valueOf(60.0), event.getValue());
                    assertEquals(ObservationStatus.VALID, event.getStatus());
                    assertEquals("km/h", event.getUnit());
                }
            }
        }

        assertTrue("Must record valid RPM observation", foundRpm);
        assertTrue("Must record valid Speed observation", foundSpeed);

        // Verify manifest
        SessionManifest manifest = finalizedManifest.get();
        assertNotNull(manifest);
        assertEquals(SessionStatus.COMPLETE, manifest.getStatus());
        assertTrue(manifest.isValidationPassed());
        assertEquals(lines.size(), manifest.getRecordCount());
        assertEquals(0, manifest.getInvalidRecordCount());
    }

    /**
     * Test 2: Unavailable readings (NO DATA) must produce explicit status with null value, never numeric zero.
     */
    @Test
    public void testUnavailableObservationExplicitStatus() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        FakeObdTransport transport = new FakeObdTransport();
        transport.setResponse("010C", "NO DATA"); // Unavailable RPM

        CountDownLatch obsLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Collections.singletonList("0C"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 100, 5000, 10
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));
        engine.stopCapture();
        assertTrue(finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        boolean checkedNoData = false;
        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            if (event.getEventType() == EventType.OBD_OBSERVATION) {
                checkedNoData = true;
                assertEquals(ObservationStatus.NO_DATA, event.getStatus());
                assertNull("Unavailable observation must NOT be replaced with numeric zero", event.getValue());
            }
        }
        assertTrue("Must verify NO_DATA event", checkedNoData);
    }

    /**
     * Test 3: Bluetooth loss mid-recording emits CONNECTION_LOST, executes bounded reconnect,
     * and records CONNECTION_RESTORED with truthful gap duration before resuming capture.
     */
    @Test
    public void testBluetoothDisconnectAndRecovery() throws Exception {
        String sessionId = UUID.randomUUID().toString();

        FakeObdTransport initialTransport = new FakeObdTransport();
        initialTransport.setResponse("010C", "41 0C 1A F8");

        FakeObdTransport recoveredTransport = new FakeObdTransport();
        recoveredTransport.setResponse("010C", "41 0C 1E 00"); // 1920 rpm
        recoveredTransport.setResponse("0100", "41 00 BE 3E B8 11"); // Probe for verification

        CountDownLatch lostLatch = new CountDownLatch(1);
        CountDownLatch restoredLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        AtomicInteger transportCreations = new AtomicInteger(0);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository,
                () -> {
                    int count = transportCreations.incrementAndGet();
                    return recoveredTransport;
                },
                Collections.singletonList("0C"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) {}
                    @Override public void onConnectionLost(String reason) { lostLatch.countDown(); }
                    @Override public void onConnectionRestored(long gapDurationMs) { restoredLatch.countDown(); }
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 50, 5000, 10
        );

        engine.startCapture(initialTransport);
        Thread.sleep(50); // Let 1-2 observations occur

        // Inject transport failure
        initialTransport.setFailure(new IOException("Bluetooth connection dropped (RFCOMM broken pipe)"));

        // Wait for CONNECTION_LOST and CONNECTION_RESTORED
        assertTrue("Must detect and record connection loss", lostLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Must reconnect and record connection restored", restoredLatch.await(5, TimeUnit.SECONDS));

        // Let recovered capture run briefly
        Thread.sleep(50);
        engine.stopCapture();
        assertTrue(finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        boolean foundLost = false;
        boolean foundRestored = false;
        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            if (event.getEventType() == EventType.CONNECTION_LOST) {
                foundLost = true;
                assertEquals("TRANSPORT_DISCONNECT", event.getErrorCategory());
            } else if (event.getEventType() == EventType.CONNECTION_RESTORED) {
                foundRestored = true;
                assertNotNull("Restored event must record gap_ms", event.getDetails().get("gap_ms"));
            }
        }

        assertTrue("JSONL must contain CONNECTION_LOST event", foundLost);
        assertTrue("JSONL must contain CONNECTION_RESTORED event", foundRestored);
    }

    /**
     * Test 4: Bounded reconnect stops after max retry attempts are exhausted without hanging.
     */
    @Test
    public void testReconnectExhaustionStopsSafely() throws Exception {
        String sessionId = UUID.randomUUID().toString();

        FakeObdTransport initialTransport = new FakeObdTransport();
        initialTransport.setResponse("010C", "41 0C 1A F8");

        CountDownLatch lostLatch = new CountDownLatch(1);
        CountDownLatch errorLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        AtomicInteger reconnectAttempts = new AtomicInteger(0);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository,
                () -> {
                    reconnectAttempts.incrementAndGet();
                    throw new IOException("Adapter device powered off");
                },
                Collections.singletonList("0C"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) {}
                    @Override public void onConnectionLost(String reason) { lostLatch.countDown(); }
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) { errorLatch.countDown(); }
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                3, 20, 2000, 10 // max 3 attempts
        );

        engine.startCapture(initialTransport);
        Thread.sleep(30);

        // Inject failure
        initialTransport.setFailure(new IOException("Hardware disconnect"));

        assertTrue("Must detect connection loss", lostLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Must report error on reconnect exhaustion", errorLatch.await(5, TimeUnit.SECONDS));

        engine.stopCapture();
        assertTrue("Must finalize cleanly after reconnect exhaustion", finalizedLatch.await(5, TimeUnit.SECONDS));

        assertEquals("Must attempt exactly 3 retries", 3, reconnectAttempts.get());

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        boolean foundError = false;
        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            if (event.getEventType() == EventType.CAPTURE_ERROR) {
                foundError = true;
                assertEquals("RECONNECT_EXHAUSTED", event.getErrorCategory());
            }
        }
        assertTrue("JSONL must contain CAPTURE_ERROR event", foundError);
    }

    /**
     * Test 5: Safe stop during reconnect cancels backoff immediately and finalizes session cleanly.
     */
    @Test
    public void testStopRequestedDuringReconnect() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        FakeObdTransport initialTransport = new FakeObdTransport();
        initialTransport.setResponse("010C", "41 0C 1A F8");

        CountDownLatch lostLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository,
                () -> {
                    // Long delay simulating adapter lookup
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                    throw new IOException("Unavailable");
                },
                Collections.singletonList("0C"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) {}
                    @Override public void onConnectionLost(String reason) { lostLatch.countDown(); }
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 5000, 30000, 10
        );

        engine.startCapture(initialTransport);
        Thread.sleep(30);

        // Inject failure to trigger reconnect
        initialTransport.setFailure(new IOException("Transport drop"));
        assertTrue(lostLatch.await(5, TimeUnit.SECONDS));

        // Immediately request stop while reconnect is pending
        engine.stopCapture();

        assertTrue("Must finalize within 5 seconds without waiting for 5s backoff", finalizedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(CaptureState.STOPPED, engine.getState());
    }

    /**
     * Test 6: Duplicate start and stop actions are idempotent and do not corrupt files.
     */
    @Test
    public void testDuplicateStartAndStopIdempotence() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        FakeObdTransport transport = new FakeObdTransport();
        transport.setResponse("010C", "41 0C 1A F8");

        CountDownLatch obsLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Collections.singletonList("0C"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 100, 5000, 10
        );

        engine.startCapture(transport);
        // Duplicate start must be safely ignored
        engine.startCapture(transport);

        assertTrue(obsLatch.await(2, TimeUnit.SECONDS));

        // First stop
        engine.stopCapture();
        assertTrue(finalizedLatch.await(2, TimeUnit.SECONDS));

        // Duplicate stop must be safe
        engine.stopCapture();

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        int startCount = 0;
        int stopCount = 0;
        int finalizeCount = 0;

        for (String line : lines) {
            SessionEvent event = SessionEvent.fromJson(line);
            if (event.getEventType() == EventType.SESSION_STARTED) startCount++;
            if (event.getEventType() == EventType.SESSION_STOPPED) stopCount++;
            if (event.getEventType() == EventType.SESSION_FINALIZED) finalizeCount++;
        }

        assertEquals("Must contain exactly 1 SESSION_STARTED event", 1, startCount);
        assertEquals("Must contain exactly 1 SESSION_STOPPED event", 1, stopCount);
        assertEquals("Must contain exactly 1 SESSION_FINALIZED event", 1, finalizeCount);
    }

    private static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
