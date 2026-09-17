package com.ignitionai.phase7.latex;

import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;

import java.util.stream.Collectors;

public class LatexGenerator {

    public String generateCertificate(CertificateSnapshot snapshot) {
        VehicleHealthAssessment ha = snapshot.getHealthAssessment();
        
        StringBuilder latex = new StringBuilder();
        latex.append("\\documentclass[12pt,a4paper]{article}\n");
        latex.append("\\usepackage[utf8]{inputenc}\n");
        latex.append("\\usepackage{graphicx}\n");
        latex.append("\\usepackage{geometry}\n");
        latex.append("\\usepackage{booktabs}\n");
        latex.append("\\usepackage{xcolor}\n");
        latex.append("\\geometry{margin=1in}\n");
        latex.append("\n\\begin{document}\n\n");
        
        // Header
        latex.append("\\begin{center}\n");
        latex.append("\\Huge{\\textbf{IgnitionAI Vehicle Health Certificate}}\\\\\n");
        latex.append("\\vspace{0.5cm}\n");
        latex.append("\\Large{\\textbf{Certificate ID: }} ").append(escapeLatex(snapshot.getCertificateId())).append("\\\\\n");
        latex.append("\\vspace{0.2cm}\n");
        latex.append("\\normalsize{Generated on: ").append(escapeLatex(ha.getAssessmentTimestamp())).append("}\n");
        latex.append("\\end{center}\n\n");
        
        latex.append("\\vspace{1cm}\n");
        
        // Vehicle Identity Section
        latex.append("\\section*{Vehicle Identity}\n");
        latex.append("\\begin{itemize}\n");
        latex.append("  \\item \\textbf{Vehicle ID:} ").append(escapeLatex(ha.getVehicleId())).append("\n");
        latex.append("  \\item \\textbf{VIN/Identity:} ").append(escapeLatex(ha.getVinOrIdentityStatus())).append("\n");
        latex.append("  \\item \\textbf{Configuration:} ").append(escapeLatex(ha.getVehicleConfiguration())).append("\n");
        String odo = (ha.getOdometerKm() != null) ? String.format("%.1f km", ha.getOdometerKm()) : "UNAVAILABLE";
        latex.append("  \\item \\textbf{Odometer:} ").append(odo).append("\n");
        latex.append("\\end{itemize}\n\n");
        
        // Overall Health Score Section
        latex.append("\\section*{Overall Health Assessment}\n");
        latex.append("\\begin{itemize}\n");
        String vhi = (ha.getVehicleHealthIndex() != null) ? String.format("%.2f", ha.getVehicleHealthIndex()) : "UNAVAILABLE";
        latex.append("  \\item \\textbf{Vehicle Health Index (VHI):} ").append(vhi).append("\n");
        latex.append("  \\item \\textbf{Health Band:} ").append(escapeLatex(ha.getHealthBand().name())).append("\n");
        latex.append("  \\item \\textbf{Overall Severity:} ").append(escapeLatex(ha.getOverallSeverity().name())).append("\n");
        latex.append("\\end{itemize}\n\n");
        
        // Subsystem Score Table
        latex.append("\\section*{Subsystem Health Scores}\n");
        latex.append("\\begin{table}[h!]\n");
        latex.append("\\centering\n");
        latex.append("\\begin{tabular}{l l c l}\n");
        latex.append("\\toprule\n");
        latex.append("\\textbf{Subsystem} & \\textbf{Score} & \\textbf{Severity} & \\textbf{Evidence} \\\\\n");
        latex.append("\\midrule\n");
        
        if (ha.getSubsystemAssessments() != null) {
            for (SubsystemHealthAssessment sub : ha.getSubsystemAssessments()) {
                String subScore = (sub.getScore() != null) ? String.format("%.2f", sub.getScore()) : "UNAVAILABLE";
                String severity = sub.getSeverity().name();
                String evidence = sub.getEvidenceReferences() != null ? 
                    sub.getEvidenceReferences().stream().map(this::escapeLatex).collect(Collectors.joining(", ")) : "None";
                
                latex.append(escapeLatex(sub.getSubsystemName())).append(" & ")
                     .append(subScore).append(" & ")
                     .append(escapeLatex(severity)).append(" & ")
                     .append(evidence).append(" \\\\\n");
            }
        } else {
            latex.append("\\multicolumn{4}{c}{No Subsystem Data Available} \\\\\n");
        }
        
        latex.append("\\bottomrule\n");
        latex.append("\\end{tabular}\n");
        latex.append("\\end{table}\n\n");
        
        // Confidence and Data Quality Section
        latex.append("\\section*{Confidence \\& Data Quality}\n");
        latex.append("\\begin{itemize}\n");
        String conf = (ha.getOverallConfidence() != null) ? String.format("%.2f", ha.getOverallConfidence()) : "UNAVAILABLE";
        latex.append("  \\item \\textbf{Overall Confidence:} ").append(conf).append("\n");
        String uncert = (ha.getOverallUncertainty() != null) ? String.format("%.2f", ha.getOverallUncertainty()) : "UNAVAILABLE";
        latex.append("  \\item \\textbf{Overall Uncertainty:} ").append(uncert).append("\n");
        latex.append("  \\item \\textbf{Data Quality Status:} ").append(escapeLatex(ha.getDataQualityStatus())).append("\n");
        latex.append("\\end{itemize}\n\n");

        // Methodology & Traceability Section
        latex.append("\\section*{Methodology \\& Traceability}\n");
        latex.append("\\begin{itemize}\n");
        latex.append("  \\item \\textbf{Certificate Schema Version:} ").append(escapeLatex(snapshot.getCertificateSchemaVersion())).append("\n");
        latex.append("  \\item \\textbf{Calculation Version:} ").append(escapeLatex(ha.getCalculationVersion())).append("\n");
        latex.append("  \\item \\textbf{Model Versions:} ").append(escapeLatex(ha.getModelVersions())).append("\n");
        latex.append("  \\item \\textbf{Registry Release Version:} ").append(escapeLatex(ha.getRegistryReleaseVersion())).append("\n");
        latex.append("\\end{itemize}\n\n");
        
        latex.append("\\vspace{1cm}\n");
        latex.append("\\small{\\textit{This certificate is automatically generated based purely on structured diagnostic outputs. It makes no unsupported diagnostic claims.}}\n");

        latex.append("\\end{document}\n");
        
        return latex.toString();
    }
    
    private String escapeLatex(String text) {
        if (text == null) return "";
        return text.replace("&", "\\&")
                   .replace("%", "\\%")
                   .replace("$", "\\$")
                   .replace("#", "\\#")
                   .replace("_", "\\_")
                   .replace("{", "\\{")
                   .replace("}", "\\}");
    }
}
