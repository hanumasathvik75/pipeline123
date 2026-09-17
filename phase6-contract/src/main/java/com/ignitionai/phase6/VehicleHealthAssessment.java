package com.ignitionai.phase6;

import java.util.List;

public class VehicleHealthAssessment {
    private String vehicleId;
    private String vinOrIdentityStatus;
    private String vehicleConfiguration;
    private String assessmentTimestamp;
    private String assessmentWindow;
    private Double odometerKm;
    
    private Double vehicleHealthIndex;
    private VehicleHealthBand healthBand;
    private SeverityLevel overallSeverity;
    private Double overallConfidence;
    private Double overallUncertainty;
    private String dataQualityStatus;
    
    private List<SubsystemHealthAssessment> subsystemAssessments;
    
    private String calculationVersion;
    private String modelVersions;
    private String registryReleaseVersion;

    public VehicleHealthAssessment(String vehicleId, String vinOrIdentityStatus, String vehicleConfiguration,
                                   String assessmentTimestamp, String assessmentWindow, Double odometerKm,
                                   Double vehicleHealthIndex, VehicleHealthBand healthBand, SeverityLevel overallSeverity,
                                   Double overallConfidence, Double overallUncertainty, String dataQualityStatus,
                                   List<SubsystemHealthAssessment> subsystemAssessments,
                                   String calculationVersion, String modelVersions, String registryReleaseVersion) {
        this.vehicleId = vehicleId;
        this.vinOrIdentityStatus = vinOrIdentityStatus;
        this.vehicleConfiguration = vehicleConfiguration;
        this.assessmentTimestamp = assessmentTimestamp;
        this.assessmentWindow = assessmentWindow;
        this.odometerKm = odometerKm;
        this.vehicleHealthIndex = vehicleHealthIndex;
        this.healthBand = healthBand;
        this.overallSeverity = overallSeverity;
        this.overallConfidence = overallConfidence;
        this.overallUncertainty = overallUncertainty;
        this.dataQualityStatus = dataQualityStatus;
        this.subsystemAssessments = subsystemAssessments;
        this.calculationVersion = calculationVersion;
        this.modelVersions = modelVersions;
        this.registryReleaseVersion = registryReleaseVersion;
    }

    public String getVehicleId() { return vehicleId; }
    public String getVinOrIdentityStatus() { return vinOrIdentityStatus; }
    public String getVehicleConfiguration() { return vehicleConfiguration; }
    public String getAssessmentTimestamp() { return assessmentTimestamp; }
    public String getAssessmentWindow() { return assessmentWindow; }
    public Double getOdometerKm() { return odometerKm; }
    public Double getVehicleHealthIndex() { return vehicleHealthIndex; }
    public VehicleHealthBand getHealthBand() { return healthBand; }
    public SeverityLevel getOverallSeverity() { return overallSeverity; }
    public Double getOverallConfidence() { return overallConfidence; }
    public Double getOverallUncertainty() { return overallUncertainty; }
    public String getDataQualityStatus() { return dataQualityStatus; }
    public List<SubsystemHealthAssessment> getSubsystemAssessments() { return subsystemAssessments; }
    public String getCalculationVersion() { return calculationVersion; }
    public String getModelVersions() { return modelVersions; }
    public String getRegistryReleaseVersion() { return registryReleaseVersion; }
}
