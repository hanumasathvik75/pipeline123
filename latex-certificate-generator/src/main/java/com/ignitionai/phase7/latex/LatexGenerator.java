package com.ignitionai.phase7.latex;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.virtualsensors.index.DiagnosticIndexNarrator;
import com.ignitionai.virtualsensors.index.DiagnosticIndexResult;
import com.ignitionai.virtualsensors.index.FaultCandidate;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a CertificateSnapshot as a formatted Vehicle Health Certificate.
 *
 * Layout intent: a headline VHI block and a "Monitored Indicators" table give the familiar
 * inspection-report shape a dealership already expects (Cars24/Spinny-style). The "IgnitionAI
 * Diagnostic Index" and "Ranked Fault Candidates" sections are the differentiated part — findings
 * derived from relating multiple PIDs over time (fuel-trim persistence, throttle-to-RPM response
 * lag, coolant warm-up/steady-state behaviour, charging ripple) that a single-PID OBD read cannot
 * produce. Every number carries its own confidence/coverage label; nothing here is a calibrated
 * failure probability.
 */
public class LatexGenerator {

    public String generateCertificate(CertificateSnapshot snapshot) {
        VehicleHealthAssessment ha = snapshot.getHealthAssessment();
        StringBuilder tex = new StringBuilder();

        tex.append("\\documentclass[11pt,a4paper]{article}\n")
           .append("\\usepackage[utf8]{inputenc}\n")
           .append("\\usepackage{graphicx}\n")
           .append("\\usepackage{geometry}\n")
           .append("\\usepackage{booktabs}\n")
           .append("\\usepackage{tabularx}\n")
           .append("\\usepackage{colortbl}\n")
           .append("\\usepackage{xcolor}\n")
           .append("\\usepackage{enumitem}\n")
           .append("\\usepackage{mathpazo}\n")
           .append("\\usepackage{longtable}\n")
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
        atAGlanceSection(tex, snapshot, ha);
        tex.append("\\newpage\n\n");
        diagnosticIndexSection(tex, snapshot);
        faultCandidateSection(tex, snapshot);
        dtcSection(tex, snapshot);
        episodeSection(tex, snapshot);
        degradationSection(tex, snapshot);
        confidenceSection(tex, ha);
        methodologySection(tex, snapshot, ha);
        footer(tex);

        tex.append("\\end{document}\n");
        return tex.toString();
    }

    private void header(StringBuilder tex, CertificateSnapshot snapshot, VehicleHealthAssessment ha) {
        tex.append("\\noindent{\\footnotesize\\color{iaiSlate}\\textbf{PRE-OWNED VEHICLE INSPECTION}}\\\\[4pt]\n")
           .append("\\noindent\n")
           .append("\\begin{minipage}[c]{0.55\\textwidth}\n")
           .append("\\IfFileExists{ignitionai_logo.png}{\\includegraphics[height=1.15cm]{ignitionai_logo.png}}{\\textbf{\\huge\\color{iaiNavy}ignitionAI}}\n")
           .append("\\end{minipage}\\hfill\n")
           .append("\\begin{minipage}[c]{0.4\\textwidth}\\raggedleft\n")
           .append("{\\bfseries\\Large\\rmfamily\\color{iaiNavy}Vehicle Health Certificate}\\\\[3pt]\n")
           .append("{\\small\\color{iaiSlate}Evidence-first pre-owned inspection}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}").append(escapeLatex(ha.getAssessmentTimestamp())).append("}\n")
           .append("\\end{minipage}\n\n")
           .append("\\vspace{6pt}{\\color{iaiNavy}\\hrule height 1.6pt}\\vspace{1pt}{\\color{iaiRule}\\hrule height 0.6pt}\\vspace{8pt}\n\n")
           .append("\\noindent{\\footnotesize\\bfseries\\color{iaiWarn}Preliminary report.} {\\footnotesize\\color{iaiSlate}Source: ")
           .append(escapeLatex(snapshot.getSourceType())).append(". Generic models are uncalibrated; unknown systems are unassessed; simulation is not a vehicle inspection.}\\\\[10pt]\n\n");
    }

