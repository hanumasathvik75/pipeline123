package com.ignitionai.phase7.snapshot;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.virtualsensors.index.DiagnosticIndexResult;
import com.ignitionai.virtualsensors.index.FaultCandidate;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CertificateSnapshot {
    private String certificateId;
    private String certificateSchemaVersion;

    private VehicleHealthAssessment healthAssessment;
    private final String sourceType;
    private final String sessionId;
    private final String evidenceDigest;

    // Evidence-appendix fields. Optional/settable so the two original constructors (and every
    // existing caller/test that uses them) keep working unchanged; InspectionService populates
    // these after construction. Never null after construction — default to empty.
    private List<String> dtcCodes = List.of();
    private List<AnomalyEpisode> anomalyEpisodes = List.of();
    private List<DegradationOutput> degradationOutputs = List.of();
    private List<DiagnosticIndexResult> diagnosticIndex = List.of();
    private List<FaultCandidate> rankedFaultCandidates = List.of();
    private Long captureDurationMs;
    private Integer recordCount;

    public CertificateSnapshot(VehicleHealthAssessment healthAssessment) {
        this(healthAssessment, "UNSPECIFIED", "UNSPECIFIED", "UNSPECIFIED");
    }

    public CertificateSnapshot(VehicleHealthAssessment healthAssessment, String sourceType, String sessionId, String evidenceDigest) {
        this.healthAssessment = healthAssessment;
        this.sourceType = sourceType;
        this.sessionId = sessionId;
        this.evidenceDigest = evidenceDigest;
        this.certificateSchemaVersion = "1.1.0";
        this.certificateId = generateDeterministicId(healthAssessment);
    }

    public void setEvidenceAppendix(List<String> dtcCodes, List<AnomalyEpisode> anomalyEpisodes,
            List<DegradationOutput> degradationOutputs, List<DiagnosticIndexResult> diagnosticIndex,
            List<FaultCandidate> rankedFaultCandidates, Long captureDurationMs, Integer recordCount) {
        this.dtcCodes = dtcCodes == null ? List.of() : List.copyOf(dtcCodes);
        this.anomalyEpisodes = anomalyEpisodes == null ? List.of() : List.copyOf(anomalyEpisodes);
        this.degradationOutputs = degradationOutputs == null ? List.of() : List.copyOf(degradationOutputs);
        this.diagnosticIndex = diagnosticIndex == null ? List.of() : List.copyOf(diagnosticIndex);
        this.rankedFaultCandidates = rankedFaultCandidates == null ? List.of() : List.copyOf(rankedFaultCandidates);
        this.captureDurationMs = captureDurationMs;
        this.recordCount = recordCount;
    }

    public List<String> getDtcCodes() { return dtcCodes; }
    public List<AnomalyEpisode> getAnomalyEpisodes() { return anomalyEpisodes; }
    public List<DegradationOutput> getDegradationOutputs() { return degradationOutputs; }
    public List<DiagnosticIndexResult> getDiagnosticIndex() { return diagnosticIndex; }
    public List<FaultCandidate> getRankedFaultCandidates() { return rankedFaultCandidates; }
    public Long getCaptureDurationMs() { return captureDurationMs; }
    public Integer getRecordCount() { return recordCount; }

    private String generateDeterministicId(VehicleHealthAssessment assessment) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String content = assessment.getVehicleId() + "|" + 
                             assessment.getAssessmentTimestamp() + "|" + 
                             assessment.getVehicleHealthIndex() + "|" + 
                             assessment.getCalculationVersion() + "|" + sourceType + "|" + sessionId + "|" + evidenceDigest;
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "CERT-" + hexString.toString().substring(0, 16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return "CERT-" + UUID.randomUUID().toString();
        }
    }

    public String getCertificateId() { return certificateId; }
    public String getCertificateSchemaVersion() { return certificateSchemaVersion; }
    public VehicleHealthAssessment getHealthAssessment() { return healthAssessment; }
    public String getSourceType() { return sourceType; }
    public String getSessionId() { return sessionId; }
    public String getEvidenceDigest() { return evidenceDigest; }
}
