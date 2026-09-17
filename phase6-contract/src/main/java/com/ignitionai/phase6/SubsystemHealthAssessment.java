package com.ignitionai.phase6;

import java.util.List;

public class SubsystemHealthAssessment {
    private final String subsystemId;
    private final String subsystemName;
    private final SeverityLevel severityLevel;
    private final Double healthScore;
    private final Double confidence;
    private final Double uncertainty;
    private final List<String> degradedDataFlags;
    private final List<String> evidenceReferences;
    private final boolean dataQualitySufficient;
    
    public SubsystemHealthAssessment(String subsystemId, String subsystemName, SeverityLevel severityLevel, 
                                     Double healthScore, Double confidence, Double uncertainty, 
                                     List<String> degradedDataFlags, List<String> evidenceReferences, boolean dataQualitySufficient) {
        this.subsystemId = subsystemId;
        this.subsystemName = subsystemName;
        this.severityLevel = severityLevel;
        this.healthScore = healthScore;
        this.confidence = confidence;
        this.uncertainty = uncertainty;
        this.degradedDataFlags = degradedDataFlags;
        this.evidenceReferences = evidenceReferences;
        this.dataQualitySufficient = dataQualitySufficient;
    }

    public SubsystemHealthAssessment(String subsystemId, Double healthScore, SeverityLevel severityLevel, 
                                     Double confidence, List<String> evidenceReferences, boolean dataQualitySufficient) {
        this(subsystemId, subsystemId, severityLevel, healthScore, confidence, 0.0, null, evidenceReferences, dataQualitySufficient);
    }

    public String getSubsystemId() { return subsystemId; }
    public String getSubsystemName() { return subsystemName != null ? subsystemName : subsystemId; }
    public SeverityLevel getSeverityLevel() { return severityLevel; }
    public SeverityLevel getSeverity() { return severityLevel; } // For compatibility
    public Double getHealthScore() { return healthScore; }
    public Double getScore() { return healthScore; } // For compatibility
    public Double getConfidence() { return confidence; }
    public Double getUncertainty() { return uncertainty; }
    public List<String> getDegradedDataFlags() { return degradedDataFlags; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public boolean isDataQualitySufficient() { return dataQualitySufficient; }
}