    /** Page 1: everything a buyer or dealership principal actually reads -- the headline finding,
     * a plain-English verdict, vehicle/capture facts and a verification block. The full technical
     * breakdown (Diagnostic Index table, Monitored Indicators, evidence appendix) starts on page 2;
     * this page exists so the certificate has the same "one number, one verdict, then the detail"
     * shape as the reference reports it's meant to sit alongside, instead of leading with a table. */
    private void atAGlanceSection(StringBuilder tex, CertificateSnapshot snapshot, VehicleHealthAssessment ha) {
        List<DiagnosticIndexResult> idx = snapshot.getDiagnosticIndex();
        DiagnosticIndexResult overall = idx.stream().filter(r -> "overall_condition".equals(r.getIndexId())).findFirst().orElse(null);

        long supported, flagged;
        String headline, headlineColor;
        if (overall != null && overall.getHealthPct() != null) {
            // Prefer the confidence-weighted blend of the 7 mechanical-condition indices as the
            // single headline number -- this is that plugin's whole purpose, so use it directly
            // instead of a flat average across every index (which would double-count its inputs).
            supported = parseRatioPart(overall.getEvidence().get("components_supported"), 1);
            flagged = Long.parseLong(overall.getEvidence().getOrDefault("components_flagged", "0"));
            headline = String.format(Locale.ROOT, "%.0f", overall.getHealthPct());
            headlineColor = flagged > 0 ? "iaiWarn" : "iaiGood";
        } else {
            supported = idx.stream().filter(r -> r.getHealthPct() != null && !"overall_condition".equals(r.getIndexId())).count();
            flagged = idx.stream().filter(r -> r.getState() == DiagnosticIndexResult.State.FLAGGED && !"overall_condition".equals(r.getIndexId())).count();
            double healthSum = 0; int healthCount = 0;
            for (DiagnosticIndexResult r : idx) if (r.getHealthPct() != null && !"overall_condition".equals(r.getIndexId())) { healthSum += r.getHealthPct(); healthCount++; }
            headline = healthCount > 0 ? String.format(Locale.ROOT, "%.0f", healthSum / healthCount) : "UNAVAILABLE";
            headlineColor = healthCount == 0 ? "iaiSlate" : flagged > 0 ? "iaiWarn" : "iaiGood";
        }
        String durationText = snapshot.getCaptureDurationMs() != null
            ? String.format(Locale.ROOT, "%.1f min", snapshot.getCaptureDurationMs() / 60000.0) : "UNAVAILABLE";
        String records = snapshot.getRecordCount() != null ? String.valueOf(snapshot.getRecordCount()) : "UNAVAILABLE";
        String vhi = ha.getVehicleHealthIndex() != null ? String.format(Locale.ROOT, "%.2f", ha.getVehicleHealthIndex()) : "UNAVAILABLE";
        String band = ha.getHealthBand() != null ? ha.getHealthBand().name() : "UNKNOWN";

        tex.append("\\vspace{6pt}\n\\begin{center}\n")
           .append("{\\rmfamily\\fontsize{72}{72}\\selectfont\\bfseries\\color{").append(headlineColor).append("}").append(headline).append("}")
           .append("{\\Large /100}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}IGNITIONAI DIAGNOSTIC INDEX \\textbar\\ ").append(flagged).append(" of ").append(supported).append(" assessed systems flagged}\n")
           .append("\\end{center}\n\\vspace{8pt}\n\n")
           .append("\\noindent{\\large\\color{iaiNavy}").append(escapeLatex(verdictParagraph(snapshot))).append("}\\\\[14pt]\n\n")
           .append("\\colorbox{iaiPanel}{\\begin{minipage}{\\dimexpr\\textwidth-16pt}\\vspace{6pt}\n")
           .append("\\begin{tabularx}{\\textwidth}{@{}X X@{}}\n")
           .append("{\\small\\color{iaiSlate}VEHICLE} & {\\small\\color{iaiSlate}CAPTURE} \\\\\n")
           .append("{\\small ").append(escapeLatex(ha.getVehicleId())).append("}\\newline{\\small ").append(escapeLatex(ha.getVehicleConfiguration() == null ? "Configuration unresolved" : ha.getVehicleConfiguration())).append("} & ")
           .append("{\\small ").append(durationText).append(" \\textbar\\ ").append(records).append(" records}\\newline{\\small Source: ").append(escapeLatex(snapshot.getSourceType())).append("} \\\\\n")
           .append("\\end{tabularx}\\vspace{6pt}\\end{minipage}}\n\n\\vspace{10pt}\n\n")
           .append("\\noindent\\begin{tabularx}{\\textwidth}{@{}X@{}}\n")
           .append("\\toprule\n")
           .append("{\\small\\bfseries\\color{iaiNavy}Verification} \\\\\n")
           .append("\\midrule\n")
           .append("{\\small\\ttfamily Certificate ID: ").append(escapeLatex(snapshot.getCertificateId())).append("}\\\\\n")
           .append("{\\small\\ttfamily Evidence hash: ").append(escapeLatex(shorten(snapshot.getEvidenceDigest(), 32))).append("}\\\\\n")
           .append("{\\footnotesize\\color{iaiSlate}This certificate is reproducible from its evidence snapshot -- changing any input value changes this hash. No live verification service is attached to this ID yet.}\\\\\n")
           .append("\\bottomrule\n")
           .append("\\end{tabularx}\n\n\\vspace{10pt}\n\n")
           .append("{\\footnotesize\\color{iaiSlate}Legacy baseline monitoring (Vehicle Health Index, provisional): ").append(vhi)
           .append("/100, Band: ").append(escapeLatex(band)).append(", Overall severity: ").append(escapeLatex(ha.getOverallSeverity() == null ? "UNKNOWN" : ha.getOverallSeverity().name()))
           .append(" \\textemdash{} superseded by the Diagnostic Index above; per-signal detail retained internally, not printed here.}\n\n");
    }

