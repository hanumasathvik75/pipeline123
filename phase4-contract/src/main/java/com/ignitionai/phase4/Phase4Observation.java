package com.ignitionai.phase4;

import java.util.List;
import java.util.Collections;

public class Phase4Observation {
    private final String vehicleId;
    private final String sessionId;
    private final String subsystemOrSignalId;
    private final List<String> sourceObservationReferences;
    
    private final Double observedValue;
    private final Double expectedValue;
    private final Double residual;
    private final Double uncertainty;
    private final Double normalizedResidual;
    
    private final Double anomalyScore;
    private final AnomalyState anomalyState;
    
    private final Long timestampMs;
    private final String vehicleContextVersion;
    private final String modelVersion;
    private final String configurationVersion;
    
    private final List<String> evidenceReferences;

    public Phase4Observation(String vehicleId, String sessionId, String subsystemOrSignalId, 
                             List<String> sourceObservationReferences, Double observedValue, 
                             Double expectedValue, Double residual, Double uncertainty, 
                             Double normalizedResidual, Double anomalyScore, 
                             AnomalyState anomalyState, Long timestampMs, 
                             String vehicleContextVersion, String modelVersion, 
                             String configurationVersion, List<String> evidenceReferences) {
        this.vehicleId = vehicleId;
        this.sessionId = sessionId;
        this.subsystemOrSignalId = subsystemOrSignalId;
        this.sourceObservationReferences = sourceObservationReferences != null ? Collections.unmodifiableList(sourceObservationReferences) : Collections.emptyList();
        this.observedValue = observedValue;
        this.expectedValue = expectedValue;
        this.residual = residual;
        this.uncertainty = uncertainty;
        this.normalizedResidual = normalizedResidual;
        this.anomalyScore = anomalyScore;
        this.anomalyState = anomalyState != null ? anomalyState : AnomalyState.NOMINAL;
        this.timestampMs = timestampMs;
        this.vehicleContextVersion = vehicleContextVersion;
        this.modelVersion = modelVersion;
        this.configurationVersion = configurationVersion;
        this.evidenceReferences = evidenceReferences != null ? Collections.unmodifiableList(evidenceReferences) : Collections.emptyList();
    }

    public String getVehicleId() { return vehicleId; }
    public String getSessionId() { return sessionId; }
    public String getSubsystemOrSignalId() { return subsystemOrSignalId; }
    public List<String> getSourceObservationReferences() { return sourceObservationReferences; }
    public Double getObservedValue() { return observedValue; }
    public Double getExpectedValue() { return expectedValue; }
    public Double getResidual() { return residual; }
    public Double getUncertainty() { return uncertainty; }
    public Double getNormalizedResidual() { return normalizedResidual; }
    public Double getAnomalyScore() { return anomalyScore; }
    public AnomalyState getAnomalyState() { return anomalyState; }
    public Long getTimestampMs() { return timestampMs; }
    public String getVehicleContextVersion() { return vehicleContextVersion; }
    public String getModelVersion() { return modelVersion; }
    public String getConfigurationVersion() { return configurationVersion; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }

    public Phase4Observation withAnomalyResult(Double score, AnomalyState state) {
        return new Phase4Observation(
            this.vehicleId, this.sessionId, this.subsystemOrSignalId, this.sourceObservationReferences,
            this.observedValue, this.expectedValue, this.residual, this.uncertainty,
            this.normalizedResidual, score, state, this.timestampMs,
            this.vehicleContextVersion, this.modelVersion, this.configurationVersion,
            this.evidenceReferences
        );
    }
}
