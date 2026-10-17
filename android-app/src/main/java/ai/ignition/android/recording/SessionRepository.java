package ai.ignition.android.recording;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the local session lifecycle, incremental writing, finalization,
 * validation, and detection/recovery of interrupted sessions.
 * Independent of Android UI and backend networks.
 */
public class SessionRepository {

    private final File baseDir;
    private final File sessionsDir;
    private final Map<String, JsonlSessionWriter> activeWriters = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    public SessionRepository(File baseDir) {
        if (baseDir == null) {
            throw new IllegalArgumentException("baseDir cannot be null");
        }
        this.baseDir = baseDir;
        this.sessionsDir = new File(baseDir, "sessions");
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
    }

    public File getSessionsDir() { return sessionsDir; }

    public File getSessionDir(String sessionId) {
        return new File(sessionsDir, sessionId);
    }

    public File getSessionJsonlFile(String sessionId) {
        return new File(getSessionDir(sessionId), "session.jsonl");
    }

    public File getManifestFile(String sessionId) {
        return new File(getSessionDir(sessionId), "manifest.json");
    }

    private Object getSessionLock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    /**
     * Creates and begins a new local session.
     * Writes an initial RECORDING manifest and a SESSION_STARTED event.
     *
     * @param sessionId Unique session ID.
     * @return Initialized JsonlSessionWriter.
     * @throws IOException If storage operations fail.
     */
    public JsonlSessionWriter startSession(String sessionId) throws IOException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId cannot be null or empty");
        }

        synchronized (getSessionLock(sessionId)) {
            File dir = getSessionDir(sessionId);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Failed to create session directory: " + dir.getAbsolutePath());
            }

            File jsonlFile = getSessionJsonlFile(sessionId);
            File manifestFile = getManifestFile(sessionId);

            String startTime = Instant.now().toString();

            // Write initial RECORDING manifest
            SessionManifest initialManifest = SessionManifest.builder()
                    .sessionId(sessionId)
                    .startTime(startTime)
                    .status(SessionStatus.RECORDING)
                    .validationSummary("Session in progress")
                    .build();
            writeManifestToFile(manifestFile, initialManifest);

            // Open writer and record initial event
            JsonlSessionWriter writer = new JsonlSessionWriter(jsonlFile, sessionId, true);
            SessionEvent startEvent = SessionEvent.builder()
                    .sessionId(sessionId)
                    .sequence(1)
                    .timestamp(startTime)
                    .eventType(EventType.SESSION_STARTED)
                    .build();
            writer.writeEvent(startEvent);
            writer.flush();

            activeWriters.put(sessionId, writer);
            return writer;
        }
    }

    public JsonlSessionWriter getActiveWriter(String sessionId) {
        return activeWriters.get(sessionId);
    }

    /**
     * Finalizes a session safely and idempotently.
     * Stops accepting new events, flushes/closes the writer, validates the generated JSONL,
     * and produces the final manifest.json.
     *
     * Calling finalize repeatedly on the same session returns the existing manifest
     * without generating duplicate terminal events or corrupting files.
     *
     * @param sessionId Session ID to finalize.
     * @return Final SessionManifest.
     * @throws IOException If storage operations fail.
     */
    public SessionManifest finalizeSession(String sessionId) throws IOException {
        synchronized (getSessionLock(sessionId)) {
            File manifestFile = getManifestFile(sessionId);
            File jsonlFile = getSessionJsonlFile(sessionId);

            // If already finalized (not in RECORDING state), return existing manifest idempotently
            if (manifestFile.exists()) {
                try {
                    SessionManifest existing = readManifestFromFile(manifestFile);
                    if (existing != null && existing.getStatus() != SessionStatus.RECORDING) {
                        return existing;
                    }
                } catch (Exception ignored) {}
            }

            JsonlSessionWriter writer = activeWriters.remove(sessionId);
            String endTime = Instant.now().toString();

            if (writer != null && !writer.isClosed()) {
                // Record terminal lifecycle events
                SessionEvent stopEvent = SessionEvent.builder()
                        .sessionId(sessionId)
                        .sequence(writer.getCurrentSequence() + 1)
                        .timestamp(endTime)
                        .eventType(EventType.SESSION_STOPPED)
                        .build();
                writer.writeEvent(stopEvent);

                SessionEvent finalizeEvent = SessionEvent.builder()
                        .sessionId(sessionId)
                        .sequence(writer.getCurrentSequence() + 1)
                        .timestamp(endTime)
                        .eventType(EventType.SESSION_FINALIZED)
                        .build();
                writer.writeEvent(finalizeEvent);

                writer.close();
            }

            // Run line-by-line validation
            ValidationResult validation = SessionValidator.validateFile(jsonlFile, sessionId);

            SessionStatus finalStatus;
            if (validation.isValid()) {
                finalStatus = SessionStatus.COMPLETE;
            } else {
                finalStatus = SessionStatus.INVALID;
            }

            String startTime = validation.getFirstTimestamp() != null ? validation.getFirstTimestamp() : endTime;
            String resolvedEndTime = validation.getLastTimestamp() != null ? validation.getLastTimestamp() : endTime;

            SessionManifest manifest = SessionManifest.builder()
                    .sessionId(sessionId)
                    .startTime(startTime)
                    .endTime(resolvedEndTime)
                    .durationMs(validation.getDurationMs())
                    .status(finalStatus)
                    .recordCount(validation.getTotalLineCount())
                    .validRecordCount(validation.getValidRecordCount())
                    .invalidRecordCount(validation.getInvalidRecordCount())
                    .fileSizeBytes(validation.getFileSizeBytes())
                    .validationPassed(validation.isValid())
                    .validationSummary(validation.getSummary())
                    .finalizedAt(Instant.now().toString())
                    .build();

            writeManifestToFile(manifestFile, manifest);
            return manifest;
        }
    }

    /**
     * Scans the sessions directory for sessions left incomplete due to an unexpected
     * application kill or process termination.
     * Preserves all partial JSONL evidence and marks the session INTERRUPTED.
     *
     * @return List of recovered session manifests.
     */
    public List<SessionManifest> detectAndRecoverInterruptedSessions() {
        List<SessionManifest> recovered = new ArrayList<>();
        File[] sessionDirs = sessionsDir.listFiles(File::isDirectory);
        if (sessionDirs == null) return recovered;

        for (File dir : sessionDirs) {
            String sessionId = dir.getName();
            synchronized (getSessionLock(sessionId)) {
                // Skip currently active in-memory writers
                if (activeWriters.containsKey(sessionId)) {
                    continue;
                }

                File manifestFile = new File(dir, "manifest.json");
                File jsonlFile = new File(dir, "session.jsonl");

                boolean needsRecovery = false;
                if (!manifestFile.exists()) {
                    needsRecovery = true;
                } else {
                    try {
                        SessionManifest manifest = readManifestFromFile(manifestFile);
                        if (manifest != null && manifest.getStatus() == SessionStatus.RECORDING) {
                            needsRecovery = true;
                        }
                    } catch (Exception e) {
                        needsRecovery = true;
                    }
                }

                if (needsRecovery) {
                    ValidationResult validation = SessionValidator.validateFile(jsonlFile, sessionId);
                    String now = Instant.now().toString();
                    String startTime = validation.getFirstTimestamp() != null ? validation.getFirstTimestamp() : now;
                    String endTime = validation.getLastTimestamp() != null ? validation.getLastTimestamp() : now;

                    SessionManifest recoveryManifest = SessionManifest.builder()
                            .sessionId(sessionId)
                            .startTime(startTime)
                            .endTime(endTime)
                            .durationMs(validation.getDurationMs())
                            .status(SessionStatus.INTERRUPTED)
                            .recordCount(validation.getTotalLineCount())
                            .validRecordCount(validation.getValidRecordCount())
                            .invalidRecordCount(validation.getInvalidRecordCount())
                            .fileSizeBytes(validation.getFileSizeBytes())
                            .validationPassed(validation.isValid())
                            .validationSummary("Session interrupted by unexpected termination; data preserved (" + validation.getSummary() + ")")
                            .finalizedAt(now)
                            .build();

                    try {
                        writeManifestToFile(manifestFile, recoveryManifest);
                        recovered.add(recoveryManifest);
                    } catch (IOException ignored) {}
                }
            }
        }
        return recovered;
    }

    /**
     * Reads a session manifest from storage.
     */
    public SessionManifest getSessionManifest(String sessionId) {
        File manifestFile = getManifestFile(sessionId);
        if (!manifestFile.exists()) return null;
        try {
            return readManifestFromFile(manifestFile);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Lists all session manifests found in local storage.
     */
    public List<SessionManifest> listSessions() {
        List<SessionManifest> list = new ArrayList<>();
        File[] dirs = sessionsDir.listFiles(File::isDirectory);
        if (dirs == null) return list;

        for (File dir : dirs) {
            File manifestFile = new File(dir, "manifest.json");
            if (manifestFile.exists()) {
                try {
                    SessionManifest m = readManifestFromFile(manifestFile);
                    if (m != null) list.add(m);
                } catch (Exception ignored) {}
            }
        }
        // Sort descending by start time
        list.sort((a, b) -> {
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return b.getStartTime().compareTo(a.getStartTime());
        });
        return list;
    }

    private void writeManifestToFile(File manifestFile, SessionManifest manifest) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifestFile), StandardCharsets.UTF_8))) {
            writer.write(manifest.toJson());
            writer.write("\n");
            writer.flush();
        }
    }

    private SessionManifest readManifestFromFile(File manifestFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifestFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return SessionManifest.fromJson(sb.toString());
    }
}