    private static long parseRatioPart(String ratio, int part) {
        if (ratio == null) return 0;
        String[] parts = ratio.split("/");
        try { return Long.parseLong(parts[part].trim()); } catch (Exception e) { return 0; }
    }

    /** Plain-English verdict combining the Diagnostic Index outcome and DTC status -- the one
     * paragraph a non-technical reader needs before deciding whether to read further. Uses the
     * same overall_condition-preferring counts as the headline so the two never disagree. */
    private String verdictParagraph(CertificateSnapshot snapshot) {
        List<DiagnosticIndexResult> idx = snapshot.getDiagnosticIndex();
        DiagnosticIndexResult overall = idx.stream().filter(r -> "overall_condition".equals(r.getIndexId())).findFirst().orElse(null);
        long supported, flagged;
        if (overall != null && overall.getHealthPct() != null) {
            supported = parseRatioPart(overall.getEvidence().get("components_supported"), 1);
            flagged = Long.parseLong(overall.getEvidence().getOrDefault("components_flagged", "0"));
        } else {
            supported = idx.stream().filter(r -> r.getHealthPct() != null && !"overall_condition".equals(r.getIndexId())).count();
            flagged = idx.stream().filter(r -> r.getState() == DiagnosticIndexResult.State.FLAGGED && !"overall_condition".equals(r.getIndexId())).count();
        }
        int dtcCount = snapshot.getDtcCodes().size();
        if (flagged == 0 && dtcCount == 0) {
            return "This inspection did not flag any of the " + supported + " assessed systems, and no diagnostic trouble codes were present at the time of the scan. See page 2 for the full breakdown and its supporting evidence.";
        }
        StringBuilder sb = new StringBuilder("This inspection flagged " + flagged + " of " + supported + " assessed systems");
        if (dtcCount > 0) sb.append(" and found " + dtcCount + " stored diagnostic trouble code(s)");
        sb.append(". See page 2 for what was found, why it matters, and the evidence behind each finding.");
        return sb.toString();
    }

