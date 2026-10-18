package ai.ignition.spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Incoming request from Android app containing recorded session data to assess.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionAssessmentRequest {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("vin")
    private String vin;

    @JsonProperty("raw_jsonl")
    private String rawJsonl;

    @JsonProperty("records")
    private List<Map<String, Object>> records;

    @JsonProperty("file_path")
    private String filePath;

    public SessionAssessmentRequest() {}

    public SessionAssessmentRequest(String sessionId, String rawJsonl) {
        this.sessionId = sessionId;
        this.rawJsonl = rawJsonl;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getRawJsonl() { return rawJsonl; }
    public void setRawJsonl(String rawJsonl) { this.rawJsonl = rawJsonl; }

    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
