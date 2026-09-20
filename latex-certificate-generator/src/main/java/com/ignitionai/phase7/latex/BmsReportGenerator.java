package com.ignitionai.phase7.latex;

import com.ignitionai.bms.BmsSessionDiagnosticsResult;
import com.ignitionai.bms.BmsTrendResult;

import java.util.List;
import java.util.Locale;

/**
 * Battery Management System diagnostics report: a separate PDF from the (OBD-based) Vehicle
 * Health Certificate, since a BMS pack's own telemetry (per-cell voltages, dual SoC estimators,
 * cell temperatures, Pack SoH) is a genuinely different evidence stream that most OBD-II ports
 * never expose. Covers both what a single session can say on its own (cell imbalance, SoC
 * estimator disagreement, thermal spread -- see {@link com.ignitionai.bms.BmsSessionDiagnostics})
 * and, when there's enough trustworthy cross-session history, the predictive Pack SoH degradation
 * trend (see {@link com.ignitionai.bms.BmsDegradationTrend}). When history is insufficient this
 * reports that honestly rather than fabricating a confident trend line.
 */
public class BmsReportGenerator {

    public String generateReport(String vehicleId, String reportId, String generatedAt,
            List<BmsSessionDiagnosticsResult> sessions, BmsTrendResult trend) {
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

        header(tex, vehicleId, reportId, generatedAt);
        trendSection(tex, trend);
        sessionTableSection(tex, sessions);
        flagSummarySection(tex, sessions);
        disclaimerAndFooter(tex, reportId);

        tex.append("\\end{document}\n");
        return tex.toString();
    }

    private void header(StringBuilder tex, String vehicleId, String reportId, String generatedAt) {
        tex.append("\\noindent{\\footnotesize\\color{iaiSlate}\\textbf{BATTERY MANAGEMENT SYSTEM DIAGNOSTICS REPORT}}\\\\[4pt]\n")
           .append("\\noindent\n")
           .append("\\begin{minipage}[c]{0.55\\textwidth}\n")
           .append("\\IfFileExists{ignitionai_logo.png}{\\includegraphics[height=1.15cm]{ignitionai_logo.png}}{\\textbf{\\huge\\color{iaiNavy}ignitionAI}}\n")
           .append("\\end{minipage}\\hfill\n")
           .append("\\begin{minipage}[c]{0.4\\textwidth}\\raggedleft\n")
           .append("{\\bfseries\\Large\\rmfamily\\color{iaiNavy}BMS Diagnostics Report}\\\\[3pt]\n")
           .append("{\\small\\color{iaiSlate}Report ID ").append(escapeLatex(reportId)).append("}\n")
           .append("\\end{minipage}\n\n")
           .append("\\vspace{6pt}{\\color{iaiNavy}\\hrule height 1.6pt}\\vspace{1pt}{\\color{iaiRule}\\hrule height 0.6pt}\\vspace{8pt}\n\n")
           .append("\\noindent{\\small Vehicle: ").append(escapeLatex(vehicleId))
           .append(" \\quad\\textbar\\quad Generated: ").append(escapeLatex(generatedAt)).append("}\\\\[10pt]\n\n")
           .append("\\noindent{\\footnotesize\\color{iaiSlate}Evidence source: the pack's own Battery Management System telemetry (per-cell voltages, dual SoC estimators, cell temperatures, reported Pack SoH) -- a separate signal path from the OBD-II Vehicle Health Certificate, since most OBD-II ports never expose BMS internals.}\\\\[10pt]\n\n");
    }

