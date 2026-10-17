package ai.ignition.android.recording;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaming line-by-line validator for local session.jsonl files.
 * Reads sequentially without loading the entire session into memory.
 */
public final class SessionValidator {

    private static final int MAX_RECORDED_ERRORS = 20;

    private SessionValidator() {}

    /**
     * Validates a session.jsonl file against the local schema.
     *
     * @param jsonlFile The file to validate.
     * @param expectedSessionId The expected session ID, or null to accept any uniform session ID.
     * @return ValidationResult containing line counts, validity flag, duration, and error details.
     */
    public static ValidationResult validateFile(File jsonlFile, String expectedSessionId) {
        if (jsonlFile == null || !jsonlFile.exists() || !jsonlFile.isFile()) {
            List<String> errors = new ArrayList<>();
            errors.add("File does not exist or is not a regular file: " + (jsonlFile != null ? jsonlFile.getAbsolutePath() : "null"));
            return new ValidationResult(false, 0, 0, 0, 0, null, null, 0, "File not found", errors);
        }

        long fileSizeBytes = jsonlFile.length();
        if (fileSizeBytes == 0) {
            List<String> errors = new ArrayList<>();
            errors.add("File is empty (0 bytes)");
            return new ValidationResult(false, 0, 0, 0, 0, null, null, 0, "File is empty", errors);
        }

        long validCount = 0;
        long invalidCount = 0;
        long totalLineCount = 0;
        String firstTimestamp = null;
        String lastTimestamp = null;
        String effectiveSessionId = expectedSessionId;
        long lastSequence = -1;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonlFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLineCount++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    invalidCount++;
                    if (errors.size() < MAX_RECORDED_ERRORS) {
                        errors.add("Line " + totalLineCount + ": Empty or blank line encountered");
                    }
                    continue;
                }

                try {
                    SessionEvent event = SessionEvent.fromJson(line);
                    // Verify session ID uniformity
                    if (effectiveSessionId == null) {
                        effectiveSessionId = event.getSessionId();
                    } else if (!effectiveSessionId.equals(event.getSessionId())) {
                        throw new IllegalArgumentException("Session ID mismatch: expected " + effectiveSessionId + " but found " + event.getSessionId());
                    }

                    // Verify sequence monotonicity
                    if (lastSequence != -1 && event.getSequence() <= lastSequence) {
                        // Non-monotonic sequence is logged as invalid record
                        throw new IllegalArgumentException("Non-monotonic sequence: previous " + lastSequence + ", current " + event.getSequence());
                    }
                    lastSequence = event.getSequence();

                    if (firstTimestamp == null) {
                        firstTimestamp = event.getTimestamp();
                    }
                    lastTimestamp = event.getTimestamp();
                    validCount++;
                } catch (Exception e) {
                    invalidCount++;
                    if (errors.size() < MAX_RECORDED_ERRORS) {
                        errors.add("Line " + totalLineCount + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            invalidCount++;
            if (errors.size() < MAX_RECORDED_ERRORS) {
                errors.add("I/O read failure during validation: " + e.getMessage());
            }
        }

        long durationMs = 0;
        if (firstTimestamp != null && lastTimestamp != null) {
            try {
                Instant start = Instant.parse(firstTimestamp);
                Instant end = Instant.parse(lastTimestamp);
                durationMs = Math.max(0, Duration.between(start, end).toMillis());
            } catch (Exception ignored) {}
        }

        boolean passed = (invalidCount == 0 && validCount > 0);
        String summary = passed
                ? "VALID: " + validCount + " records verified (" + fileSizeBytes + " bytes, " + durationMs + " ms duration)"
                : "INVALID: " + invalidCount + " invalid lines, " + validCount + " valid records (" + fileSizeBytes + " bytes)";

        return new ValidationResult(passed, validCount, invalidCount, totalLineCount, fileSizeBytes, firstTimestamp, lastTimestamp, durationMs, summary, errors);
    }
}
