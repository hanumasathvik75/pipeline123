package com.ignitionai.phase7.snapshot;

import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;

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