    private void trendSection(StringBuilder tex, BmsTrendResult trend) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Pack State-of-Health Trend}\\\\[2pt]\n");
        if (trend == null || trend.estimatedCurrentSohPct == null) {
            String note = trend == null ? "No cross-session history was supplied for this vehicle." : trend.note;
            tex.append("{\\small\\color{iaiSlate}\\textbf{Insufficient history for a trend.} ").append(escapeLatex(note))
               .append(" A confident degradation trend needs multiple sessions with trustworthy, chronologically-ordered timestamps; this report will not fabricate one from too little data.}\\\\[10pt]\n\n");
            return;
        }
        String directionColor = switch (trend.direction) {
            case WORSENING -> "iaiBad";
            case IMPROVING -> "iaiWarn";
            case STABLE -> "iaiGood";
            default -> "iaiSlate";
        };
        tex.append("\\begin{tabularx}{\\textwidth}{@{}p{4.2cm}X@{}}\n")
           .append("{\\small\\bfseries Direction} & {\\small\\color{").append(directionColor).append("}\\bfseries ").append(escapeLatex(trend.direction.name())).append("} \\\\\n")
           .append("{\\small\\bfseries Estimated current SoH} & {\\small ").append(String.format(Locale.ROOT, "%.2f\\%%", trend.estimatedCurrentSohPct)).append("} \\\\\n")
           .append("{\\small\\bfseries Slope} & {\\small ").append(trend.slopePctPerDay == null ? "n/a" : String.format(Locale.ROOT, "%.4f\\%%/day", trend.slopePctPerDay)).append("} \\\\\n")
           .append("{\\small\\bfseries Model uncertainty} & {\\small ").append(trend.uncertainty == null ? "n/a" : String.format(Locale.ROOT, "%.4f", trend.uncertainty)).append("} \\\\\n")
           .append("{\\small\\bfseries Sessions used} & {\\small ").append(trend.pointsUsed).append("} \\\\\n")
           .append("\\end{tabularx}\\\\[4pt]\n")
           .append("{\\footnotesize\\color{iaiSlate}").append(escapeLatex(trend.note)).append("}\\\\[10pt]\n\n");
    }

    private void sessionTableSection(StringBuilder tex, List<BmsSessionDiagnosticsResult> sessions) {
        tex.append("{\\bfseries\\color{iaiNavy}\\large Per-Session Diagnostics}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Every row is computed from that session's own telemetry alone -- cell imbalance, SoC-EKF vs SoC-Coulomb-Count disagreement, and thermal spread across the pack's cell-temperature sensors all need no multi-session history.}\\\\[6pt]\n")
           .append("\\begin{longtable}{@{}p{3.6cm}p{1.2cm}p{1.6cm}p{2.1cm}p{2.1cm}p{2.1cm}p{2.8cm}@{}}\n\\toprule\n")
           .append("\\textbf{Session} & \\textbf{n} & \\textbf{SoH} & \\textbf{Imbalance (max)} & \\textbf{SoC disagree (max)} & \\textbf{Thermal spread (max)} & \\textbf{Flags} \\\\\n\\midrule\n\\endfirsthead\n")
           .append("\\toprule\n\\textbf{Session} & \\textbf{n} & \\textbf{SoH} & \\textbf{Imbalance (max)} & \\textbf{SoC disagree (max)} & \\textbf{Thermal spread (max)} & \\textbf{Flags} \\\\\n\\midrule\n\\endhead\n")
           .append("\\bottomrule\n\\endfoot\n");
        for (BmsSessionDiagnosticsResult s : sessions) {
            String flags = String.join(", ",
                joinIf(s.cellImbalanceFlagged, "Cell imbalance"),
                joinIf(s.socDisagreementFlagged, "SoC disagreement"),
                joinIf(s.thermalSpreadFlagged, "Thermal spread")).replaceAll("^, |, $", "");
            tex.append("{\\footnotesize ").append(escapeLatex(s.sessionId)).append("} & ")
               .append(s.readingCount).append(" & ")
               .append(fmtPct(s.representativeSohPct)).append(" & ")
               .append(fmtV(s.maxVoltageImbalanceV)).append(" & ")
               .append(fmtPct(s.maxSocDisagreementPct)).append(" & ")
               .append(fmtC(s.maxThermalSpreadC)).append(" & ")
               .append("{\\footnotesize ").append(flags.isEmpty() ? "\\textcolor{iaiGood}{None}" : "\\textcolor{iaiBad}{" + escapeLatex(flags) + "}").append("} \\\\\n");
        }
        tex.append("\\end{longtable}\n\n\\vspace{6pt}\n\n");
    }

    private void flagSummarySection(StringBuilder tex, List<BmsSessionDiagnosticsResult> sessions) {
        long imbalance = sessions.stream().filter(s -> s.cellImbalanceFlagged).count();
        long socDisagree = sessions.stream().filter(s -> s.socDisagreementFlagged).count();
        long thermal = sessions.stream().filter(s -> s.thermalSpreadFlagged).count();
        tex.append("{\\bfseries\\color{iaiNavy}\\large Summary}\\\\[2pt]\n")
           .append("{\\small\\color{iaiSlate}Across ").append(sessions.size()).append(" session(s): ")
           .append(imbalance).append(" flagged for cell voltage imbalance, ")
           .append(socDisagree).append(" flagged for SoC estimator disagreement, ")
           .append(thermal).append(" flagged for thermal spread.}\\\\[10pt]\n\n");
    }

    private void disclaimerAndFooter(StringBuilder tex, String reportId) {
        tex.append("{\\footnotesize\\itshape\\color{iaiSlate}Method boundary: session-level flags use fixed thresholds calibrated against this dataset (see config/bms-diagnostics-config.json), not a model fitted against confirmed pack failures. The SoH trend, when present, is a Kalman-filtered slope over the sessions listed above; a session following a known discontinuity (e.g. a pack swap or BMS recalibration) resets the model's uncertainty rather than being fitted as gradual physical change.}\n\n\\vspace{10pt}\n\n")
           .append("{\\color{iaiRule}\\hrule}\\vspace{4pt}\n")
           .append("{\\footnotesize\\color{iaiSlate}IgnitionAI \\textbar\\ www.ignitionai.in \\textbar\\ Udyam Reg. No. UDYAM-TN-02-0444115 \\textbar\\ Report ")
           .append(escapeLatex(reportId)).append("\\\\\n")
           .append("A sole proprietorship registered under Udyam, Tamil Nadu, India}\n\n");
    }

    private String joinIf(boolean condition, String label) { return condition ? label : ""; }
    private String fmtPct(Double d) { return d == null ? "n/a" : String.format(Locale.ROOT, "%.1f\\%%", d); }
    private String fmtV(Double d) { return d == null ? "n/a" : String.format(Locale.ROOT, "%.3fV", d); }
    private String fmtC(Double d) { return d == null ? "n/a" : String.format(Locale.ROOT, "%.1f\\textdegree{}C", d); }

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
