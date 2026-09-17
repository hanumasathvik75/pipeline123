package com.ignitionai.phase7.app;

import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.CertificateValidator;
import com.ignitionai.phase7.validation.ValidationResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class DealershipAppMVP {

    private LatexGenerator latexGenerator = new LatexGenerator();
    private CertificateValidator validator = new CertificateValidator();
    private AssessmentHistoryView historyView = new AssessmentHistoryView();

    public void loadAndDisplaySnapshot(CertificateSnapshot snapshot) {
        System.out.println("==================================================");
        System.out.println("          PRE-OWNED DEALERSHIP DASHBOARD          ");
        System.out.println("==================================================");
        
        ValidationResult validation = validator.validateSnapshot(snapshot);
        if (!validation.isValid()) {
            System.out.println("[WARNING] Certificate Snapshot Validation Failed:");
            for (String err : validation.getErrors()) {
                System.out.println("  - " + err);
            }
        }
        
        VehicleHealthAssessment ha = snapshot.getHealthAssessment();
        System.out.println("Vehicle ID: " + ha.getVehicleId());
        System.out.println("Assessment Date: " + ha.getAssessmentTimestamp());
        System.out.println("Overall VHI: " + ha.getVehicleHealthIndex());
        System.out.println("Health Band: " + ha.getHealthBand());
        System.out.println("Overall Severity: " + ha.getOverallSeverity());
        System.out.println("Data Quality: " + ha.getDataQualityStatus());
        System.out.println("Overall Confidence: " + ha.getOverallConfidence());
        System.out.println("--------------------------------------------------");
        System.out.println("Subsystem Scores:");
        
        if (ha.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment sub : ha.getSubsystemAssessments()) {
                String evidence = sub.getEvidenceReferences() != null ? String.join(", ", sub.getEvidenceReferences()) : "None";
                System.out.printf("  - %s: %.2f (Severity: %s) [Evidence: %s]\n", 
                                  sub.getSubsystemName(), sub.getScore(), sub.getSeverity(), evidence);
            }
        } else {
            System.out.println("  No subsystem data available.");
        }
        System.out.println("==================================================\n");
    }
    
    public void compareAssessments(CertificateSnapshot older, CertificateSnapshot newer) {
        historyView.displayComparison(older, newer);
    }
    
    public void exportCertificate(CertificateSnapshot snapshot, String outputDirectory) {
        String latex = latexGenerator.generateCertificate(snapshot);
        ValidationResult latexValidation = validator.validateLatex(latex);
        if (!latexValidation.isValid()) {
            System.out.println("[ERROR] Generated LaTeX failed validation:");
            for(String e : latexValidation.getErrors()) System.out.println(e);
            return;
        }
        
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File file = new File(dir, snapshot.getCertificateId() + ".tex");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(latex);
            System.out.println("Certificate successfully exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to write certificate: " + e.getMessage());
        }
    }
}
