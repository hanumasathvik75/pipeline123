package tools.pipeline_runner;

import com.ignitionai.bms.*;
import com.ignitionai.phase7.latex.BmsReportGenerator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Verification tool: runs real per-session BMS diagnostics and a cross-session SoH trend against
 * the actual processed_data/{april,may,june,july} export folders. Usage:
 * java -cp out tools.pipeline_runner.BmsAnalysisRunner "C:\path\to\processed_data"
 *
 * April and May are diagnosed per-session but excluded from the trend: most of their timestamp
 * fields are corrupted to a "1900-01-01" placeholder, so there is no trustworthy way to place them
 * in real chronological order relative to June/July. June and July both have real, usable dates
 * and are combined into one trend. A session whose Pack SoH is higher than the previous session's
 * by more than a small tolerance is treated as a discontinuity (a real battery's SoH should not
 * meaningfully increase under normal use -- an increase implies an event like a pack swap or BMS
 * recalibration, not gradual physical improvement) and resets the trend model's uncertainty rather
 * than being fitted as if it were smooth improvement.
 */
public final class BmsAnalysisRunner {
    // Real, clean (non-event) sessions in this dataset wobble by about 1 percentage point between
    // drive-profile templates (96.67 <-> 97.67); the one genuine event (a pack swap or BMS
    // recalibration around 28 July) is a 2.33pp jump to 100.00. 1.5 sits between the two so routine
    // wobble isn't misread as an event. First attempt used 0.5 and fired on nearly every session --
    // caught by inspecting the discontinuity log before trusting the trend output, not assumed correct.
    private static final double SOH_INCREASE_DISCONTINUITY_THRESHOLD_PCT = 1.5;

    public static void main(String[] args) throws Exception {
        Path base = Path.of(args.length > 0 ? args[0]
            : "C:\\Users\\SHASHANK\\.gemini\\antigravity\\scratch\\bqi_project\\processed_data");

        String[] folders = {"aprl sheet2", "may sheet2", "june", "july"};
        List<TimedSession> timedSessions = new ArrayList<>();
        List<BmsSessionDiagnosticsResult> allSessions = new ArrayList<>();

        for (String folder : folders) {
            File dir = base.resolve(folder).toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".csv") && !name.contains("DROPPED_ROWS"));
            if (files == null) { System.out.println("Folder not found: " + dir); continue; }
            Arrays.sort(files, Comparator.comparing(File::getName));
            System.out.println("\n=== " + folder + " (" + files.length + " files) ===");
            for (File f : files) {
                List<BmsReading> readings = BmsCsvReader.read(f.toPath());
                if (readings.isEmpty()) { System.out.printf("%-40s EMPTY/UNREADABLE%n", f.getName()); continue; }
                BmsSessionDiagnosticsResult d = BmsSessionDiagnostics.evaluate(f.getName(), readings);
                allSessions.add(d);
                System.out.printf("%-40s n=%-5d SoH=%-6s imbalance(max)=%-7s socDisagree(max)=%-6s thermalSpread(max)=%-6s flags=[%s%s%s]%n",
                    f.getName(), d.readingCount, fmt(d.representativeSohPct), fmt(d.maxVoltageImbalanceV), fmt(d.maxSocDisagreementPct), fmt(d.maxThermalSpreadC),
                    d.cellImbalanceFlagged ? "IMBALANCE " : "", d.socDisagreementFlagged ? "SOC_DISAGREE " : "", d.thermalSpreadFlagged ? "THERMAL_SPREAD" : "");

                LocalDateTime firstTs = readings.stream().map(r -> r.timestamp).filter(Objects::nonNull).findFirst().orElse(null);
                if (firstTs != null && d.representativeSohPct != null && (folder.equals("june") || folder.equals("july"))) {
                    timedSessions.add(new TimedSession(f.getName(), firstTs, d.representativeSohPct, d.confidence));
                }
            }
        }

        System.out.println("\n=== Cross-session SoH trend (June + July only -- real, trustworthy timestamps) ===");
        timedSessions.sort(Comparator.comparing(t -> t.timestamp));
        List<BmsTrendPoint> points = new ArrayList<>();
        LocalDateTime previous = null;
        Double previousSoh = null;
        for (TimedSession t : timedSessions) {
            double hoursSincePrevious = previous == null ? 0.0 : ChronoUnit.MINUTES.between(previous, t.timestamp) / 60.0;
            boolean discontinuity = previousSoh != null && (t.sohPct - previousSoh) > SOH_INCREASE_DISCONTINUITY_THRESHOLD_PCT;
            if (discontinuity) System.out.printf("  [discontinuity] %s: SoH %.2f -> %.2f treated as a reset event, not a fitted trend%n", t.sessionId, previousSoh, t.sohPct);
            points.add(new BmsTrendPoint(t.sessionId, t.sohPct, hoursSincePrevious, discontinuity, t.confidence));
            previous = t.timestamp; previousSoh = t.sohPct;
        }
        BmsTrendResult trend = BmsDegradationTrend.analyze(points);
        System.out.println("Direction: " + trend.direction);
        System.out.println("Estimated current SoH: " + fmt(trend.estimatedCurrentSohPct));
        System.out.println("Slope (%/day): " + fmt(trend.slopePctPerDay));
        System.out.println("Uncertainty: " + fmt(trend.uncertainty));
        System.out.println("Points used: " + trend.pointsUsed);
        System.out.println("Note: " + trend.note);

        String vehicleId = "BQI-FLEET-VEHICLE";
        String reportId = "BMS-" + System.currentTimeMillis();
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String texSource = new BmsReportGenerator().generateReport(vehicleId, reportId, generatedAt, allSessions, trend);
        Path outDir = Path.of("out/bms-reports");
        Files.createDirectories(outDir);
        File logo = new File("latex-certificate-generator/src/main/resources/branding/ignitionai_logo.png");
        if (logo.isFile()) Files.copy(logo.toPath(), outDir.resolve("ignitionai_logo.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Path tex = outDir.resolve(reportId + ".tex");
        Files.writeString(tex, texSource);
        System.out.println("\nWrote " + tex.toAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", "-halt-on-error",
            "-no-shell-escape", "-output-directory=" + outDir.toAbsolutePath(), tex.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(outDir.resolve(reportId + "-latex-build.log").toFile());
        Process p = pb.start();
        boolean finished = p.waitFor(45, TimeUnit.SECONDS);
        if (!finished) { p.destroyForcibly(); System.out.println("pdflatex timed out for " + reportId); return; }
        System.out.println("pdflatex exit code: " + p.exitValue() + " (see " + outDir.resolve(reportId + "-latex-build.log") + ")");
        Path pdf = outDir.resolve(reportId + ".pdf");
        if (Files.exists(pdf)) System.out.println("BMS report PDF written: " + pdf.toAbsolutePath());
    }

    private static String fmt(Double d) { return d == null ? "n/a" : String.format(Locale.ROOT, "%.3f", d); }

    private static final class TimedSession {
        final String sessionId; final LocalDateTime timestamp; final double sohPct; final double confidence;
        TimedSession(String sessionId, LocalDateTime timestamp, double sohPct, double confidence) {
            this.sessionId = sessionId; this.timestamp = timestamp; this.sohPct = sohPct; this.confidence = confidence;
        }
    }
}
