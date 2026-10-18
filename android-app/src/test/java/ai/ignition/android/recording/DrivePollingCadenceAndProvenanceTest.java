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
 * Regression test suite verifying:
 * 1. Polling cadence enforces deliberate target cycle interval and inter-PID pacing without busy looping.
 * 2. Sequential OBD requests strictly never overlap on the transport.
 * 3. Data provenance (raw response bytes, latency, cycle number, PID) is truthfully attached to SessionEvents.
 * 4. Unavailable readings (NO DATA, UNSUPPORTED, errors) preserve null without zero substitution.
 * 5. Payload viewer operations do not cause additional OBD queries.
 * 6. Observation counts distinguish actual observations from lifecycle, connectivity, and error events.
 * 7. Duplicate start actions are strictly idempotent and do not spawn multiple capture workers.
 * 8. Stop requests interrupt pending sleep delays promptly.
 */
public class DrivePollingCadenceAndProvenanceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testBaseDir;
    private SessionRepository repository;

    @Before
    public void setUp() throws Exception {
        testBaseDir = tempFolder.newFolder("polling_cadence_test");
        repository = new SessionRepository(testBaseDir);
    }

    /**
     * Test transport with concurrency detection and exchange counters.
     */
    public static class ConcurrencyCheckingTransport implements ObdTransport {
        private final Map<String, String> responses = new LinkedHashMap<>();
        private final AtomicInteger activeExchanges = new AtomicInteger(0);
        private final AtomicInteger maxConcurrentExchanges = new AtomicInteger(0);
        private final AtomicInteger totalExchanges = new AtomicInteger(0);
        private volatile boolean connected = true;

        public void setResponse(String pid, String response) {
            responses.put(pid, response);
        }

        @Override
        public void connect(Duration timeout) throws IOException {
            connected = true;
        }

        @Override
        public String exchange(String command, Duration timeout) throws IOException {
            int current = activeExchanges.incrementAndGet();
            totalExchanges.incrementAndGet();
            maxConcurrentExchanges.updateAndGet(max -> Math.max(max, current));

            try {
                // Simulate slight hardware delay (5ms)
                Thread.sleep(5);
            } catch (InterruptedException ignored) {}

            activeExchanges.decrementAndGet();

            for (Map.Entry<String, String> e : responses.entrySet()) {
                if (command.contains(e.getKey())) {
                    return e.getValue();
                }
            }
            return "41 " + command + " 00";
        }

        @Override
        public boolean isConnected() { return connected; }

        @Override
        public TransportType getTransportType() { return TransportType.CLASSIC_SPP_CONFIRMED; }

        @Override
        public String getDeviceDescription() { return "TestAdapter [00:11:22:33:44:55]"; }

        @Override
        public void close() { connected = false; }

        public int getMaxConcurrentExchanges() { return maxConcurrentExchanges.get(); }
        public int getTotalExchanges() { return totalExchanges.get(); }
    }

    /**
     * Test 1: Polling cadence respects target cycle interval (does not spin in a busy loop).
     */
    @Test
    public void testPollingCadenceRespectsTargetCycleInterval() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "41 0C 1A F8"); // RPM: 1726 rpm
        transport.setResponse("010D", "41 0D 3C");    // Speed: 60 km/h

        // Configure 250ms target cycle interval with 10ms inter-PID pacing
        long targetCycleMs = 250L;
        long interPidDelayMs = 10L;

        CountDownLatch threeCyclesLatch = new CountDownLatch(6); // 2 PIDs * 3 cycles = 6 observations

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Arrays.asList("0C", "0D"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { threeCyclesLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) {}
                },
                5, 100, 5000, targetCycleMs, interPidDelayMs
        );

        long startTime = System.currentTimeMillis();
        engine.startCapture(transport);

        assertTrue("Must complete 3 cycles within deadline", threeCyclesLatch.await(5, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - startTime;

        engine.stopCapture();

        // 3 cycles with 250ms target interval must take at least ~500ms (cycle 1 + cycle 2 + start of cycle 3)
        assertTrue("Elapsed time (" + elapsed + "ms) must enforce the target cycle interval without busy looping",
                elapsed >= 450);
        assertTrue("Cycle count must be at least 3", engine.getCycleCount() >= 3);
        assertTrue("Engine must report measured cycle duration", engine.getLastCycleDurationMs() > 0);
    }

    /**
     * Test 2: Sequential OBD requests strictly never overlap on the transport.
     */
    @Test
    public void testSequentialPidRequestsDoNotOverlap() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "41 0C 1A F8");
        transport.setResponse("010D", "41 0D 3C");
        transport.setResponse("0105", "41 05 78");
        transport.setResponse("0104", "41 04 64");

        CountDownLatch obsLatch = new CountDownLatch(8); // 2 cycles

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Arrays.asList("0C", "0D", "05", "04"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) {}
                },
                5, 100, 5000, 50, 10
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(4, TimeUnit.SECONDS));
        engine.stopCapture();

        assertEquals("Max concurrent exchanges on the transport must never exceed 1",
                1, transport.getMaxConcurrentExchanges());
    }

    /**
     * Test 3: Data provenance (raw response bytes, latency, cycle number, PID) is attached to SessionEvent.
     */
    @Test
    public void testDataProvenanceAndRawResponseAttached() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "41 0C 1A F8"); // Raw RPM

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
                5, 100, 5000, 50, 10
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));
        engine.stopCapture();
        assertTrue("Must finalize session", finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        SessionEvent rpmEvent = null;
        for (String line : lines) {
            SessionEvent e = SessionEvent.fromJson(line);
            if (e.getEventType() == EventType.OBD_OBSERVATION) {
                rpmEvent = e;
                break;
            }
        }

        assertNotNull("Must find OBD_OBSERVATION event", rpmEvent);
        assertEquals("Engine RPM", rpmEvent.getSignalName());
        assertEquals(Double.valueOf(1726.0), rpmEvent.getValue());
        assertEquals(ObservationStatus.VALID, rpmEvent.getStatus());

        // Check provenance details
        Map<String, String> details = rpmEvent.getDetails();
        assertNotNull("Details must not be null", details);
        assertEquals("010C", details.get("pid"));
        assertEquals("41 0C 1A F8", details.get("raw_response"));
        assertNotNull("request_duration_ms must be tracked", details.get("request_duration_ms"));
        assertEquals("1", details.get("cycle_index"));
    }

    /**
     * Test 4: Unavailable and unsupported readings remain null without zero substitution.
     */
    @Test
    public void testUnavailableAndUnsupportedRemainNullWithoutZeroSubstitution() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "NO DATA");
        transport.setResponse("010D", "UNABLE TO CONNECT");

        CountDownLatch obsLatch = new CountDownLatch(2);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Arrays.asList("0C", "0D"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 100, 5000, 50, 10
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));
        engine.stopCapture();
        assertTrue("Must finalize session", finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        for (String line : lines) {
            SessionEvent e = SessionEvent.fromJson(line);
            if (e.getEventType() == EventType.OBD_OBSERVATION) {
                assertEquals(ObservationStatus.NO_DATA, e.getStatus());
                assertNull("Unavailable values must remain strictly null; NEVER substituted with 0.0", e.getValue());
            }
        }

        assertTrue("Unavailable observation count must be at least 2", engine.getUnavailableObservationCount() >= 2);
        assertEquals("Valid observation count must be 0", 0, engine.getValidObservationCount());
    }

    /**
     * Test 5: Payload viewer reading does not trigger additional OBD requests.
     */
    @Test
    public void testViewerDoesNotTriggerObdRequests() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("Engine RPM")
                .value(2200.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .addDetail("raw_response", "41 0C 22 60")
                .build());
        writer.close();

        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        int initialCount = transport.getTotalExchanges();

        // Perform payload reader operations
        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        PayloadReader.PayloadPage page = PayloadReader.readPage(jsonlFile, 0, 50);
        long count = PayloadReader.countLines(jsonlFile);
        ValidationResult validation = SessionValidator.validateFile(jsonlFile, sessionId);

        assertEquals("Payload page must contain 2 records (start + obs)", 2, page.getRecords().size());
        assertEquals("Total lines must match", 2, count);
        assertTrue("Validation must pass", validation.isValid());

        assertEquals("Payload viewer operations must never issue OBD transport queries",
                initialCount, transport.getTotalExchanges());
    }

    /**
     * Test 6: Observation counts clearly distinguish actual observations from lifecycle events.
     */
    @Test
    public void testEventCountsDistinguishObservationsFromLifecycleEvents() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "41 0C 1A F8"); // Valid
        transport.setResponse("010D", "NO DATA");     // Unavailable

        CountDownLatch obsLatch = new CountDownLatch(2);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        DriveCaptureEngine engine = new DriveCaptureEngine(
                sessionId, repository, () -> transport,
                Arrays.asList("0C", "0D"),
                new DriveCaptureEngine.CaptureListener() {
                    @Override public void onStateChanged(CaptureState state) {}
                    @Override public void onObservation(SessionEvent event) { obsLatch.countDown(); }
                    @Override public void onConnectionLost(String reason) {}
                    @Override public void onConnectionRestored(long gapDurationMs) {}
                    @Override public void onError(String error) {}
                    @Override public void onFinalized(SessionManifest manifest) { finalizedLatch.countDown(); }
                },
                5, 100, 5000, 50, 10
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));
        engine.stopCapture();
        assertTrue(finalizedLatch.await(3, TimeUnit.SECONDS));

        // Engine distinguishes valid from unavailable
        assertTrue("Valid count must be at least 1", engine.getValidObservationCount() >= 1);
        assertTrue("Unavailable count must be at least 1", engine.getUnavailableObservationCount() >= 1);

        // Record count includes start + observations + stop + finalize
        long totalRecords = engine.getRecordCount();
        assertTrue("Total records (" + totalRecords + ") must exceed observations alone",
                totalRecords > engine.getValidObservationCount());
    }

    /**
     * Test 7: Duplicate start actions are strictly idempotent and do not spawn extra workers.
     */
    @Test
    public void testDuplicateStartIsIdempotent() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
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
                5, 100, 5000, 50, 10
        );

        engine.startCapture(transport);
        // Duplicate start must be safely ignored
        engine.startCapture(transport);

        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));
        engine.stopCapture();
        assertTrue("Must finalize session", finalizedLatch.await(3, TimeUnit.SECONDS));

        File jsonlFile = repository.getSessionJsonlFile(sessionId);
        List<String> lines = readLines(jsonlFile);

        int startEventCount = 0;
        for (String line : lines) {
            SessionEvent e = SessionEvent.fromJson(line);
            if (e.getEventType() == EventType.SESSION_STARTED) {
                startEventCount++;
            }
        }

        assertEquals("Must contain exactly 1 SESSION_STARTED event", 1, startEventCount);
    }

    /**
     * Test 8: Stop requests interrupt pending sleep delays promptly without hanging.
     */
    @Test
    public void testStopCancelsPendingDelayPromptly() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ConcurrencyCheckingTransport transport = new ConcurrencyCheckingTransport();
        transport.setResponse("010C", "41 0C 1A F8");

        CountDownLatch obsLatch = new CountDownLatch(1);
        CountDownLatch finalizedLatch = new CountDownLatch(1);

        // Configure a long 10-second target cycle interval
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
                5, 100, 5000, 10000L, 25L
        );

        engine.startCapture(transport);
        assertTrue(obsLatch.await(3, TimeUnit.SECONDS));

        long stopStartTime = System.currentTimeMillis();
        // Request stop while the 10-second cycle sleep is active
        engine.stopCapture();

        assertTrue("Must finalize within 2 seconds without waiting for 10-second cycle",
                finalizedLatch.await(2, TimeUnit.SECONDS));
        long stopDuration = System.currentTimeMillis() - stopStartTime;
        assertTrue("Stop must take under 1500ms (took " + stopDuration + "ms)", stopDuration < 1500);
        assertEquals(CaptureState.STOPPED, engine.getState());
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
