package com.ignitionai.phase7.test;

import com.ignitionai.phase6.Phase6Fixtures;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.CertificateValidator;
import com.ignitionai.phase7.validation.ValidationResult;
import com.ignitionai.phase7.app.DealershipAppMVP;
import com.ignitionai.phase7.app.AssessmentHistoryView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Phase7Tests {

    public static void main(String[] args) {
        System.out.println("=== Phase 7 Tests ===");
        int passed = 0;
        int failed = 0;
        
        try { testCertificateContractValidation(); passed++; System.out.println("  PASS  testCertificateContractValidation"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testCertificateContractValidation: " + t.getMessage()); }
        try { testDeterministicCertificateGeneration(); passed++; System.out.println("  PASS  testDeterministicCertificateGeneration"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testDeterministicCertificateGeneration: " + t.getMessage()); }
        try { testScoreAndSeverityRendering(); passed++; System.out.println("  PASS  testScoreAndSeverityRendering"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testScoreAndSeverityRendering: " + t.getMessage()); }
        try { testMissingDataRendering(); passed++; System.out.println("  PASS  testMissingDataRendering"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testMissingDataRendering: " + t.getMessage()); }
        try { testEvidenceReferencePreservation(); passed++; System.out.println("  PASS  testEvidenceReferencePreservation"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testEvidenceReferencePreservation: " + t.getMessage()); }
        try { testHistoricalComparison(); passed++; System.out.println("  PASS  testHistoricalComparison"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testHistoricalComparison: " + t.getMessage()); }
        try { testInvalidInputRejection(); passed++; System.out.println("  PASS  testInvalidInputRejection"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testInvalidInputRejection: " + t.getMessage()); }
        try { testPhase6FixtureCompatibility(); passed++; System.out.println("  PASS  testPhase6FixtureCompatibility"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testPhase6FixtureCompatibility: " + t.getMessage()); }
        try { testLatexCompilation(); passed++; System.out.println("  PASS  testLatexCompilation"); } catch(Throwable t) { failed++; System.out.println("  FAIL  testLatexCompilation: " + t.getMessage()); }
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testCertificateContractValidation() {
        VehicleHealthAssessment ha = Phase6Fixtures.getHealthyVehicle();
        CertificateSnapshot snap = new CertificateSnapshot(ha);
        CertificateValidator validator = new CertificateValidator();
        ValidationResult res = validator.validateSnapshot(snap);
        if (!res.isValid()) {
            throw new AssertionError("Healthy fixture should be valid, but got errors: " + String.join(", ", res.getErrors()));
        }
    }

    private static void testDeterministicCertificateGeneration() {
        VehicleHealthAssessment ha1 = Phase6Fixtures.getHealthyVehicle();
        CertificateSnapshot snap1 = new CertificateSnapshot(ha1);
        
        VehicleHealthAssessment ha2 = Phase6Fixtures.getHealthyVehicle();
        CertificateSnapshot snap2 = new CertificateSnapshot(ha2);
        
        if (!snap1.getCertificateId().equals(snap2.getCertificateId())) {
            throw new AssertionError("Certificate generation is not deterministic for identical inputs. " + 
                snap1.getCertificateId() + " != " + snap2.getCertificateId());
        }
    }

    private static void testScoreAndSeverityRendering() {
        VehicleHealthAssessment ha = Phase6Fixtures.getDegradedVehicle();
        CertificateSnapshot snap = new CertificateSnapshot(ha);
        LatexGenerator gen = new LatexGenerator();
        String latex = gen.generateCertificate(snap);
        
        if (!latex.contains("68.00")) throw new AssertionError("VHI score not rendered correctly");
        if (!latex.contains("C\\_FAIR")) throw new AssertionError("Health band not rendered correctly");
        if (!latex.contains("MEDIUM")) throw new AssertionError("Severity not rendered correctly");
    }

    private static void testMissingDataRendering() {
        VehicleHealthAssessment ha = Phase6Fixtures.getMissingDataVehicle();
        CertificateSnapshot snap = new CertificateSnapshot(ha);
        LatexGenerator gen = new LatexGenerator();
        String latex = gen.generateCertificate(snap);
        
        if (!latex.contains("UNAVAILABLE")) throw new AssertionError("Missing data did not fall back to UNAVAILABLE in LaTeX");
        if (!latex.contains("No Subsystem Data Available")) throw new AssertionError("Missing subsystems not handled gracefully");
    }

    private static void testEvidenceReferencePreservation() {
        VehicleHealthAssessment ha = Phase6Fixtures.getRecoveredVehicle();
        CertificateSnapshot snap = new CertificateSnapshot(ha);
        LatexGenerator gen = new LatexGenerator();
        String latex = gen.generateCertificate(snap);
        
        if (!latex.contains("evt-51, rep-01")) throw new AssertionError("Evidence references not preserved in LaTeX");
    }

    private static void testHistoricalComparison() {
        CertificateSnapshot older = new CertificateSnapshot(Phase6Fixtures.getHistoricalComparison1());
        CertificateSnapshot newer = new CertificateSnapshot(Phase6Fixtures.getHistoricalComparison2());
        
        PrintStream stdout = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        
        try {
            AssessmentHistoryView view = new AssessmentHistoryView();
            view.displayComparison(older, newer);
        } finally {
            System.setOut(stdout);
        }
        
        String output = baos.toString();
        if (!output.contains("(-15.00)")) throw new AssertionError("Historical VHI negative diff not correctly formatted");
        if (!output.contains("(-25.00)")) throw new AssertionError("Historical subsystem negative diff not correctly formatted");
    }

    private static void testInvalidInputRejection() {
        // Test invalid range
        VehicleHealthAssessment ha = new VehicleHealthAssessment(
            "VIN_INVALID", "VERIFIED", "SUV", "2026-09-18", "30d", 1000.0,
            150.0, com.ignitionai.phase6.VehicleHealthBand.A_EXCELLENT, com.ignitionai.phase6.SeverityLevel.NONE,
            0.9, 0.1, "SUFFICIENT", null, "v1", "v1", "REL-1"
        );
        CertificateSnapshot snap = new CertificateSnapshot(ha);
        CertificateValidator validator = new CertificateValidator();
        ValidationResult res = validator.validateSnapshot(snap);
        
        if (res.isValid()) throw new AssertionError("Validator should have rejected invalid VHI range");
        if (!res.getErrors().stream().anyMatch(e -> e.contains("range"))) {
            throw new AssertionError("Expected range error message, got: " + res.getErrors());
        }
    }

    private static void testPhase6FixtureCompatibility() {
        if (Phase6Fixtures.getCriticalSubsystemVehicle().getHealthBand() != com.ignitionai.phase6.VehicleHealthBand.E_CRITICAL) {
            throw new AssertionError("Phase 6 fixture critical band mismatch");
        }
        if (Phase6Fixtures.getLowConfidenceVehicle().getOverallConfidence() != 0.40) {
            throw new AssertionError("Phase 6 fixture low confidence mismatch");
        }
    }

    private static void testLatexCompilation() {
        // Test validator checks LaTeX structure.
        LatexGenerator gen = new LatexGenerator();
        String latex = gen.generateCertificate(new CertificateSnapshot(Phase6Fixtures.getHealthyVehicle()));
        
        CertificateValidator validator = new CertificateValidator();
        ValidationResult res = validator.validateLatex(latex);
        if (!res.isValid()) throw new AssertionError("LaTeX structure failed validation: " + res.getErrors());
        
        // Ensure unsupported claims are rejected
        ValidationResult resBad = validator.validateLatex("\\begin{document} we recommend you replace the engine \\end{document}");
        if (resBad.isValid()) throw new AssertionError("Validator failed to catch unsupported claims in LaTeX");
    }
}
