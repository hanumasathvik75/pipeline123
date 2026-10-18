package ai.ignition.android.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diagnostic result model returned by Spring Boot Gateway to Android App.
 */
public class AssessmentResult {

    private final String sessionId;
    private final int vhiScore;
    private final String healthStatus;
    private final String dominantFault;
    private final double confidence;
    private final int framesEvaluated;
    private final int cleanFrames;
    private final int anomalyFrames;
    private final Map<String, Object> faultBreakdown;
    private final Map<String, Object> telemetrySummary;
    private final List<String> workshopInspectionPlan;
    private final String status;
    private final String message;

    public AssessmentResult(String sessionId, int vhiScore, String healthStatus,
                            String dominantFault, double confidence, int framesEvaluated,
                            int cleanFrames, int anomalyFrames,
                            Map<String, Object> faultBreakdown,
                            Map<String, Object> telemetrySummary,
                            List<String> workshopInspectionPlan,
                            String status, String message) {
        this.sessionId = sessionId;
        this.vhiScore = vhiScore;
        this.healthStatus = healthStatus;
        this.dominantFault = dominantFault;
        this.confidence = confidence;
        this.framesEvaluated = framesEvaluated;
        this.cleanFrames = cleanFrames;
        this.anomalyFrames = anomalyFrames;
        this.faultBreakdown = faultBreakdown;
        this.telemetrySummary = telemetrySummary;
        this.workshopInspectionPlan = workshopInspectionPlan != null ? workshopInspectionPlan : new ArrayList<>();
        this.status = status;
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public int getVhiScore() { return vhiScore; }
    public String getHealthStatus() { return healthStatus; }
    public String getDominantFault() { return dominantFault; }
    public double getConfidence() { return confidence; }
    public int getFramesEvaluated() { return framesEvaluated; }
    public int getCleanFrames() { return cleanFrames; }
    public int getAnomalyFrames() { return anomalyFrames; }
    public Map<String, Object> getFaultBreakdown() { return faultBreakdown; }
    public Map<String, Object> getTelemetrySummary() { return telemetrySummary; }
    public List<String> getWorkshopInspectionPlan() { return workshopInspectionPlan; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }

    public int getStatusColor() {
        if (vhiScore >= 85) return 0xFF1E8E3E; // Green
        if (vhiScore >= 65) return 0xFFE37400; // Orange / Amber
        return 0xFFD93025; // Red
    }

    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "Vehicle Health Index (VHI): %d / 100\n", vhiScore));
        sb.append("Health Status: ").append(healthStatus).append("\n");
        sb.append(String.format(Locale.ROOT, "Primary Condition: %s (%.1f%% confidence)\n", dominantFault, confidence * 100.0));
        sb.append(String.format(Locale.ROOT, "Frames Evaluated: %,d (Clean: %,d, Anomalies: %,d)\n\n", framesEvaluated, cleanFrames, anomalyFrames));

        if (!workshopInspectionPlan.isEmpty()) {
            sb.append("Workshop Physical Verification Steps:\n");
            for (String step : workshopInspectionPlan) {
                sb.append(" • ").append(step).append("\n");
            }
        }
        return sb.toString();
    }
}
