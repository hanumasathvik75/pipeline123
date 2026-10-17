package ai.ignition.android.recording;

import ai.ignition.android.obd.Elm327Initializer;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.obd.ObdTransport;
import ai.ignition.android.util.RedactionUtil;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background capture engine that polls real OBD live diagnostic signals,
 * formats them into SessionEvents without fabricating data, appends them incrementally
 * to the session JSONL file, detects communication loss, and manages bounded reconnection.
 */
public class DriveCaptureEngine {

    public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 5;
    public static final long DEFAULT_INITIAL_BACKOFF_MS = 1000L;
    public static final long DEFAULT_MAX_RECONNECT_BUDGET_MS = 60000L;
    public static final long DEFAULT_TARGET_CYCLE_MS = 1000L; // 1.0 second per 4-PID cycle (1 Hz)
    public static final long DEFAULT_INTER_PID_DELAY_MS = 25L; // 25ms pacing gap between PID commands
    public static final long MIN_CYCLE_GAP_MS = 50L; // Minimum gap after cycle completes

    public interface CaptureListener {
        void onStateChanged(CaptureState state);
        void onObservation(SessionEvent event);
        void onConnectionLost(String reason);
        void onConnectionRestored(long gapDurationMs);
        void onError(String error);
        void onFinalized(SessionManifest manifest);
    }

    private final String sessionId;
    private final SessionRepository repository;
    private final TransportFactory transportFactory;
    private final List<String> supportedPids;
    private final CaptureListener listener;
    private final int maxReconnectAttempts;
    private final long initialBackoffMs;
    private final long maxReconnectBudgetMs;
    private final long targetCycleMs;
    private final long interPidDelayMs;

    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    // Diagnostic and provenance counters
    private final AtomicLong recordCount = new AtomicLong(0);
    private final AtomicLong cycleCount = new AtomicLong(0);
    private final AtomicLong validObservationCount = new AtomicLong(0);
    private final AtomicLong unavailableObservationCount = new AtomicLong(0);
    private final AtomicLong errorObservationCount = new AtomicLong(0);

    private volatile long lastCycleDurationMs = 0;
    private volatile long lastRequestDurationMs = 0;
    private volatile Instant lastSuccessfulObservationTime = null;

    private volatile CaptureState currentState = CaptureState.IDLE;
    private volatile ObdTransport currentTransport;
    private volatile JsonlSessionWriter writer;
    private Thread workerThread;

    public DriveCaptureEngine(String sessionId,
                              SessionRepository repository,
                              TransportFactory transportFactory,
                              List<String> supportedPids,
                              CaptureListener listener) {
        this(sessionId, repository, transportFactory, supportedPids, listener,
                DEFAULT_MAX_RECONNECT_ATTEMPTS, DEFAULT_INITIAL_BACKOFF_MS,
                DEFAULT_MAX_RECONNECT_BUDGET_MS, DEFAULT_TARGET_CYCLE_MS, DEFAULT_INTER_PID_DELAY_MS);
    }

    /**
     * Backward-compatible constructor for legacy tests that specify a single delay value.
     */
    public DriveCaptureEngine(String sessionId,
                              SessionRepository repository,
                              TransportFactory transportFactory,
                              List<String> supportedPids,
                              CaptureListener listener,
                              int maxReconnectAttempts,
                              long initialBackoffMs,
                              long maxReconnectBudgetMs,
                              long pollDelayMs) {
        this(sessionId, repository, transportFactory, supportedPids, listener,
                maxReconnectAttempts, initialBackoffMs, maxReconnectBudgetMs,
                pollDelayMs, Math.min(25L, Math.max(0L, pollDelayMs / 4)));
    }

    public DriveCaptureEngine(String sessionId,
                              SessionRepository repository,
                              TransportFactory transportFactory,
                              List<String> supportedPids,
                              CaptureListener listener,
                              int maxReconnectAttempts,
                              long initialBackoffMs,
                              long maxReconnectBudgetMs,
                              long targetCycleMs,
                              long interPidDelayMs) {
        this.sessionId = sessionId;
        this.repository = repository;
        this.transportFactory = transportFactory;
        this.supportedPids = (supportedPids != null && !supportedPids.isEmpty())
                ? new ArrayList<>(supportedPids)
                : Arrays.asList("0C", "0D", "05", "04"); // Default RPM, Speed, Coolant, Load
        this.listener = listener;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.maxReconnectBudgetMs = maxReconnectBudgetMs;
        this.targetCycleMs = Math.max(10L, targetCycleMs);
        this.interPidDelayMs = Math.max(0L, interPidDelayMs);
    }