    private void diagnosticIndexSection(StringBuilder tex, CertificateSnapshot snapshot) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large IgnitionAI Diagnostic Index}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Findings from relating multiple signals over the full drive \\textemdash{} not a single-PID read.}\\\\[6pt]\n");
        List<DiagnosticIndexResult> results = snapshot.getDiagnosticIndex();
        if (results.isEmpty()) {
            tex.append("{\\small\\color{iaiSlate}No diagnostic-index plugins were available for this session.}\\\\[10pt]\n\n");
            return;
        }
        String cols = "@{}p{2.3cm}p{1.5cm}p{1.8cm}p{1.7cm}p{\\dimexpr\\textwidth-7.3cm\\relax}@{}";
        tex.append("\\begin{longtable}{").append(cols).append("}\n\\toprule\n")
           .append("\\textbf{Index} & \\textbf{Health} & \\textbf{Confidence} & \\textbf{State} & \\textbf{What this means} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Index} & \\textbf{Health} & \\textbf{Confidence} & \\textbf{State} & \\textbf{What this means} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (DiagnosticIndexResult r : results) {
            String health = r.getHealthPct() != null ? String.format(Locale.ROOT, "%.1f\\%%", r.getHealthPct()) : "UNAVAILABLE";
            String conf = r.getConfidencePct() != null ? String.format(Locale.ROOT, "%.0f\\%%", r.getConfidencePct()) : "UNAVAILABLE";
            String state = stateLabel(r.getState());
            String sentence = escapeLatex(DiagnosticIndexNarrator.plainEnglishFinding(r));
            String trace = r.getState() == DiagnosticIndexResult.State.NORMAL || r.getState() == DiagnosticIndexResult.State.FLAGGED
                ? escapeLatex(String.join("; ", r.getContributingPids())) + ": " + escapeLatex(shortEvidence(r.getEvidence()))
                : escapeLatex(r.getReason() == null ? "unavailable" : r.getReason());
            tex.append(escapeLatex(r.getDisplayName())).append(" & ").append(health).append(" & ").append(conf).append(" & ")
               .append(state).append(" & ").append(sentence).append("\\newline{\\footnotesize\\color{iaiSlate} ").append(trace).append("} \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{10pt}\n\n");
    }

    private void faultCandidateSection(StringBuilder tex, CertificateSnapshot snapshot) {
        List<FaultCandidate> candidates = snapshot.getRankedFaultCandidates();
        if (candidates.isEmpty()) return;
        tex.append("{\\bfseries\\color{iaiNavy}\\large Ranked Fault Candidates}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}When a finding above points toward a likely part or component, it's collected here so a technician checks one probable cause first instead of chasing each flagged reading separately. Likelihood is a heuristic ranking, not a calibrated failure probability -- technician confirmation required.}\\\\[6pt]\n")
           .append("\\begin{longtable}{@{}p{3.4cm}p{1.7cm}p{5.4cm}p{\\dimexpr\\textwidth-10.5cm\\relax}@{}}\n\\toprule\n")
           .append("\\textbf{Candidate} & \\textbf{Likelihood} & \\textbf{Likely root cause} & \\textbf{Corroboration} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Candidate} & \\textbf{Likelihood} & \\textbf{Likely root cause} & \\textbf{Corroboration} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (FaultCandidate c : candidates) {
            tex.append(escapeLatex(c.getName())).append(" & ").append(String.format(Locale.ROOT, "%.0f\\%%", c.getLikelihoodPct()))
               .append(" & {\\footnotesize ").append(escapeLatex(c.getRootCauseHypothesis())).append("} & {\\footnotesize ")
               .append(escapeLatex(c.getCorroboration())).append("} \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{10pt}\n\n");
    }

    private void dtcSection(StringBuilder tex, CertificateSnapshot snapshot) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Diagnostic Trouble Codes}\\\\[4pt]\n");
        if (snapshot.getDtcCodes().isEmpty()) {
            tex.append("{\\small\\color{iaiSlate}No active DTCs in the latest complete scan.}\\\\[10pt]\n\n");
        } else {
            tex.append("{\\small ").append(escapeLatex(String.join(", ", snapshot.getDtcCodes()))).append("}\\\\[10pt]\n\n");
        }
    }

    private void episodeSection(StringBuilder tex, CertificateSnapshot snapshot) {
        List<AnomalyEpisode> episodes = snapshot.getAnomalyEpisodes();
        if (episodes.isEmpty()) return;
        tex.append("{\\bfseries\\color{iaiNavy}\\large Anomaly Episodes}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Sustained deviations detected during this capture, with their supporting evidence count.}\\\\[6pt]\n")
           .append("\\begin{longtable}{@{}p{\\dimexpr\\textwidth-7cm\\relax}p{2cm}p{2cm}p{3cm}@{}}\n\\toprule\n")
           .append("\\textbf{Signal} & \\textbf{Duration} & \\textbf{Severity} & \\textbf{Evidence points} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Signal} & \\textbf{Duration} & \\textbf{Severity} & \\textbf{Evidence points} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (AnomalyEpisode ep : episodes) {
            String duration = ep.getDurationMs() != null ? String.format(Locale.ROOT, "%.1f s", ep.getDurationMs() / 1000.0) : "UNAVAILABLE";
            String severity = ep.getSeverity() != null ? String.format(Locale.ROOT, "%.2f", ep.getSeverity()) : "UNAVAILABLE";
            int refs = ep.getEvidenceReferences() != null ? ep.getEvidenceReferences().size() : 0;
            tex.append(escapeLatex(ep.getSubsystemId())).append(" & ").append(duration).append(" & ").append(severity)
               .append(" & ").append(refs).append(" \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{10pt}\n\n");
    }

    private void degradationSection(StringBuilder tex, CertificateSnapshot snapshot) {
        List<DegradationOutput> outputs = snapshot.getDegradationOutputs();
        if (outputs.isEmpty()) return;
        tex.append("{\\bfseries\\color{iaiNavy}\\large Degradation \\& Predictive Risk}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Trend and risk outputs are simulation/preliminary until validated against real-fleet outcomes.}\\\\[6pt]\n")
           .append("\\begin{longtable}{@{}p{\\dimexpr\\textwidth-9cm\\relax}p{2.2cm}p{2.2cm}p{1.8cm}p{2.8cm}@{}}\n\\toprule\n")
           .append("\\textbf{Indicator} & \\textbf{State} & \\textbf{Trend} & \\textbf{Risk} & \\textbf{Data} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Indicator} & \\textbf{State} & \\textbf{Trend} & \\textbf{Risk} & \\textbf{Data} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (DegradationOutput d : outputs) {
            String risk = d.getRiskStatus() == DegradationOutput.RiskStatus.AVAILABLE && d.getEventRiskEstimate() != null
                ? String.format(Locale.ROOT, "%.0f\\%%", d.getEventRiskEstimate() * 100) : escapeLatex(String.valueOf(d.getRiskStatus()));
            tex.append(escapeLatex(d.getSubsystemId())).append(" & ").append(escapeLatex(String.valueOf(d.getDegradationState())))
               .append(" & ").append(escapeLatex(String.valueOf(d.getTrendDirection()))).append(" & ").append(risk)
               .append(" & ").append(escapeLatex(String.valueOf(d.getDataSufficiencyStatus()))).append(" \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{10pt}\n\n");
    }

    private void confidenceSection(StringBuilder tex, VehicleHealthAssessment ha) {
        String conf = ha.getOverallConfidence() != null ? String.format(Locale.ROOT, "%.0f\\%%", ha.getOverallConfidence() * 100) : "UNAVAILABLE";
        String cov = ha.getSubsystemCoverage() != null ? String.format(Locale.ROOT, "%.0f\\%%", ha.getSubsystemCoverage() * 100) : "UNAVAILABLE";
        tex.append("{\\bfseries\\color{iaiNavy}\\large Data Sufficiency \\& Coverage}\\\\[2pt]\n")
           .append("{\\footnotesize\\color{iaiSlate}How much of this session's own data supports the legacy per-signal monitoring behind this certificate -- not how likely the diagnosis is to be correct.}\\\\[6pt]\n")
           .append("{\\small Data sufficiency: ").append(conf).append(" \\quad\\textbar\\quad Subsystem coverage: ").append(cov)
           .append(" \\quad\\textbar\\quad Data quality: ").append(escapeLatex(ha.getDataQualityStatus())).append("}\\\\[10pt]\n\n");
    }

    private void methodologySection(StringBuilder tex, CertificateSnapshot snapshot, VehicleHealthAssessment ha) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Methodology \\& Traceability}\\\\[4pt]\n")
           .append("{\\footnotesize\\color{iaiSlate}\n")
           .append("Certificate ID: ").append(escapeLatex(snapshot.getCertificateId())).append(" (schema ").append(escapeLatex(snapshot.getCertificateSchemaVersion())).append(")\\\\\n")
           .append("Session: ").append(escapeLatex(snapshot.getSessionId())).append(" \\quad Evidence digest: ").append(escapeLatex(shorten(snapshot.getEvidenceDigest(), 20))).append("\\\\\n")
           .append("Scoring version: ").append(escapeLatex(ha.getCalculationVersion())).append(" \\quad Model versions: ").append(escapeLatex(ha.getModelVersions())).append("\n")
           .append(legacyEvidenceLine(ha))
           .append("}\\\\[10pt]\n\n")
           .append("{\\footnotesize\\itshape\\color{iaiSlate}Method boundary: telemetry was processed locally by deterministic statistical algorithms; the Diagnostic Index and fault-candidate ranking are heuristic, not calibrated probabilities of failure. No repair recommendation is generated by this report. This is decision support for a qualified technician, not a substitute for physical inspection.}\n\n\\vspace{10pt}\n\n");
    }

    /** Compact traceability line for the legacy per-signal evidence that used to have its own table
     * ("Monitored Indicators") -- removed from the main flow as confusing next to the Diagnostic
     * Index, but its evidence references stay traceable here rather than disappearing entirely. */
    private String legacyEvidenceLine(VehicleHealthAssessment ha) {
        if (ha.getSubsystemAssessments() == null || ha.getSubsystemAssessments().isEmpty()) return "";
        List<String> refs = new java.util.ArrayList<>();
        for (SubsystemHealthAssessment sub : ha.getSubsystemAssessments())
            if (sub.getEvidenceReferences() != null) refs.addAll(sub.getEvidenceReferences());
        if (refs.isEmpty()) return "";
        return "Legacy per-signal evidence: " + escapeLatex(String.join(", ", refs)) + "\\\\\n";
    }

    private void footer(StringBuilder tex) {
        tex.append("{\\color{iaiRule}\\hrule}\\vspace{4pt}\n")
           .append("{\\footnotesize\\color{iaiSlate}IgnitionAI \\textbar\\ www.ignitionai.in \\textbar\\ Udyam Reg. No. UDYAM-TN-02-0444115\\\\\n")
           .append("A sole proprietorship registered under Udyam, Tamil Nadu, India}\n\n");
    }

    private String stateLabel(DiagnosticIndexResult.State state) {
        return switch (state) {
            case NORMAL -> "{\\color{iaiGood}Normal}";
            case FLAGGED -> "{\\color{iaiBad}Flagged}";
            case INSUFFICIENT_DATA -> "{\\color{iaiWarn}Insufficient data}";
            case UNSUPPORTED -> "{\\color{iaiSlate}Unsupported}";
        };
    }

    private String shortEvidence(Map<String, String> evidence) {
        return evidence.entrySet().stream().limit(4)
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("; "));
    }

    private String shorten(String s, int n) {
        if (s == null) return "UNAVAILABLE";
        return s.length() <= n ? s : s.substring(0, n) + "\u2026";
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
