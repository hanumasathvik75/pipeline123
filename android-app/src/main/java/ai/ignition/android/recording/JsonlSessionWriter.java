package ai.ignition.android.recording;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe incremental JSONL writer for session events.
 * Appends each event line-by-line in UTF-8 format ending with an explicit newline.
 * Serializes writes through synchronization to prevent corruption from concurrent callbacks.
 */
public class JsonlSessionWriter implements AutoCloseable {

    private final File sessionFile;
    private final String sessionId;
    private final BufferedWriter writer;
    private final AtomicLong sequenceTracker = new AtomicLong(0);
    private volatile boolean closed = false;
    private long unwrittenEventCount = 0;
    private static final int FLUSH_INTERVAL_RECORDS = 5;

    public JsonlSessionWriter(File sessionFile, String sessionId) throws IOException {
        this(sessionFile, sessionId, true);
    }

    public JsonlSessionWriter(File sessionFile, String sessionId, boolean append) throws IOException {
        if (sessionFile == null) {
            throw new IllegalArgumentException("sessionFile cannot be null");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId cannot be null or empty");
        }
        this.sessionFile = sessionFile;
        this.sessionId = sessionId;

        File parent = sessionFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
            }
        }

        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sessionFile, append), StandardCharsets.UTF_8));
    }

    public String getSessionId() { return sessionId; }
    public File getSessionFile() { return sessionFile; }
    public boolean isClosed() { return closed; }
    public long getCurrentSequence() { return sequenceTracker.get(); }

    /**
     * Writes an event to the local session file.
     * Validates the event before writing and rejects malformed events safely without crashing.
     *
     * @param event The session event to write.
     * @return true if written successfully, false if the event was rejected as malformed or mismatched.
     * @throws SessionWriteException if an unrecoverable file I/O error occurs.
     */
    public synchronized boolean writeEvent(SessionEvent event) {
        if (closed) {
            throw new IllegalStateException("Cannot write to closed JsonlSessionWriter (session=" + sessionId + ")");
        }
        if (event == null) {
            return false;
        }

        // Validate event before writing
        try {
            event.validate();
        } catch (IllegalArgumentException e) {
            // Safely reject malformed event without crashing
            return false;
        }

        // Ensure session ID matches
        if (!sessionId.equals(event.getSessionId())) {
            return false;
        }

        // Ensure sequence numbering
        SessionEvent toWrite = event;
        if (event.getSequence() <= 0) {
            long nextSeq = sequenceTracker.incrementAndGet();
            SessionEvent.Builder b = SessionEvent.builder()
                    .schemaVersion(event.getSchemaVersion())
                    .sessionId(event.getSessionId())
                    .sequence(nextSeq)
                    .timestamp(event.getTimestamp())
                    .eventType(event.getEventType())
                    .signalName(event.getSignalName())
                    .value(event.getValue())
                    .unit(event.getUnit())
                    .status(event.getStatus())
                    .source(event.getSource())
                    .phaseId(event.getPhaseId())
                    .errorCategory(event.getErrorCategory())
                    .errorMessage(event.getErrorMessage());
            if (event.getDetails() != null) {
                for (java.util.Map.Entry<String, String> entry : event.getDetails().entrySet()) {
                    b.addDetail(entry.getKey(), entry.getValue());
                }
            }
            toWrite = b.build();
        } else {
            sequenceTracker.updateAndGet(current -> Math.max(current, event.getSequence()));
        }

        try {
            String jsonLine = toWrite.toJson();
            writer.write(jsonLine);
            writer.write("\n");
            unwrittenEventCount++;

            // Flush immediately on lifecycle/phase events or when batch threshold is met
            if (isCriticalEvent(toWrite.getEventType()) || unwrittenEventCount >= FLUSH_INTERVAL_RECORDS) {
                flush();
            }
            return true;
        } catch (IOException e) {
            throw new SessionWriteException("Failed to write event to " + sessionFile.getAbsolutePath(), e);
        }
    }

    private boolean isCriticalEvent(EventType type) {
        return type == EventType.SESSION_STARTED
                || type == EventType.SESSION_STOPPED
                || type == EventType.PHASE_CHANGED
                || type == EventType.CONNECTION_LOST
                || type == EventType.CONNECTION_RESTORED
                || type == EventType.CAPTURE_ERROR
                || type == EventType.SESSION_FINALIZED;
    }

    /**
     * Flushes buffered bytes to persistent storage.
     */
    public synchronized void flush() {
        if (closed) return;
        try {
            writer.flush();
            unwrittenEventCount = 0;
        } catch (IOException e) {
            throw new SessionWriteException("Failed to flush session file " + sessionFile.getAbsolutePath(), e);
        }
    }

    /**
     * Flushes and closes the underlying file stream.
     * Safe against multiple calls.
     */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new SessionWriteException("Error closing session file " + sessionFile.getAbsolutePath(), e);
        }
    }
}
