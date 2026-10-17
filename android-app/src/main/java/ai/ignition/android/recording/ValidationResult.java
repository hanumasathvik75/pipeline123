package ai.ignition.android.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of line-by-line validation of a local session.jsonl file.
 */
public final class ValidationResult {

    private final boolean valid;
    private final long validRecordCount;
    private final long invalidRecordCount;
    private final long totalLineCount;
    private final long fileSizeBytes;
    private final String firstTimestamp;
    private final String lastTimestamp;
    private final long durationMs;
    private final String summary;
    private final List<String> errors;

    public ValidationResult(boolean valid,
                            long validRecordCount,
                            long invalidRecordCount,
                            long totalLineCount,
                            long fileSizeBytes,
                            String firstTimestamp,
                            String lastTimestamp,
                            long durationMs,
                            String summary,
                            List<String> errors) {
        this.valid = valid;
        this.validRecordCount = validRecordCount;
        this.invalidRecordCount = invalidRecordCount;
        this.totalLineCount = totalLineCount;
        this.fileSizeBytes = fileSizeBytes;
        this.firstTimestamp = firstTimestamp;
        this.lastTimestamp = lastTimestamp;
        this.durationMs = durationMs;
        this.summary = summary != null ? summary : "";
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    public boolean isValid() { return valid; }
    public long getValidRecordCount() { return validRecordCount; }
    public long getInvalidRecordCount() { return invalidRecordCount; }
    public long getTotalLineCount() { return totalLineCount; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getFirstTimestamp() { return firstTimestamp; }
    public String getLastTimestamp() { return lastTimestamp; }
    public long getDurationMs() { return durationMs; }
    public String getSummary() { return summary; }
    public List<String> getErrors() { return errors; }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", validRecordCount=" + validRecordCount +
                ", invalidRecordCount=" + invalidRecordCount +
                ", totalLineCount=" + totalLineCount +
                ", fileSizeBytes=" + fileSizeBytes +
                ", durationMs=" + durationMs +
                ", summary='" + summary + '\'' +
                '}';
    }
}
