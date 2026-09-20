package com.ignitionai.phase7.latex;

import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.virtualsensors.index.FaultCandidate;
import com.ignitionai.virtualsensors.index.FaultCandidateRanker;

import java.util.List;
import java.util.Locale;

/**
 * A second, separate report from the Vehicle Health Certificate: Root Cause Analysis & Repair
 * Plan. Deliberately not merged into the certificate -- the certificate is the buyer/dealer-facing
 * evidence document, this is the technician-facing action list. Both are rendered from the exact
 * same {@link CertificateSnapshot}, so they can never disagree about what was found.
 *
 * Every step here is generated deterministically from data already computed by
 * {@link FaultCandidateRanker}'s probabilistic fault graph (posterior probability, corroborating
 * evidence, a static cause -> what-to-check/tools lookup) -- nothing here is free-text generated
 * by a language model. That matters: the project's own architecture spec explicitly warns that a
 * repair-plan layer "cannot change the diagnosis, fabricate evidence, or introduce an unapproved
 * action," and a full Value-of-Information test-selection loop with live technician feedback is
 * out of scope for a static report. This is the honest subset of that idea that a static document
 * can actually deliver: an evidence-ordered checklist, not an interactive diagnostic session.
 */
public class RepairPlanGenerator {

    public String generateReport(CertificateSnapshot snapshot) {
        VehicleHealthAssessment ha = snapshot.getHealthAssessment();
        List<FaultCandidate> candidates = snapshot.getRankedFaultCandidates();
        StringBuilder tex = new StringBuilder();

        tex.append("\\documentclass[11pt,a4paper]{article}\n")
           .append("\\usepackage[utf8]{inputenc}\n")
           .append("\\usepackage{graphicx}\n")
           .append("\\usepackage{geometry}\n")
           .append("\\usepackage{booktabs}\n")
           .append("\\usepackage{tabularx}\n")
           .append("\\usepackage{longtable}\n")
           .append("\\usepackage{colortbl}\n")
           .append("\\usepackage{xcolor}\n")
           .append("\\usepackage{enumitem}\n")
           .append("\\usepackage{mathpazo}\n")
           .append("\\usepackage{eso-pic}\n")
           .append("\\usepackage{tikz}\n")
           .append("\\usetikzlibrary{calc}\n")
           .append("\\renewcommand{\\labelitemi}{\\textbullet}\n")
           .append("\\geometry{margin=2.1cm}\n")
           .append("\\definecolor{iaiNavy}{HTML}{1F2937}\n")
           .append("\\definecolor{iaiSlate}{HTML}{475569}\n")
           .append("\\definecolor{iaiGood}{HTML}{15803D}\n")
           .append("\\definecolor{iaiWarn}{HTML}{B45309}\n")
           .append("\\definecolor{iaiBad}{HTML}{B91C1C}\n")
           .append("\\definecolor{iaiRule}{HTML}{D1D5DB}\n")
           .append("\\definecolor{iaiPanel}{HTML}{F3F4F6}\n")
           .append("\\AddToShipoutPictureBG{\\begin{tikzpicture}[remember picture,overlay]")
           .append("\\draw[line width=0.9pt,color=iaiNavy] ($(current page.north west)+(0.55cm,-0.55cm)$) rectangle ($(current page.south east)+(-0.55cm,0.55cm)$);")
           .append("\\end{tikzpicture}}\n")
           .append("\\pagestyle{empty}\n")
           .append("\\setlength{\\parindent}{0pt}\n")
           .append("\\begin{document}\n\n");

        header(tex, snapshot, ha);
        if (candidates.isEmpty()) {
            noFindingsSection(tex);
        } else {
            rootCauseAnalysisSection(tex, candidates);
            repairPlanSection(tex, candidates);
        }
        disclaimerAndFooter(tex, snapshot);

        tex.append("\\end{document}\n");
        return tex.toString();
    }

    private void header(StringBuilder tex, CertificateSnapshot snapshot, VehicleHealthAssessment ha) {
        tex.append("\\noindent{\\footnotesize\\color{iaiSlate}\\textbf{TECHNICIAN WORKING DOCUMENT}}\\\\[4pt]\n")
           .append("\\noindent\n")
           .append("\\begin{minipage}[c]{0.55\\textwidth}\n")
           .append("\\IfFileExists{ignitionai_logo.png}{\\includegraphics[height=1.15cm]{ignitionai_logo.png}}{\\textbf{\\huge\\color{iaiNavy}ignitionAI}}\n")
           .append("\\end{minipage}\\hfill\n")
           .append("\\begin{minipage}[c]{0.4\\textwidth}\\raggedleft\n")
           .append("{\\bfseries\\Large\\rmfamily\\color{iaiNavy}Root Cause Analysis \\& Repair Plan}\\\\[3pt]\n")
           .append("{\\small\\color{iaiSlate}Companion to Vehicle Health Certificate ").append(escapeLatex(snapshot.getCertificateId())).append("}\n")
           .append("\\end{minipage}\n\n")
           .append("\\vspace{6pt}{\\color{iaiNavy}\\hrule height 1.6pt}\\vspace{1pt}{\\color{iaiRule}\\hrule height 0.6pt}\\vspace{8pt}\n\n")
           .append("\\noindent{\\small Vehicle: ").append(escapeLatex(ha.getVehicleId()))
           .append(" \\quad\\textbar\\quad Session: ").append(escapeLatex(snapshot.getSessionId()))
           .append(" \\quad\\textbar\\quad Generated: ").append(escapeLatex(ha.getAssessmentTimestamp())).append("}\\\\[10pt]\n\n")
           .append("\\noindent{\\footnotesize\\bfseries\\color{iaiWarn}Not a certified repair procedure.} {\\footnotesize\\color{iaiSlate}This is a heuristic, evidence-ordered checklist generated from this session's Diagnostic Index and DTC findings. It does not replace manufacturer service information or technician judgement, and no step has been verified by physical inspection.}\\\\[10pt]\n\n");
    }

