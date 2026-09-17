package com.ignitionai.phase7.app;

import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase6.SubsystemHealthAssessment;

import java.util.Map;
import java.util.HashMap;

public class AssessmentHistoryView {

    public void displayComparison(CertificateSnapshot older, CertificateSnapshot newer) {
        System.out.println("==================================================");
        System.out.println("          ASSESSMENT HISTORY & COMPARISON         ");
        System.out.println("==================================================");
        VehicleHealthAssessment o = older.getHealthAssessment();
        VehicleHealthAssessment n = newer.getHealthAssessment();
        
        System.out.println("Vehicle ID: " + n.getVehicleId());
        System.out.println("Older Assessment Date: " + o.getAssessmentTimestamp());
        System.out.println("Newer Assessment Date: " + n.getAssessmentTimestamp());
        System.out.println("--------------------------------------------------");
        
        Double oVhi = o.getVehicleHealthIndex();
        Double nVhi = n.getVehicleHealthIndex();
        String vhiChange = formatChange(oVhi, nVhi);
        
        System.out.printf("VHI Movement: %.2f -> %.2f %s\n", 
                          oVhi != null ? oVhi : 0.0, 
                          nVhi != null ? nVhi : 0.0, 
                          vhiChange);
        
        System.out.println("Health Band: " + o.getHealthBand() + " -> " + n.getHealthBand());
        System.out.println("--------------------------------------------------");
        
        System.out.println("Subsystem Score Movement:");
        Map<String, Double> olderSubsystems = new HashMap<>();
        if (o.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment sub : o.getSubsystemAssessments()) {
                olderSubsystems.put(sub.getSubsystemName(), sub.getScore());
            }
        }
        
        if (n.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment nSub : n.getSubsystemAssessments()) {
                Double oScore = olderSubsystems.get(nSub.getSubsystemName());
                Double nScore = nSub.getScore();
                System.out.printf("  - %s: %.2f -> %.2f %s\n", 
                                  nSub.getSubsystemName(), 
                                  oScore != null ? oScore : 0.0, 
                                  nScore != null ? nScore : 0.0, 
                                  formatChange(oScore, nScore));
            }
        }
        System.out.println("==================================================\n");
    }
    
    private String formatChange(Double older, Double newer) {
        if (older == null || newer == null) return "(N/A)";
        double diff = newer - older;
        if (diff > 0) return String.format("(+%.2f)", diff);
        if (diff < 0) return String.format("(%.2f)", diff);
        return "(No change)";
    }
}
