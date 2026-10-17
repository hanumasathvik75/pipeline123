package ai.ignition.android.recording;

import ai.ignition.android.util.RedactionUtil;
import com.ignitionai.obdinput.storage.MiniJson;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Immutable model representing an event line in a local JSONL session file.
 * Each line must be independently parseable, newline-terminated, and conform to the versioned schema.
 */
public final class SessionEvent {

    public static final String DEFAULT_SCHEMA_VERSION = "1.0-local";

    private final String schemaVersion;
    private final String sessionId;
    private final long sequence;
    private final String timestamp;
    private final EventType eventType;

    // Optional / event-specific fields
    private final String signalName;
    private final Double value;
    private final String unit;
    private final ObservationStatus status;
    private final String source;
    private final String phaseId;
    private final String errorCategory;
    private final String errorMessage;
    private final Map<String, String> details;

    private SessionEvent(Builder builder) {
        this.schemaVersion = builder.schemaVersion != null ? builder.schemaVersion : DEFAULT_SCHEMA_VERSION;
        this.sessionId = builder.sessionId;
        this.sequence = builder.sequence;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now().toString();
        this.eventType = builder.eventType;
        this.signalName = builder.signalName;
        this.value = builder.value;
        this.unit = builder.unit;
        this.status = builder.status;
        this.source = builder.source;
        this.phaseId = builder.phaseId;
        this.errorCategory = builder.errorCategory;
        this.errorMessage = sanitizeMessage(builder.errorMessage);
        this.details = builder.details != null ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.details)) : Collections.emptyMap();
    }

    private static String sanitizeMessage(String message) {
        if (message == null) return null;
        String redacted = RedactionUtil.redactMac(message);
        return RedactionUtil.redactVinInText(redacted);
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getSessionId() { return sessionId; }
    public long getSequence() { return sequence; }
    public String getTimestamp() { return timestamp; }
    public EventType getEventType() { return eventType; }
    public String getSignalName() { return signalName; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public ObservationStatus getStatus() { return status; }
    public String getSource() { return source; }
    public String getPhaseId() { return phaseId; }
    public String getErrorCategory() { return errorCategory; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, String> getDetails() { return details; }

    /**
     * Validates whether this event satisfies all schema constraints.
     * Throws IllegalArgumentException with specific reason if invalid.
     */
    public void validate() {
        if (schemaVersion == null || schemaVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("schema_version cannot be null or empty");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("session_id cannot be null or empty");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative: " + sequence);
        }
        if (timestamp == null || timestamp.trim().isEmpty()) {
            throw new IllegalArgumentException("timestamp cannot be null or empty");
        }
        try {
            Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("timestamp must be valid ISO-8601 with timezone: " + timestamp, e);
        }
        if (eventType == null) {
            throw new IllegalArgumentException("event_type cannot be null");
        }

        if (eventType == EventType.OBD_OBSERVATION) {
            if (signalName == null || signalName.trim().isEmpty()) {
                throw new IllegalArgumentException("OBD_OBSERVATION requires a valid signal_name");
            }
            if (status == null) {
                throw new IllegalArgumentException("OBD_OBSERVATION requires an explicit ObservationStatus");
            }
            if (status == ObservationStatus.VALID) {
                if (value == null || !Double.isFinite(value)) {
                    throw new IllegalArgumentException("VALID OBD_OBSERVATION must have a finite numerical value");
                }
            } else {
                if (value != null) {
                    throw new IllegalArgumentException("Unavailable OBD_OBSERVATION (" + status + ") must not have a synthetic numeric value");
                }
            }
        }

        if (eventType == EventType.PHASE_CHANGED) {
            if (phaseId == null || phaseId.trim().isEmpty()) {
                throw new IllegalArgumentException("PHASE_CHANGED requires non-empty phase_id");
            }
        }
    }

    /**
     * Serializes this event into a single-line JSON string without internal newlines.
     */
    public String toJson() {
        validate();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"schema_version\":").append(quote(schemaVersion));
        sb.append(",\"session_id\":").append(quote(sessionId));
        sb.append(",\"sequence\":").append(sequence);
        sb.append(",\"timestamp\":").append(quote(timestamp));
        sb.append(",\"event_type\":").append(quote(eventType.name()));

        if (signalName != null) {
            sb.append(",\"signal_name\":").append(quote(signalName));
        }
        if (value != null) {
            sb.append(",\"value\":").append(value);
        }
        if (unit != null) {
            sb.append(",\"unit\":").append(quote(unit));
        }
        if (status != null) {
            sb.append(",\"status\":").append(quote(status.name()));
        }
        if (source != null) {
            sb.append(",\"source\":").append(quote(source));
        }
        if (phaseId != null) {
            sb.append(",\"phase_id\":").append(quote(phaseId));
        }
        if (errorCategory != null) {
            sb.append(",\"error_category\":").append(quote(errorCategory));
        }
        if (errorMessage != null) {
            sb.append(",\"error_message\":").append(quote(errorMessage));
        }
        if (!details.isEmpty()) {
            sb.append(",\"details\":{");
            int i = 0;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                if (i > 0) sb.append(",");
                sb.append(quote(entry.getKey())).append(":").append(quote(sanitizeMessage(entry.getValue())));
                i++;
            }
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Parses a single line JSON string into a SessionEvent.
     */
    @SuppressWarnings("unchecked")
    public static SessionEvent fromJson(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON line cannot be null or empty");
        }
        if (line.contains("\n") || line.contains("\r")) {
            throw new IllegalArgumentException("JSON line must not contain unescaped newline characters");
        }

        Object parsed = MiniJson.parse(line.trim());
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Root JSON structure must be an object");
        }
        Map<String, Object> map = (Map<String, Object>) parsed;

        Builder b = builder();
        if (map.containsKey("schema_version")) {
            b.schemaVersion((String) map.get("schema_version"));
        }
        if (map.containsKey("session_id")) {
            b.sessionId((String) map.get("session_id"));
        }
        if (map.containsKey("sequence")) {
            Object seq = map.get("sequence");
            if (seq instanceof Number) {
                b.sequence(((Number) seq).longValue());
            }
        }
        if (map.containsKey("timestamp")) {
            b.timestamp((String) map.get("timestamp"));
        }
        if (map.containsKey("event_type")) {
            String et = (String) map.get("event_type");
            b.eventType(EventType.valueOf(et.toUpperCase(Locale.ROOT)));
        }
        if (map.containsKey("signal_name")) {
            b.signalName((String) map.get("signal_name"));
        }
        if (map.containsKey("value") && map.get("value") != null) {
            Object v = map.get("value");
            if (v instanceof Number) {
                b.value(((Number) v).doubleValue());
            }
        }
        if (map.containsKey("unit")) {
            b.unit((String) map.get("unit"));
        }
        if (map.containsKey("status") && map.get("status") != null) {
            b.status(ObservationStatus.valueOf(((String) map.get("status")).toUpperCase(Locale.ROOT)));
        }
        if (map.containsKey("source")) {
            b.source((String) map.get("source"));
        }
        if (map.containsKey("phase_id")) {
            b.phaseId((String) map.get("phase_id"));
        }
        if (map.containsKey("error_category")) {
            b.errorCategory((String) map.get("error_category"));
        }
        if (map.containsKey("error_message")) {
            b.errorMessage((String) map.get("error_message"));
        }
        if (map.containsKey("details") && map.get("details") instanceof Map) {
            Map<String, Object> detailsMap = (Map<String, Object>) map.get("details");
            for (Map.Entry<String, Object> e : detailsMap.entrySet()) {
                if (e.getValue() != null) {
                    b.addDetail(e.getKey(), e.getValue().toString());
                }
            }
        }

        SessionEvent event = b.build();
        event.validate();
        return event;
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
        private long sequence = 0;
        private String timestamp;
        private EventType eventType;
        private String signalName;
        private Double value;
        private String unit;
        private ObservationStatus status;
        private String source;
        private String phaseId;
        private String errorCategory;
        private String errorMessage;
        private final Map<String, String> details = new LinkedHashMap<>();

        public Builder schemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder sequence(long sequence) { this.sequence = sequence; return this; }
        public Builder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public Builder eventType(EventType eventType) { this.eventType = eventType; return this; }
        public Builder signalName(String signalName) { this.signalName = signalName; return this; }
        public Builder value(Double value) { this.value = value; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder status(ObservationStatus status) { this.status = status; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder phaseId(String phaseId) { this.phaseId = phaseId; return this; }
        public Builder errorCategory(String errorCategory) { this.errorCategory = errorCategory; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder addDetail(String key, String value) {
            if (key != null && value != null) {
                this.details.put(key, value);
            }
            return this;
        }

        public SessionEvent build() {
            return new SessionEvent(this);
        }
    }
}
