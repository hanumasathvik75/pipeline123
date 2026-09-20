package tools.pipeline_runner;

import com.ignitionai.application.InspectionService;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.latex.RepairPlanGenerator;
import com.ignitionai.phase7.validation.CertificateValidator;
import com.ignitionai.phase7.validation.ValidationResult;
import com.ignitionai.virtualsensors.index.DiagnosticIndexResult;
import com.ignitionai.virtualsensors.index.FaultCandidate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Standalone demo tool: loads fixtures/demo-diagnostic-index-showcase.properties, runs it through
 * the real InspectionService, prints the Diagnostic Index results, and writes+compiles a real
 * certificate PDF to out/certificates/demo/. Usage: java -cp out tools.pipeline_runner.CertificateDemoRunner
 */
public final class CertificateDemoRunner {
    public static void main(String[] args) throws Exception {
        String scenarioText = Files.readString(Path.of("fixtures/demo-diagnostic-index-showcase.properties"));
        var config = ScenarioFile.parse(scenarioText);
        var stream = new ObdGenerator(config).generate().getPublicStream();
        System.out.println("Generated " + stream.size() + " OBD messages for " + config.getVehicleId());

        InspectionService service = new InspectionService(new File("virtual-sensors/src/main/resources/sensors"));
        var result = service.assess(stream);

        System.out.println("\n=== Diagnostic Index ===");
        for (DiagnosticIndexResult r : result.certificate.getDiagnosticIndex()) {
            System.out.printf("%-14s state=%-18s health=%s confidence=%s%n", r.getDisplayName(), r.getState(),
                r.getHealthPct() != null ? String.format("%.1f", r.getHealthPct()) : "n/a",
                r.getConfidencePct() != null ? String.format("%.0f%%", r.getConfidencePct()) : "n/a");
            r.getEvidence().forEach((k, v) -> System.out.println("    " + k + " = " + v));
            if (r.getReason() != null) System.out.println("    reason: " + r.getReason());
        }

        System.out.println("\n=== Ranked Fault Candidates ===");
        for (FaultCandidate c : result.certificate.getRankedFaultCandidates()) {
            System.out.printf("%-45s likelihood=%.0f%%  root cause: %s%n", c.getName(), c.getLikelihoodPct(), c.getRootCauseHypothesis());
        }

        System.out.println("\nVHI: " + result.certificate.getHealthAssessment().getVehicleHealthIndex()
            + "  band=" + result.certificate.getHealthAssessment().getHealthBand()
            + "  episodes=" + result.episodes.size());

        ValidationResult validation = new CertificateValidator().validateSnapshot(result.certificate);
        if (!validation.isValid()) { System.out.println("SNAPSHOT INVALID: " + validation.getErrors()); return; }

        Path outDir = Path.of("out/certificates/demo");
        Files.createDirectories(outDir);
        File logo = new File("latex-certificate-generator/src/main/resources/branding/ignitionai_logo.png");
        if (logo.isFile()) Files.copy(logo.toPath(), outDir.resolve("ignitionai_logo.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        compile(outDir, result.certificate.getCertificateId(), new LatexGenerator().generateCertificate(result.certificate));
        compile(outDir, result.certificate.getCertificateId() + "-repair-plan", new RepairPlanGenerator().generateReport(result.certificate));
    }

    private static void compile(Path outDir, String baseName, String texSource) throws Exception {
        Path tex = outDir.resolve(baseName + ".tex");
        Files.writeString(tex, texSource);
        System.out.println("\nWrote " + tex.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", "-halt-on-error",
            "-no-shell-escape", "-output-directory=" + outDir.toAbsolutePath(), tex.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(outDir.resolve(baseName + "-latex-build.log").toFile());
        Process p = pb.start();
        boolean finished = p.waitFor(45, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) { p.destroyForcibly(); System.out.println("pdflatex timed out for " + baseName); return; }
        System.out.println("pdflatex exit code: " + p.exitValue() + " (see " + outDir.resolve(baseName + "-latex-build.log") + ")");
        Path pdf = outDir.resolve(baseName + ".pdf");
        if (Files.exists(pdf)) System.out.println("PDF written: " + pdf.toAbsolutePath());
    }
}
