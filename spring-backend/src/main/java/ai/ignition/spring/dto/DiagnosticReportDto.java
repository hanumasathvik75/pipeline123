package ai.ignition.spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Clean diagnostic assessment report returned by Spring Boot Gateway to Android App.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiagnosticReportDto {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("vhi_score")
    private int vhiScore;

    @JsonProperty("health_status")
    private String healthStatus;

    @JsonProperty("dominant_fault")
    private String dominantFault;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("frames_evaluated")
    private int framesEvaluated;

    @JsonProperty("clean_frames")
    private int cleanFrames;

    @JsonProperty("anomaly_frames")
    private int anomalyFrames;

    @JsonProperty("fault_breakdown")
    private Map<String, Map<String, Object>> faultBreakdown;

    @JsonProperty("telemetry_summary")
    private Map<String, Object> telemetrySummary;

    @JsonProperty("workshop_inspection_plan")
    private List<String> workshopInspectionPlan;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    public DiagnosticReportDto() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getVhiScore() { return vhiScore; }
    public void setVhiScore(int vhiScore) { this.vhiScore = vhiScore; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getDominantFault() { return dominantFault; }
    public void setDominantFault(String dominantFault) { this.dominantFault = dominantFault; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public int getFramesEvaluated() { return framesEvaluated; }
    public void setFramesEvaluated(int framesEvaluated) { this.framesEvaluated = framesEvaluated; }

    public int getCleanFrames() { return cleanFrames; }
    public void setCleanFrames(int cleanFrames) { this.cleanFrames = cleanFrames; }

    public int getAnomalyFrames() { return anomalyFrames; }
    public void setAnomalyFrames(int anomalyFrames) { this.anomalyFrames = anomalyFrames; }

    public Map<String, Map<String, Object>> getFaultBreakdown() { return faultBreakdown; }
    public void setFaultBreakdown(Map<String, Map<String, Object>> faultBreakdown) { this.faultBreakdown = faultBreakdown; }

    public Map<String, Object> getTelemetrySummary() { return telemetrySummary; }
    public void setTelemetrySummary(Map<String, Object> telemetrySummary) { this.telemetrySummary = telemetrySummary; }

    public List<String> getWorkshopInspectionPlan() { return workshopInspectionPlan; }
    public void setWorkshopInspectionPlan(List<String> workshopInspectionPlan) { this.workshopInspectionPlan = workshopInspectionPlan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
