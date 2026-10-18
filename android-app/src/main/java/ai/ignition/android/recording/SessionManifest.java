package ai.ignition.android.recording;

import com.ignitionai.obdinput.storage.MiniJson;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Metadata manifest generated upon session finalization or recovery.
 * Provides validation summary, record counts, file size, and completion status.
 */
public final class SessionManifest {

    public static final String DEFAULT_SCHEMA_VERSION = "1.0-local";

    private final String schemaVersion;
    private final String sessionId;
    private final String startTime;
    private final String endTime;
    private final long durationMs;
    private final SessionStatus status;
    private final long recordCount;
    private final long validRecordCount;
    private final long invalidRecordCount;
    private final long fileSizeBytes;
    private final boolean validationPassed;
    private final String validationSummary;
    private final String finalizedAt;

    private SessionManifest(Builder b) {
        this.schemaVersion = b.schemaVersion != null ? b.schemaVersion : DEFAULT_SCHEMA_VERSION;
        this.sessionId = b.sessionId;
        this.startTime = b.startTime;
        this.endTime = b.endTime;
        this.durationMs = b.durationMs;
        this.status = b.status != null ? b.status : SessionStatus.RECORDING;
        this.recordCount = b.recordCount;
        this.validRecordCount = b.validRecordCount;
        this.invalidRecordCount = b.invalidRecordCount;
        this.fileSizeBytes = b.fileSizeBytes;
        this.validationPassed = b.validationPassed;
        this.validationSummary = b.validationSummary != null ? b.validationSummary : "No validation summary";
        this.finalizedAt = b.finalizedAt != null ? b.finalizedAt : Instant.now().toString();
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getSessionId() { return sessionId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public long getDurationMs() { return durationMs; }
    public SessionStatus getStatus() { return status; }
    public long getRecordCount() { return recordCount; }
    public long getValidRecordCount() { return validRecordCount; }
    public long getInvalidRecordCount() { return invalidRecordCount; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public boolean isValidationPassed() { return validationPassed; }
    public String getValidationSummary() { return validationSummary; }
    public String getFinalizedAt() { return finalizedAt; }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"schema_version\":").append(quote(schemaVersion));
        sb.append(",\"session_id\":").append(quote(sessionId));
        sb.append(",\"start_time\":").append(quote(startTime));
        sb.append(",\"end_time\":").append(quote(endTime));
        sb.append(",\"duration_ms\":").append(durationMs);
        sb.append(",\"status\":").append(quote(status.name()));
        sb.append(",\"record_count\":").append(recordCount);
        sb.append(",\"valid_record_count\":").append(validRecordCount);
        sb.append(",\"invalid_record_count\":").append(invalidRecordCount);
        sb.append(",\"file_size_bytes\":").append(fileSizeBytes);
        sb.append(",\"validation_passed\":").append(validationPassed);
        sb.append(",\"validation_summary\":").append(quote(validationSummary));
        sb.append(",\"finalized_at\":").append(quote(finalizedAt));
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static SessionManifest fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Manifest JSON cannot be null or empty");
        }
        Object parsed = MiniJson.parse(json.trim());
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Root manifest structure must be an object");
        }
        Map<String, Object> map = (Map<String, Object>) parsed;

        Builder b = builder();
        if (map.containsKey("schema_version")) b.schemaVersion((String) map.get("schema_version"));
        if (map.containsKey("session_id")) b.sessionId((String) map.get("session_id"));
        if (map.containsKey("start_time")) b.startTime((String) map.get("start_time"));
        if (map.containsKey("end_time")) b.endTime((String) map.get("end_time"));
        if (map.containsKey("duration_ms") && map.get("duration_ms") instanceof Number) {
            b.durationMs(((Number) map.get("duration_ms")).longValue());
        }
        if (map.containsKey("status") && map.get("status") != null) {
            b.status(SessionStatus.valueOf(((String) map.get("status")).toUpperCase(Locale.ROOT)));
        }
        if (map.containsKey("record_count") && map.get("record_count") instanceof Number) {
            b.recordCount(((Number) map.get("record_count")).longValue());
        }
        if (map.containsKey("valid_record_count") && map.get("valid_record_count") instanceof Number) {
            b.validRecordCount(((Number) map.get("valid_record_count")).longValue());
        }
        if (map.containsKey("invalid_record_count") && map.get("invalid_record_count") instanceof Number) {
            b.invalidRecordCount(((Number) map.get("invalid_record_count")).longValue());
        }
        if (map.containsKey("file_size_bytes") && map.get("file_size_bytes") instanceof Number) {
            b.fileSizeBytes(((Number) map.get("file_size_bytes")).longValue());
        }
        if (map.containsKey("validation_passed") && map.get("validation_passed") instanceof Boolean) {
            b.validationPassed((Boolean) map.get("validation_passed"));
        }
        if (map.containsKey("validation_summary")) b.validationSummary((String) map.get("validation_summary"));
        if (map.containsKey("finalized_at")) b.finalizedAt((String) map.get("finalized_at"));

        return b.build();
    }

    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 32) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String schemaVersion = DEFAULT_SCHEMA_VERSION;
        private String sessionId;
        private String startTime;
        private String endTime;
        private long durationMs = 0;
        private SessionStatus status = SessionStatus.RECORDING;
        private long recordCount = 0;
        private long validRecordCount = 0;
        private long invalidRecordCount = 0;
        private long fileSizeBytes = 0;
        private boolean validationPassed = false;
        private String validationSummary;
        private String finalizedAt;

        public Builder schemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder status(SessionStatus status) { this.status = status; return this; }
        public Builder recordCount(long recordCount) { this.recordCount = recordCount; return this; }
        public Builder validRecordCount(long validRecordCount) { this.validRecordCount = validRecordCount; return this; }
        public Builder invalidRecordCount(long invalidRecordCount) { this.invalidRecordCount = invalidRecordCount; return this; }
        public Builder fileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder validationPassed(boolean validationPassed) { this.validationPassed = validationPassed; return this; }
        public Builder validationSummary(String validationSummary) { this.validationSummary = validationSummary; return this; }
        public Builder finalizedAt(String finalizedAt) { this.finalizedAt = finalizedAt; return this; }

        public SessionManifest build() {
            return new SessionManifest(this);
        }
    }
}
