package com.ignitionai.phase6;

public class Phase6ContractValidator {

    public static void validateContract(VehicleHealthAssessment ha) {
        if (ha == null) throw new IllegalArgumentException("VehicleHealthAssessment is null");
        if (ha.getVehicleId() == null) throw new IllegalArgumentException("Missing vehicleId");
        if (ha.getAssessmentTimestamp() == null && ha.getAssessmentTimestampMs() == null) throw new IllegalArgumentException("Missing assessmentTimestamp");
        if (ha.getCalculationVersion() == null) throw new IllegalArgumentException("Missing calculationVersion");
        
        Double vhi = ha.getVehicleHealthIndex();
        if (vhi != null && (vhi < 0.0 || vhi > 100.0)) {
            throw new IllegalArgumentException("VHI must be between 0 and 100");
        }
        
        if (ha.getOverallConfidence() != null && (ha.getOverallConfidence() < 0.0 || ha.getOverallConfidence() > 1.0)) {
            throw new IllegalArgumentException("Overall confidence must be between 0 and 1");
        }
        
        if (ha.getOverallUncertainty() != null && (ha.getOverallUncertainty() < 0.0 || ha.getOverallUncertainty() > 1.0)) {
            throw new IllegalArgumentException("Overall uncertainty must be between 0 and 1");
        }
        
        if (ha.getSubsystemCoverage() != null && (ha.getSubsystemCoverage() < 0.0 || ha.getSubsystemCoverage() > 1.0)) {
            throw new IllegalArgumentException("Subsystem coverage must be between 0 and 1");
        }

        if (ha.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment sub : ha.getSubsystemAssessments()) {
                if (sub.getSubsystemId() == null) throw new IllegalArgumentException("Subsystem missing ID");
                if (sub.getSeverityLevel() == null) throw new IllegalArgumentException("Subsystem missing severity");
                
                Double score = sub.getHealthScore();
                if (score != null && (score < 0.0 || score > 100.0)) {
                    throw new IllegalArgumentException("Subsystem healthScore must be between 0 and 100");
                }
                
                Double conf = sub.getConfidence();
                if (conf != null && (conf < 0.0 || conf > 1.0)) {
                    throw new IllegalArgumentException("Subsystem confidence must be between 0 and 1");
                }
                
                Double unc = sub.getUncertainty();
                if (unc != null && (unc < 0.0 || unc > 1.0)) {
                    throw new IllegalArgumentException("Subsystem uncertainty must be between 0 and 1");
                }
            }
        }
    }
}