    public String getSessionId() { return sessionId; }
    public CaptureState getState() { return currentState; }
    public boolean isRunning() { return running.get(); }
    public long getRecordCount() { return recordCount.get(); }
    public long getCycleCount() { return cycleCount.get(); }
    public long getValidObservationCount() { return validObservationCount.get(); }
    public long getUnavailableObservationCount() { return unavailableObservationCount.get(); }
    public long getErrorObservationCount() { return errorObservationCount.get(); }
    public long getLastCycleDurationMs() { return lastCycleDurationMs; }
    public long getLastRequestDurationMs() { return lastRequestDurationMs; }
    public Instant getLastSuccessfulObservationTime() { return lastSuccessfulObservationTime; }
    public long getTargetCycleMs() { return targetCycleMs; }

    private void updateState(CaptureState newState) {
        this.currentState = newState;
        if (listener != null) {
            try {
                listener.onStateChanged(newState);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Starts the capture engine with an already connected transport or connects a fresh one.
     *
     * @param initialTransport Connected ObdTransport, or null to connect via TransportFactory.
     */
    public synchronized void startCapture(ObdTransport initialTransport) {
        if (running.get()) {
            return;
        }
        running.set(true);
        stopRequested.set(false);

        captureExecutor.execute(() -> {
            workerThread = Thread.currentThread();
            try {
                updateState(CaptureState.INITIALIZING);

                // Start session in repository
                writer = repository.getActiveWriter(sessionId);
                if (writer == null) {
                    writer = repository.startSession(sessionId);
                }
                recordCount.set(writer.getCurrentSequence());

                // Prepare transport
                if (initialTransport != null) {
                    if (!initialTransport.isConnected()) {
                        initialTransport.connect(Duration.ofSeconds(6));
                    }
                    currentTransport = initialTransport;
                } else {
                    currentTransport = transportFactory.createTransport();
                    currentTransport.connect(Duration.ofSeconds(6));
                    Elm327Initializer.initialize(currentTransport);
                }

                updateState(CaptureState.RECORDING);

                // Main capture loop with deliberate cycle cadence and inter-PID pacing
                while (running.get() && !stopRequested.get()) {
                    long cycleStartNs = System.nanoTime();
                    long currentCycle = cycleCount.incrementAndGet();

                    boolean success = pollSupportedPids(currentCycle);
                    if (!success) {
                        // Connection lost during polling
                        if (stopRequested.get() || !running.get()) {
                            break;
                        }
                        boolean recovered = handleConnectionLossAndReconnect();
                        if (!recovered) {
                            // Reconnect budget exhausted or stop requested
                            break;
                        }
                        continue;
                    }

                    long cycleDurationMs = (System.nanoTime() - cycleStartNs) / 1_000_000L;
                    lastCycleDurationMs = cycleDurationMs;

                    long sleepMs = Math.max(MIN_CYCLE_GAP_MS, targetCycleMs - cycleDurationMs);
                    if (sleepMs > 0 && running.get() && !stopRequested.get()) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException ie) {
                            if (stopRequested.get() || !running.get()) break;
                        }
                    }
                }
            } catch (Exception e) {
                if (!stopRequested.get()) {
                    updateState(CaptureState.ERROR);
                    if (listener != null) listener.onError("Fatal capture error: " + e.getMessage());
                }
            } finally {
                cleanupAndFinalize();
            }
        });
    }

    /**
     * Queries each supported PID in order with inter-PID pacing, converts the reading
     * to a SessionEvent with data provenance, and appends it incrementally to session.jsonl.
     *
     * @param cycleIndex Monotonic cycle sequence number.
     * @return true if all queries completed (even if returning NO_DATA); false if a communication/IO failure occurred.
     */
    private boolean pollSupportedPids(long cycleIndex) {
        if (currentTransport == null || !currentTransport.isConnected()) {
            return false;
        }

        String source = currentTransport.getTransportType() != null
                ? currentTransport.getTransportType().name()
                : "OBD_TRANSPORT";

        for (int i = 0; i < supportedPids.size(); i++) {
            if (!running.get() || stopRequested.get()) return true;

            String pidHex = supportedPids.get(i);
            LivePidDecoder.PidReading reading;
            switch (pidHex.toUpperCase(Locale.ROOT)) {
                case "0C":
                case "010C":
                    reading = LivePidDecoder.readRpm(currentTransport);
                    break;
                case "0D":
                case "010D":
                    reading = LivePidDecoder.readSpeed(currentTransport);
                    break;
                case "05":
                case "0105":
                    reading = LivePidDecoder.readCoolant(currentTransport);
                    break;
                case "04":
                case "0104":
                    reading = LivePidDecoder.readEngineLoad(currentTransport);
                    break;
                default:
                    continue;
            }

            // Check if IO error occurred indicating communication loss
            if (reading.status != null && reading.status.startsWith("IO_ERROR")) {
                return false;
            }

            SessionEvent event = createObservationEvent(reading, source, cycleIndex);
            boolean written = writer.writeEvent(event);
            if (written) {
                recordCount.incrementAndGet();
                if (listener != null) {
                    try {
                        listener.onObservation(event);
                    } catch (Exception ignored) {}
                }
            }

            // Pacing delay between sequential PID requests to protect ELM327 UART buffer
            if (interPidDelayMs > 0 && i < supportedPids.size() - 1 && running.get() && !stopRequested.get()) {
                try {
                    Thread.sleep(interPidDelayMs);
                } catch (InterruptedException ie) {
                    if (stopRequested.get() || !running.get()) return true;
                }
            }
        }
        return true;
    }

    private SessionEvent createObservationEvent(LivePidDecoder.PidReading reading, String source, long cycleIndex) {
        ObservationStatus status;
        Double value = null;

        if (reading.valid && reading.value != null) {
            status = ObservationStatus.VALID;
            value = reading.value;
            validObservationCount.incrementAndGet();
            lastSuccessfulObservationTime = reading.timestamp;
        } else if (reading.status != null && (reading.status.contains("NO_DATA") || reading.status.contains("EMPTY"))) {
            status = ObservationStatus.NO_DATA;
            unavailableObservationCount.incrementAndGet();
        } else if (reading.status != null && reading.status.contains("UNSUPPORTED")) {
            status = ObservationStatus.UNSUPPORTED;
            unavailableObservationCount.incrementAndGet();
        } else {
            status = ObservationStatus.ERROR;
            errorObservationCount.incrementAndGet();
        }

        lastRequestDurationMs = reading.durationMs;

        SessionEvent.Builder b = SessionEvent.builder()
                .sessionId(sessionId)
                .timestamp(reading.timestamp != null ? reading.timestamp.toString() : Instant.now().toString())
                .eventType(EventType.OBD_OBSERVATION)
                .signalName(reading.signalName != null ? reading.signalName : reading.pid)
                .value(value) // Explicitly null for unavailable data; never substitute 0.0
                .unit(reading.unit)
                .status(status)
                .source(source)
                .addDetail("pid", reading.pid)
                .addDetail("cycle_index", String.valueOf(cycleIndex))
                .addDetail("request_duration_ms", String.valueOf(reading.durationMs));

        if (reading.rawResponse != null && !reading.rawResponse.isBlank()) {
            b.addDetail("raw_response", reading.rawResponse.trim());
        }

        return b.build();
    }

    /**
     * Handles connection drop by recording a CONNECTION_LOST event, pausing telemetry,
     * executing bounded reconnect attempts with exponential backoff, and recording
     * CONNECTION_RESTORED with honest gap duration upon verified recovery.
     *
     * @return true if communication successfully reestablished; false if budget exhausted or stopped.
     */
    private boolean handleConnectionLossAndReconnect() {
        updateState(CaptureState.RECONNECTING);
        Instant lostTime = Instant.now();
        String lossReason = "Bluetooth/OBD transport communication dropped";

        if (writer != null && !writer.isClosed()) {
            SessionEvent lossEvent = SessionEvent.builder()
                    .sessionId(sessionId)
                    .eventType(EventType.CONNECTION_LOST)
                    .errorCategory("TRANSPORT_DISCONNECT")
                    .errorMessage(RedactionUtil.redactMac(lossReason))
                    .build();
            writer.writeEvent(lossEvent);
            writer.flush();
        }

        if (listener != null) {
            try {
                listener.onConnectionLost(lossReason);
            } catch (Exception ignored) {}
        }

        // Close failed transport safely
        if (currentTransport != null) {
            try {
                currentTransport.close();
            } catch (Exception ignored) {}
            currentTransport = null;
        }

        long startTime = System.currentTimeMillis();
        long currentBackoff = initialBackoffMs;

        for (int attempt = 1; attempt <= maxReconnectAttempts; attempt++) {
            if (stopRequested.get() || !running.get()) {
                return false;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= maxReconnectBudgetMs) {
                break;
            }

            // Wait backoff delay
            try {
                Thread.sleep(currentBackoff);
            } catch (InterruptedException ie) {
                if (stopRequested.get() || !running.get()) return false;
            }

            if (stopRequested.get() || !running.get()) return false;

            try {
                ObdTransport newTransport = transportFactory.createTransport();
                newTransport.connect(Duration.ofSeconds(5));

                // Re-initialize adapter
                Elm327Initializer.initialize(newTransport);

                // Verify communication before resuming capture
                String probe = newTransport.exchange("0100", Duration.ofSeconds(2));
                if (probe != null && !probe.isBlank()) {
                    currentTransport = newTransport;
                    long gapMs = Math.max(0, Duration.between(lostTime, Instant.now()).toMillis());

                    // Record truthful CONNECTION_RESTORED event with gap duration
                    SessionEvent restoredEvent = SessionEvent.builder()
                            .sessionId(sessionId)
                            .eventType(EventType.CONNECTION_RESTORED)
                            .addDetail("gap_ms", String.valueOf(gapMs))
                            .addDetail("reconnect_attempt", String.valueOf(attempt))
                            .build();
                    if (writer != null && !writer.isClosed()) {
                        writer.writeEvent(restoredEvent);
                        writer.flush();
                    }

                    updateState(CaptureState.RECORDING);
                    if (listener != null) {
                        try {
                            listener.onConnectionRestored(gapMs);
                        } catch (Exception ignored) {}
                    }
                    return true;
                } else {
                    newTransport.close();
                }
            } catch (Exception ignored) {
                // Next retry attempt
            }

            // Exponential backoff
            currentBackoff = Math.min(currentBackoff * 2, 30000L);
        }

        // Exhausted reconnect budget
        if (writer != null && !writer.isClosed()) {
            SessionEvent errorEvent = SessionEvent.builder()
                    .sessionId(sessionId)
                    .eventType(EventType.CAPTURE_ERROR)
                    .errorCategory("RECONNECT_EXHAUSTED")
                    .errorMessage("Reconnection failed after " + maxReconnectAttempts + " attempts")
                    .build();
            writer.writeEvent(errorEvent);
            writer.flush();
        }

        updateState(CaptureState.ERROR);
        if (listener != null) {
            listener.onError("Reconnect budget exhausted (" + maxReconnectAttempts + " attempts)");
        }
        return false;
    }

    /**
     * Safely stops the capture engine and finalizes the recording session.
     * Safe against multiple calls (idempotent).
     */
    public synchronized void stopCapture() {
        if (!running.get() && currentState == CaptureState.STOPPED) {
            return;
        }
        stopRequested.set(true);
        running.set(false);

        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void cleanupAndFinalize() {
        updateState(CaptureState.STOPPING);

        // Close transport
        if (currentTransport != null) {
            try {
                currentTransport.close();
            } catch (Exception ignored) {}
            currentTransport = null;
        }

        // Finalize session in repository
        SessionManifest manifest = null;
        try {
            manifest = repository.finalizeSession(sessionId);
        } catch (IOException e) {
            if (listener != null) listener.onError("Failed to finalize session: " + e.getMessage());
        }

        updateState(CaptureState.STOPPED);
        if (listener != null && manifest != null) {
            try {
                listener.onFinalized(manifest);
            } catch (Exception ignored) {}
        }
    }

    public void shutdown() {
        stopCapture();
        captureExecutor.shutdown();
    }
}
