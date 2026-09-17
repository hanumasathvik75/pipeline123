package com.ignitionai.phase7.validation;

import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;

public class CertificateValidator {

    public ValidationResult validateSnapshot(CertificateSnapshot snapshot) {
        ValidationResult result = new ValidationResult();
        
        if (snapshot == null) {
            result.addError("Snapshot is null.");
            return result;
        }

        if (snapshot.getCertificateId() == null || snapshot.getCertificateId().trim().isEmpty()) {
            result.addError("Missing certificate ID.");
        }
        if (snapshot.getCertificateSchemaVersion() == null || snapshot.getCertificateSchemaVersion().trim().isEmpty()) {
            result.addError("Missing certificate schema version.");
        }

        VehicleHealthAssessment ha = snapshot.getHealthAssessment();
        if (ha == null) {
            result.addError("Missing vehicle health assessment data.");
            return result;
        }

        if (ha.getVehicleId() == null || ha.getVehicleId().trim().isEmpty()) {
            result.addError("Missing vehicle ID.");
        }
        
        if (ha.getAssessmentTimestamp() == null || ha.getAssessmentTimestamp().trim().isEmpty()) {
            result.addError("Missing assessment timestamp.");
        }
        
        if (ha.getCalculationVersion() == null || ha.getCalculationVersion().trim().isEmpty()) {
            result.addError("Missing calculation version.");
        }

        Double vhi = ha.getVehicleHealthIndex();
        if (vhi != null && (vhi < 0.0 || vhi > 100.0)) {
            result.addError("Invalid overall VHI score range. Must be 0.0 to 100.0.");
        }

        if (ha.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment sub : ha.getSubsystemAssessments()) {
                Double score = sub.getScore();
                if (score != null && (score < 0.0 || score > 100.0)) {
                    result.addError("Invalid subsystem score range for " + sub.getSubsystemName() + ".");
                }
                
                if (sub.getEvidenceReferences() == null || sub.getEvidenceReferences().isEmpty()) {
                    // Check if missing subsystem evidence is an error. 
                    // Let's add a warning or an error depending on strictness. The prompt says "missing subsystem evidence".
                    result.addError("Missing subsystem evidence for " + sub.getSubsystemName() + ".");
                }
            }
        }
        
        return result;
    }

    public ValidationResult validateLatex(String latex) {
        ValidationResult result = new ValidationResult();
        if (latex == null || latex.trim().isEmpty()) {
            result.addError("LaTeX document is empty.");
            return result;
        }
        
        if (!latex.contains("\\begin{document}") || !latex.contains("\\end{document}")) {
            result.addError("Malformed LaTeX: missing document environment.");
        }
        
        // Unsupported claims check (naive)
        String lowerLatex = latex.toLowerCase();
        if (lowerLatex.contains("we recommend") || lowerLatex.contains("requires repair") || lowerLatex.contains("replace")) {
            result.addError("Unsupported claims detected in LaTeX output.");
        }
        
        return result;
    }
}
