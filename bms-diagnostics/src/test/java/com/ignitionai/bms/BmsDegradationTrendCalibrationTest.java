package com.ignitionai.bms;

import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Calibration test for {@link BmsDegradationTrend}: proves it can actually detect realistic
 * Li-ion fade rates (1.5-3%/year), not just stay STABLE forever because a threshold was tuned
 * against one flat dataset. Real-world fade is comparable in size to this dataset's own measured
 * session-to-session estimator wobble (~1pp) -- a genuine signal-to-noise problem, so detection is
 * necessarily statistical, not perfect per-instance. These assertions check the estimator's
 * documented, Monte-Carlo-verified operating point (see config/bms-diagnostics-config.json's
 * trend_rationale) using fixed seeds so the counts are exactly reproducible, not "on average"
 * claims that could flake.
 */
public final class BmsDegradationTrendCalibrationTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== BmsDegradationTrend Calibration Tests ===");
        test("detectsSevereDegradationMoreOftenThanNotAndRarelyMisreadsAsImproving", BmsDegradationTrendCalibrationTest::testSevereDegradationDetectionRate);
        test("nullNoiseOnlyDoesNotAlwaysFalseFlagWorsening", BmsDegradationTrendCalibrationTest::testNullFalsePositiveRateBounded);
        test("meanSlopeEstimateTracksTrueSlopeSignAndRoughMagnitude", BmsDegradationTrendCalibrationTest::testMeanSlopeEstimateIsReasonable);
        test("sparsePostDiscontinuityHistoryStaysHonestlyStable", BmsDegradationTrendCalibrationTest::testPostDiscontinuityGuard);
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    private interface Check { void run(); }

    private static void test(String name, Check check) {
        try { check.run(); passed++; System.out.println("  PASS  " + name); }
        catch (Throwable t) { failed++; System.out.println("  FAIL  " + name + ": " + t.getMessage()); }
    }

    private static void testSevereDegradationDetectionRate() {
        int trials = 80, worsening = 0, improving = 0;
        for (int t = 0; t < trials; t++) {
            TrendDirection d = simulate(-3.0 / 365.0, 7000 + t);
            if (d == TrendDirection.WORSENING) worsening++;
            if (d == TrendDirection.IMPROVING) improving++;
        }
        if (worsening < trials / 2)
            throw new AssertionError("Expected a fast (-3%/yr) real degradation to be flagged WORSENING in at least half of " + trials + " trials, got " + worsening);
        if (improving > 0)
            throw new AssertionError("A genuinely degrading pack should never be misread as IMPROVING, got " + improving + "/" + trials);
    }

    private static void testNullFalsePositiveRateBounded() {
        int trials = 80, worsening = 0;
        for (int t = 0; t < trials; t++) {
            if (simulate(0.0, 5000 + t) == TrendDirection.WORSENING) worsening++;
        }
        // Detecting real-world-scale fade against real-world-scale estimator noise is a genuine
        // signal-to-noise tradeoff (documented in config) -- some false positives on pure noise are
        // the accepted cost of being sensitive enough to catch real degradation at all. This bounds
        // that cost rather than pretending it's zero.
        if (worsening > trials * 0.35)
            throw new AssertionError("Expected the false-WORSENING rate on pure noise (no real degradation) to stay under 35% of " + trials + " trials, got " + worsening);
    }

    private static void testMeanSlopeEstimateIsReasonable() {
        int trials = 80;
        double trueSlope = -3.0 / 365.0;
        double sum = 0;
        for (int t = 0; t < trials; t++) sum += simulateSlope(trueSlope, 7000 + t);
        double mean = sum / trials;
        if (mean >= 0) throw new AssertionError("Expected the mean slope estimate over " + trials + " trials to be negative (degrading), got " + mean);
        if (Math.abs(mean) > Math.abs(trueSlope) * 6)
            throw new AssertionError("Expected the mean slope estimate to stay within a reasonable multiple of the true injected slope " + trueSlope + ", got mean=" + mean);
    }

    /** Mirrors the real dataset's own shape: a discontinuity (pack swap/recalibration) followed by
     * too few sessions to say anything about direction -- must not fabricate a confident IMPROVING
     * or WORSENING read off 1-3 post-event points, which is exactly the false IMPROVING this guard
     * was added to fix (see BmsDegradationTrend's insufficientPostEventHistory handling). */
    private static void testPostDiscontinuityGuard() {
        List<BmsTrendPoint> points = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double soh = (i % 2 == 0) ? 97.67 : 96.67;
            points.add(new BmsTrendPoint("pre-" + i, soh, i == 0 ? 0.0 : 36.0, false, 1.0));
        }
        points.add(new BmsTrendPoint("event", 100.0, 36.0, true, 1.0));
        points.add(new BmsTrendPoint("post-1", 100.0, 24.0, false, 1.0));
        points.add(new BmsTrendPoint("post-2", 100.0, 24.0, false, 1.0));

        BmsTrendResult result = BmsDegradationTrend.analyze(points);
        if (result.direction != TrendDirection.STABLE)
            throw new AssertionError("Expected STABLE (too little clean history since the discontinuity to claim a direction), got " + result.direction);
        if (!result.note.toLowerCase(java.util.Locale.ROOT).contains("discontinuity"))
            throw new AssertionError("Expected the note to explain the guard, got: " + result.note);
    }

    private static TrendDirection simulate(double trueSlopePerDay, long seed) { return analyze(trueSlopePerDay, seed).direction; }
    private static double simulateSlope(double trueSlopePerDay, long seed) { return analyze(trueSlopePerDay, seed).slopePctPerDay; }

    private static BmsTrendResult analyze(double trueSlopePerDay, long seed) {
        Random rnd = new Random(seed);
        double startSoh = 100.0;
        int sessions = 60;
        double avgSpacingDays = 365.0 / sessions;
        List<BmsTrendPoint> points = new ArrayList<>();
        double elapsedDays = 0, prevDays = 0;
        for (int i = 0; i < sessions; i++) {
            double trueSoh = startSoh + trueSlopePerDay * elapsedDays;
            double wobble = (rnd.nextBoolean() ? 1 : -1) * (0.5 + rnd.nextDouble() * 0.5);
            double observedSoh = trueSoh + wobble;
            double hoursSincePrevious = i == 0 ? 0.0 : (elapsedDays - prevDays) * 24.0;
            points.add(new BmsTrendPoint("S" + i, observedSoh, hoursSincePrevious, false, 1.0));
            prevDays = elapsedDays;
            elapsedDays += avgSpacingDays * (0.6 + rnd.nextDouble() * 0.8);
        }
        return BmsDegradationTrend.analyze(points);
    }
}
