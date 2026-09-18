package com.ignitionai.vhi;

import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase6.VehicleHealthBand;
import com.ignitionai.phase6.SeverityLevel;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class VhiCalculator {

    public VehicleHealthAssessment calculateVhi(String vehicleId, Long timestampMs, Long windowMs, 
                                                List<SubsystemHealthAssessment> subsystemAssessments, 
                                                Map<String, Double> componentWeights,
                                                List<String> evidenceReferences, String calculationVersion,
                                                String registryVersion, String contextVersion) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map.Entry<String, Double> e : componentWeights.entrySet()) {
            if (e.getValue() == null || !Double.isFinite(e.getValue()) || e.getValue() <= 0)
                throw new IllegalArgumentException("Weights must be finite and positive");
        }
        for (SubsystemHealthAssessment sub : subsystemAssessments) {
            if (!seen.add(sub.getSubsystemId())) throw new IllegalArgumentException("Duplicate subsystem");
            if (!componentWeights.containsKey(sub.getSubsystemId())) throw new IllegalArgumentException("Missing subsystem weight");
        }
        double weightedSum = 0.0;
        double validWeightSum = 0.0;
        double totalWeightSum = componentWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double weightedConfidence = 0.0;
        double weightedUncertainty = 0.0;
        
        SeverityLevel overallSeverity = SeverityLevel.UNKNOWN;
        List<String> excludedSubsystemIds = new ArrayList<>();
        Map<String, String> exclusionReasons = new java.util.HashMap<>();
        boolean hasInsufficientEvidence = false;
        
        for (SubsystemHealthAssessment sub : subsystemAssessments) {
            Double weight = componentWeights.getOrDefault(sub.getSubsystemId(), 1.0);


            if (sub.getSeverityLevel() == SeverityLevel.UNKNOWN || sub.getHealthScore() == null) {
                excludedSubsystemIds.add(sub.getSubsystemId());
                exclusionReasons.put(sub.getSubsystemId(), sub.getDegradedDataFlags().isEmpty() ? "UNKNOWN_REASON" : sub.getDegradedDataFlags().get(0));
                continue; // Skip unknown subsystems
            }
            if (!sub.isDataQualitySufficient()) hasInsufficientEvidence = true;
            
            if (!Double.isFinite(sub.getHealthScore()) || sub.getHealthScore() < 0 || sub.getHealthScore() > 100)
                throw new IllegalArgumentException("Invalid subsystem score");
            weightedSum += sub.getHealthScore() * weight;
            validWeightSum += weight;
            
            double conf = sub.getConfidence() != null ? sub.getConfidence() : 0.0;
            double unc = sub.getUncertainty() != null ? sub.getUncertainty() : 0.0;
            
            weightedConfidence += conf * weight;
            weightedUncertainty += unc * weight;
            
            // Determine max severity
            if (sub.getSeverityLevel() == SeverityLevel.CRITICAL) overallSeverity = SeverityLevel.CRITICAL;
            else if (sub.getSeverityLevel() == SeverityLevel.DEGRADED && overallSeverity != SeverityLevel.CRITICAL) overallSeverity = SeverityLevel.DEGRADED;
            else if (sub.getSeverityLevel() == SeverityLevel.WATCH && overallSeverity != SeverityLevel.CRITICAL && overallSeverity != SeverityLevel.DEGRADED) overallSeverity = SeverityLevel.WATCH;
            else if (sub.getSeverityLevel() == SeverityLevel.NORMAL && overallSeverity == SeverityLevel.UNKNOWN) overallSeverity = SeverityLevel.NORMAL;
        }

        for (String id : componentWeights.keySet()) if (!seen.contains(id)) {
            excludedSubsystemIds.add(id); exclusionReasons.put(id, "NOT_OBSERVED");
        }
        Double vhi = null;
        VehicleHealthBand band = VehicleHealthBand.UNKNOWN;
        Double overallConf = null;
        Double overallUnc = null;
        Double coverage = totalWeightSum > 0 ? (validWeightSum / totalWeightSum) : 0.0;
        String dataQuality = excludedSubsystemIds.isEmpty() && !hasInsufficientEvidence ? "SUFFICIENT" : "PARTIAL";

        if (validWeightSum > 0) {
            vhi = weightedSum / validWeightSum;
            overallConf = weightedConfidence / totalWeightSum;
            overallUnc = weightedUncertainty / validWeightSum;
            
            if (vhi >= 90.0) band = VehicleHealthBand.EXCELLENT;
            else if (vhi >= 75.0) band = VehicleHealthBand.GOOD;
            else if (vhi >= 50.0) band = VehicleHealthBand.FAIR;
            else if (vhi >= 25.0) band = VehicleHealthBand.POOR;
            else band = VehicleHealthBand.CRITICAL;
        } else {
            dataQuality = "INSUFFICIENT";
        }
        
        return new VehicleHealthAssessment(vehicleId, timestampMs, windowMs, 
                                           "UNKNOWN", "UNAVAILABLE",
                                           java.time.Instant.ofEpochMilli(timestampMs).toString(), windowMs + " ms", null,
                                           vhi, band, overallSeverity, overallConf, overallUnc, 
                                           dataQuality, coverage, excludedSubsystemIds, exclusionReasons,
                                           subsystemAssessments, componentWeights, evidenceReferences, 
                                           calculationVersion, registryVersion, contextVersion, contextVersion);
    }
}
