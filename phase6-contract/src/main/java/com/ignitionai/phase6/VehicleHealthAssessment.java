package com.ignitionai.phase6;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class VehicleHealthAssessment {
    private final String vehicleId;
    private final Long assessmentTimestampMs;
    private final Long assessmentWindowMs;
    
    private final String vinOrIdentityStatus;
    private final String vehicleConfiguration;
    private final String assessmentTimestamp;
    private final String assessmentWindow;
    private final Double odometerKm;

    private final Double vehicleHealthIndex;
    private final VehicleHealthBand vehicleHealthBand;
    private final SeverityLevel overallSeverity;
    private final Double overallConfidence;
    private final Double overallUncertainty;
    private final String dataQualityStatus;

    private final Double subsystemCoverage;
    private final List<String> excludedSubsystemIds;
    private final Map<String, String> exclusionReasons;
    
    private final List<SubsystemHealthAssessment> subsystemAssessments;
    private final Map<String, Double> componentWeights;
    
    private final List<String> evidenceReferences;
    private final String calculationVersion;
    private final String registryReleaseVersion;
    private final String contextVersion;
    private final String modelVersions;
    
    public VehicleHealthAssessment(String vehicleId, Long assessmentTimestampMs, Long assessmentWindowMs,
                                   String vinOrIdentityStatus, String vehicleConfiguration,
                                   String assessmentTimestamp, String assessmentWindow, Double odometerKm,
                                   Double vehicleHealthIndex, VehicleHealthBand vehicleHealthBand,
                                   SeverityLevel overallSeverity, Double overallConfidence, Double overallUncertainty,
                                   String dataQualityStatus, Double subsystemCoverage,
                                   List<String> excludedSubsystemIds, Map<String, String> exclusionReasons,
                                   List<SubsystemHealthAssessment> subsystemAssessments,
                                   Map<String, Double> componentWeights,
                                   List<String> evidenceReferences, String calculationVersion,
                                   String registryReleaseVersion, String contextVersion, String modelVersions) {
        this.vehicleId = vehicleId;
        this.assessmentTimestampMs = assessmentTimestampMs;
        this.assessmentWindowMs = assessmentWindowMs;
        this.vinOrIdentityStatus = vinOrIdentityStatus;
        this.vehicleConfiguration = vehicleConfiguration;
        this.assessmentTimestamp = assessmentTimestamp;
        this.assessmentWindow = assessmentWindow;
        this.odometerKm = odometerKm;
        
        this.vehicleHealthIndex = vehicleHealthIndex;
        this.vehicleHealthBand = vehicleHealthBand;
        this.overallSeverity = overallSeverity;
        this.overallConfidence = overallConfidence;
        this.overallUncertainty = overallUncertainty;
        this.dataQualityStatus = dataQualityStatus;
        
        this.subsystemCoverage = subsystemCoverage;
        this.excludedSubsystemIds = excludedSubsystemIds != null ? Collections.unmodifiableList(excludedSubsystemIds) : Collections.emptyList();
        this.exclusionReasons = exclusionReasons != null ? Collections.unmodifiableMap(exclusionReasons) : Collections.emptyMap();

        this.subsystemAssessments = subsystemAssessments != null ? Collections.unmodifiableList(subsystemAssessments) : Collections.emptyList();
        this.componentWeights = componentWeights != null ? Collections.unmodifiableMap(componentWeights) : Collections.emptyMap();
        this.evidenceReferences = evidenceReferences != null ? Collections.unmodifiableList(evidenceReferences) : Collections.emptyList();
        this.calculationVersion = calculationVersion;
        this.registryReleaseVersion = registryReleaseVersion;
        this.contextVersion = contextVersion;
        this.modelVersions = modelVersions;
    }

    public VehicleHealthAssessment(String vehicleId, String vinOrIdentityStatus, String vehicleConfiguration,
                                   String assessmentTimestamp, String assessmentWindow, Double odometerKm,
                                   Double vehicleHealthIndex, VehicleHealthBand vehicleHealthBand,
                                   SeverityLevel overallSeverity, Double overallConfidence, Double overallUncertainty,
                                   String dataQualityStatus, List<SubsystemHealthAssessment> subsystemAssessments,
                                   String calculationVersion, String registryReleaseVersion, String modelVersions) {
        this(vehicleId, null, null, vinOrIdentityStatus, vehicleConfiguration, assessmentTimestamp, assessmentWindow, odometerKm,
             vehicleHealthIndex, vehicleHealthBand, overallSeverity, overallConfidence, overallUncertainty, dataQualityStatus,
             1.0, null, null, subsystemAssessments, null, null, calculationVersion, registryReleaseVersion, modelVersions, modelVersions);
    }

    public String getVehicleId() { return vehicleId; }
    public Long getAssessmentTimestampMs() { return assessmentTimestampMs; }
    public Long getAssessmentWindowMs() { return assessmentWindowMs; }
    public String getVinOrIdentityStatus() { return vinOrIdentityStatus; }
    public String getVehicleConfiguration() { return vehicleConfiguration; }
    public String getAssessmentTimestamp() { return assessmentTimestamp != null ? assessmentTimestamp : (assessmentTimestampMs != null ? assessmentTimestampMs.toString() : null); }
    public String getAssessmentWindow() { return assessmentWindow != null ? assessmentWindow : (assessmentWindowMs != null ? assessmentWindowMs.toString() : null); }
    public Double getOdometerKm() { return odometerKm; }

    public Double getVehicleHealthIndex() { return vehicleHealthIndex; }
    public VehicleHealthBand getVehicleHealthBand() { return vehicleHealthBand; }
    public VehicleHealthBand getHealthBand() { return vehicleHealthBand; } // Compatibility
    public SeverityLevel getOverallSeverity() { return overallSeverity; }
    public Double getOverallConfidence() { return overallConfidence; }
    public Double getOverallUncertainty() { return overallUncertainty; }
    public String getDataQualityStatus() { return dataQualityStatus; }

    public Double getSubsystemCoverage() { return subsystemCoverage; }
    public List<String> getExcludedSubsystemIds() { return excludedSubsystemIds; }
    public Map<String, String> getExclusionReasons() { return exclusionReasons; }

    public List<SubsystemHealthAssessment> getSubsystemAssessments() { return subsystemAssessments; }
    public Map<String, Double> getComponentWeights() { return componentWeights; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    
    public String getCalculationVersion() { return calculationVersion; }
    public String getRegistryReleaseVersion() { return registryReleaseVersion; }
    public String getContextVersion() { return contextVersion; }
    public String getModelVersions() { return modelVersions != null ? modelVersions : contextVersion; }
}