    private void noFindingsSection(StringBuilder tex) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large No Root Cause Analysis Available}\\\\[6pt]\n")
           .append("{\\small\\color{iaiSlate}No Diagnostic Index finding or DTC from this session corroborated a candidate root cause strongly enough to report one. This means either the vehicle showed no flagged findings, or the findings present don't yet map to a named component in the fault graph. No repair plan is generated when there is nothing to base one on.}\\\\[10pt]\n\n");
    }

    private void rootCauseAnalysisSection(StringBuilder tex, List<FaultCandidate> candidates) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Root Cause Analysis}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Every candidate below is a posterior probability combining all corroborating evidence this session found -- not an independent guess per finding. See the Vehicle Health Certificate for the underlying Diagnostic Index and DTC evidence this is built from.}\\\\[6pt]\n")
           .append("\\begin{longtable}{@{}p{4cm}p{1.6cm}p{\\dimexpr\\textwidth-11.6cm\\relax}p{6cm}@{}}\n\\toprule\n")
           .append("\\textbf{Candidate cause} & \\textbf{Probability} & \\textbf{Supporting evidence} & \\textbf{Why this evidence implicates it} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Candidate cause} & \\textbf{Probability} & \\textbf{Supporting evidence} & \\textbf{Why this evidence implicates it} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (FaultCandidate c : candidates) {
            tex.append(escapeLatex(c.getName())).append(" & ").append(String.format(Locale.ROOT, "%.0f\\%%", c.getLikelihoodPct()))
               .append(" & {\\footnotesize ").append(escapeLatex(c.getCorroboration())).append("} & {\\footnotesize ")
               .append(escapeLatex(c.getRootCauseHypothesis())).append("} \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{10pt}\n\n");
    }

    private void repairPlanSection(StringBuilder tex, List<FaultCandidate> candidates) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Stepwise Repair Plan}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Ordered by probability -- check the most likely cause first. Stop and re-scan once a step is confirmed; a fix should make the corresponding Diagnostic Index finding and any related DTC clear on the next inspection.}\\\\[8pt]\n");
        int step = 1;
        for (FaultCandidate c : candidates) {
            tex.append("{\\bfseries\\color{iaiNavy}Step ").append(step++).append(". ").append(escapeLatex(c.getName()))
               .append(" \\small\\color{iaiSlate}(").append(String.format(Locale.ROOT, "%.0f\\%%", c.getLikelihoodPct())).append(" probability)}\\\\[2pt]\n")
               .append("\\begin{tabularx}{\\textwidth}{@{}p{2.6cm}X@{}}\n")
               .append("{\\small\\bfseries What to check} & {\\small ").append(escapeLatex(c.getRootCauseHypothesis())).append("} \\\\\n")
               .append("{\\small\\bfseries Tools needed} & {\\small ").append(escapeLatex(FaultCandidateRanker.toolsFor(c.getName()))).append("} \\\\\n")
               .append("{\\small\\bfseries Triggered by} & {\\small ").append(escapeLatex(c.getCorroboration())).append("} \\\\\n")
               .append("{\\small\\bfseries If confirmed} & {\\small Repair or replace the affected component, then re-scan to confirm the finding clears.} \\\\\n")
               .append("{\\small\\bfseries If ruled out} & {\\small Proceed to the next step below.} \\\\\n")
               .append("\\end{tabularx}\\\\[10pt]\n\n");
        }
    }

    private void disclaimerAndFooter(StringBuilder tex, CertificateSnapshot snapshot) {
        tex.append("{\\footnotesize\\itshape\\color{iaiSlate}Method boundary: candidate causes and their order come from a heuristic evidence-combination rule (see the Vehicle Health Certificate's methodology section), not a fitted model calibrated against real repair outcomes. Confirming or ruling out a step is a physical-inspection decision for a qualified technician; this document orders the search, it does not perform or replace it.}\n\n\\vspace{10pt}\n\n")
           .append("{\\color{iaiRule}\\hrule}\\vspace{4pt}\n")
           .append("{\\footnotesize\\color{iaiSlate}IgnitionAI \\textbar\\ www.ignitionai.in \\textbar\\ Udyam Reg. No. UDYAM-TN-02-0444115 \\textbar\\ Certificate ")
           .append(escapeLatex(snapshot.getCertificateId())).append("\\\\\n")
           .append("A sole proprietorship registered under Udyam, Tamil Nadu, India}\n\n");
    }

    private String escapeLatex(String text) {
        if (text == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\': escaped.append("\\textbackslash{}"); break;
                case '&': case '%': case '$': case '#': case '{': case '}': escaped.append('\\').append(c); break;
                case '_': escaped.append("\\_\\allowbreak{}"); break;
                case '^': escaped.append("\\textasciicircum{}"); break;
                case '~': escaped.append("\\textasciitilde{}"); break;
                case '/': case '-': escaped.append(c).append("\\allowbreak{}"); break;
                default: escaped.append(Character.isISOControl(c) ? ' ' : c);
            }
        }
        return escaped.toString();
    }
}
