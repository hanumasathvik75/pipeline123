package tools.pipeline_runner;

import com.ignitionai.application.InspectionResult;
import com.ignitionai.application.InspectionService;
import com.ignitionai.obdgenerator.config.ScenarioFile;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.virtualsensors.index.DiagnosticIndexResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs 7 genuinely different fixture scenarios (healthy, one fault per major subsystem, a
 * multi-fault case exercising the Fault Graph's evidence compounding, a sensor-dropout case, and
 * a DTC-heavy case) through the real InspectionService and asserts each produces the qualitative
 * outcome it's designed to -- not just that the pipeline doesn't crash. This is the "comprehensive
 * test suite" scoped earlier: not one hand-tuned demo fixture, but several independently
 * constructed sessions with known-different expected results.
 */
public final class ComprehensiveScenarioTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Comprehensive Scenario Test Suite (7 scenarios) ===");
        run("healthy", "fixtures/test-scenario-healthy.properties", ComprehensiveScenarioTest::assertHealthy);
        run("combustion-fault", "fixtures/test-scenario-combustion-fault.properties", r -> assertOnlyFlagged(r, "combustion"));
        run("thermal-fault", "fixtures/test-scenario-thermal-fault.properties", r -> assertOnlyFlagged(r, "thermal"));
        run("charging-fault", "fixtures/test-scenario-charging-fault.properties", r -> assertOnlyFlagged(r, "charging"));
        run("multi-fault", "fixtures/test-scenario-multi-fault.properties", ComprehensiveScenarioTest::assertMultiFaultCompounds);
        run("sensor-dropout", "fixtures/test-scenario-sensor-dropout.properties", ComprehensiveScenarioTest::assertSensorDropout);
        run("dtc-heavy", "fixtures/test-scenario-dtc-heavy.properties", ComprehensiveScenarioTest::assertDtcHeavy);

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    private interface Check { void run(InspectionResult result) throws Exception; }

    private static void run(String name, String fixturePath, Check check) {
        try {
            String scenarioText = Files.readString(Path.of(fixturePath));
            var config = ScenarioFile.parse(scenarioText);
            List<ObdMessage> stream = new ObdGenerator(config).generate().getPublicStream();
            InspectionResult result = new InspectionService(new File("virtual-sensors/src/main/resources/sensors")).assess(stream);
            check.run(result);
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + ": " + t.getMessage());
        }
    }

    private static DiagnosticIndexResult find(InspectionResult r, String indexId) {
        for (DiagnosticIndexResult d : r.certificate.getDiagnosticIndex()) if (d.getIndexId().equals(indexId)) return d;
        throw new AssertionError("No result for index " + indexId);
    }

    private static final List<String> MECHANICAL_INDICES = List.of(
        "combustion", "fuel", "thermal", "load_response", "charging", "air_intake_consistency", "timing_compliance");

    private static void assertHealthy(InspectionResult r) {
        for (String id : MECHANICAL_INDICES) {
            DiagnosticIndexResult d = find(r, id);
            if (d.getState() == DiagnosticIndexResult.State.FLAGGED)
                throw new AssertionError("Expected " + id + " NORMAL on the healthy fixture, got FLAGGED: " + d.getEvidence());
        }
        if (!r.certificate.getDtcCodes().isEmpty()) throw new AssertionError("Expected no DTCs on the healthy fixture");
        DiagnosticIndexResult overall = find(r, "overall_condition");
        if (overall.getState() != DiagnosticIndexResult.State.NORMAL) throw new AssertionError("Expected overall_condition NORMAL on the healthy fixture");
    }

    private static void assertOnlyFlagged(InspectionResult r, String expectedFlaggedId) {
        for (String id : MECHANICAL_INDICES) {
            DiagnosticIndexResult d = find(r, id);
            boolean shouldBeFlagged = id.equals(expectedFlaggedId);
            if (shouldBeFlagged && d.getState() != DiagnosticIndexResult.State.FLAGGED)
                throw new AssertionError("Expected " + id + " FLAGGED, got " + d.getState() + ": " + d.getEvidence());
            if (!shouldBeFlagged && d.getState() == DiagnosticIndexResult.State.FLAGGED)
                throw new AssertionError("Expected " + id + " NORMAL (fault was isolated to " + expectedFlaggedId + "), got FLAGGED: " + d.getEvidence());
        }
        DiagnosticIndexResult overall = find(r, "overall_condition");
        if (!"1".equals(overall.getEvidence().get("components_flagged")))
            throw new AssertionError("Expected overall_condition to report exactly 1 flagged component, evidence=" + overall.getEvidence());
    }

    private static void assertMultiFaultCompounds(InspectionResult r) {
        DiagnosticIndexResult combustion = find(r, "combustion");
        DiagnosticIndexResult charging = find(r, "charging");
        if (combustion.getState() != DiagnosticIndexResult.State.FLAGGED) throw new AssertionError("Expected combustion FLAGGED in the multi-fault scenario");
        if (charging.getState() != DiagnosticIndexResult.State.FLAGGED) throw new AssertionError("Expected charging FLAGGED in the multi-fault scenario");
        if (r.certificate.getDtcCodes().isEmpty() || !r.certificate.getDtcCodes().contains("P0301"))
            throw new AssertionError("Expected P0301 present in the multi-fault scenario, got " + r.certificate.getDtcCodes());
        var candidates = r.certificate.getRankedFaultCandidates();
        if (candidates.isEmpty()) throw new AssertionError("Expected the Fault Graph to produce candidates when both an index and a DTC corroborate");
        boolean anyAboveSingleSourceLikelihood = candidates.stream().anyMatch(c -> c.getLikelihoodPct() > 40.0);
        if (!anyAboveSingleSourceLikelihood)
            throw new AssertionError("Expected at least one candidate's probability to reflect compounded evidence (>40%), got " + candidates);
    }

    private static void assertSensorDropout(InspectionResult r) {
        // Known, documented behaviour (see the earlier session finding): the generator's
        // missing_fraction makes readings vanish entirely rather than arrive flagged bad, so
        // signal_quality's missingness_fraction genuinely stays ~0 here -- that isn't a bug, it's
        // measuring something real and different from dropout. What SHOULD move under real
        // dropout is achieved-vs-expected sample rate and the raw-sample-count-driven data
        // sufficiency score, so those are what this test checks.
        DiagnosticIndexResult signalQuality = find(r, "signal_quality");
        double rateRatio = Double.parseDouble(signalQuality.getEvidence().get("avg_sample_rate_ratio"));
        if (rateRatio >= 1.0) throw new AssertionError("Expected achieved-vs-expected sample rate to show some real degradation under 50% dropout, got ratio=" + rateRatio);
        Double dataSufficiency = r.certificate.getHealthAssessment().getOverallConfidence();
        if (dataSufficiency == null || dataSufficiency > 0.75)
            throw new AssertionError("Expected data sufficiency meaningfully below the clean-baseline ~0.85 ceiling under 50% dropout, got " + dataSufficiency);
    }

    private static void assertDtcHeavy(InspectionResult r) {
        List<String> dtcs = r.certificate.getDtcCodes();
        if (dtcs.size() != 5) throw new AssertionError("Expected exactly 5 DTCs, got " + dtcs.size() + ": " + dtcs);
        for (String code : List.of("P0301", "P0171", "C0035", "B1318", "U0100"))
            if (!dtcs.contains(code)) throw new AssertionError("Expected " + code + " present, got " + dtcs);
        for (String id : MECHANICAL_INDICES) {
            DiagnosticIndexResult d = find(r, id);
            if (d.getState() == DiagnosticIndexResult.State.FLAGGED)
                throw new AssertionError("Expected clean signals (no flagged index) in the DTC-heavy scenario, got " + id + " FLAGGED");
        }
    }
}
