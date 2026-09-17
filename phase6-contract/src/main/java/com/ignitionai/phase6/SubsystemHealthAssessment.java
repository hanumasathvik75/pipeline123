package com.ignitionai.phase6;

import java.util.List;

public class SubsystemHealthAssessment {
    private String subsystemName;
    private Double score; // 0.0 to 100.0, null if not assessed
    private SeverityLevel severity;
    private Double confidence;
    private List<String> evidenceReferences;
    private boolean dataQualitySufficient;

    public SubsystemHealthAssessment(String subsystemName, Double score, SeverityLevel severity, 
                                     Double confidence, List<String> evidenceReferences, boolean dataQualitySufficient) {
        this.subsystemName = subsystemName;
        this.score = score;
        this.severity = severity;
        this.confidence = confidence;
        this.evidenceReferences = evidenceReferences;
        this.dataQualitySufficient = dataQualitySufficient;
    }

    public String getSubsystemName() { return subsystemName; }
    public Double getScore() { return score; }
    public SeverityLevel getSeverity() { return severity; }
    public Double getConfidence() { return confidence; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public boolean isDataQualitySufficient() { return dataQualitySufficient; }
}
