package com.ignitionai.bms;

import com.ignitionai.degradation.analyzer.StateSpaceModel;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-session Pack SoH trend, reusing the same {@link StateSpaceModel} (Kalman filter) the OBD
 * degradation pipeline already uses -- the math is domain-agnostic, only the noise parameters
 * differ (see config/bms-diagnostics-config.json). A discontinuity (pack swap, BMS recalibration)
 * resets the filter's uncertainty via {@link StateSpaceModel#resetUncertainty()} instead of being
 * fitted as if it were a gradual physical change, the same handling {@code TrendAnalyzer} already
 * applies for a calibration change or maintenance action on the OBD side.
 *
 * Real-world Li-ion fade (roughly 1.5-3%/year under normal use) is comparable in size to this
 * dataset's own measured session-to-session estimator wobble (~1pp) -- a real, verified
 * signal-to-noise problem, not something a bigger/smaller threshold alone fixes. Points are first
 * aggregated into small buckets (see config's trend_aggregation_window_sessions) to average that
 * noise down before the filter runs, the same way averaging reduces any i.i.d. noise. See
 * config/bms-diagnostics-config.json's trend_rationale for the Monte Carlo sweep that picked the
 * current noise/threshold/window values and documents their honest detection/false-positive rates.
 */
public final class BmsDegradationTrend {
    private static final int MIN_POINTS_FOR_TREND = 4;

    private BmsDegradationTrend() {}

    public static BmsTrendResult analyze(List<BmsTrendPoint> points) {
        if (points.size() < MIN_POINTS_FOR_TREND) {
            return BmsTrendResult.insufficientHistory(
                "Only " + points.size() + " ordered session(s) with trustworthy timing; need at least " + MIN_POINTS_FOR_TREND + ".", points.size());
        }
        BmsDiagnosticsConfig cfg = BmsDiagnosticsConfig.get();
        int aggregationWindow = (int) cfg.number("trend_aggregation_window_sessions");
        List<BmsTrendPoint> aggregated = aggregationWindow > 1 ? aggregate(points, aggregationWindow) : points;
        if (aggregated.size() < MIN_POINTS_FOR_TREND) {
            return BmsTrendResult.insufficientHistory(
                "Only " + aggregated.size() + " point(s) remain after grouping every " + aggregationWindow
                    + " session(s) to reduce estimator noise (from " + points.size() + " raw sessions); need at least " + MIN_POINTS_FOR_TREND + ".",
                points.size());
        }
        StateSpaceModel model = new StateSpaceModel(aggregated.get(0).sohPct,
            cfg.number("trend_process_noise_var_soh"), cfg.number("trend_process_noise_var_velocity"), cfg.number("trend_measurement_noise_var"));

        int pointsSinceLastDiscontinuity = 1;
        for (int i = 1; i < aggregated.size(); i++) {
            BmsTrendPoint p = aggregated.get(i);
            if (p.discontinuityBeforePoint) { model.resetUncertainty(); pointsSinceLastDiscontinuity = 0; }
            model.update(p.hoursSincePrevious, p.sohPct, p.confidence);
            pointsSinceLastDiscontinuity++;
        }

        double slopePerHour = model.getVelocity();
        double slopePerDay = slopePerHour * 24.0;
        double uncertainty = model.getUncertainty();
        double threshold = cfg.number("trend_direction_threshold_pct_per_day");

        // A reset's very next update still gets a velocity "kick" from that one jump (the reset
        // clears position/velocity uncertainty, but the first post-reset measurement still updates
        // velocity through the covariance the predict step just rebuilt) -- with too few clean
        // points since the event, that kick is indistinguishable from real direction. Reported real
        // data confirmed this: 28-July's pack-swap/recalibration jump left only 3 raw sessions (1
        // aggregated point) afterward, which produced a spurious IMPROVING read before this guard.
        boolean insufficientPostEventHistory = pointsSinceLastDiscontinuity < MIN_POINTS_FOR_TREND;

        TrendDirection direction;
        if (insufficientPostEventHistory) direction = TrendDirection.STABLE;
        else if (slopePerDay < -threshold) direction = TrendDirection.WORSENING; // SoH falling
        else if (slopePerDay > threshold) direction = TrendDirection.IMPROVING;
        else direction = TrendDirection.STABLE;

        // StateSpaceModel is a generic, domain-agnostic filter (shared with the OBD trend) with no
        // notion that SoH is physically bounded at 100% -- a pack cannot exceed its own rated
        // capacity. A slight positive slope extrapolated off a just-reset discontinuity can nudge
        // the raw filtered state a hair past 100 (observed: 100.014 against a 100.000 ceiling
        // sample); clamp to the physical range here rather than reporting a nonsensical >100% SoH.
        double clampedSoh = Math.max(0.0, Math.min(100.0, model.getState()));

        String note = insufficientPostEventHistory
            ? "Reported as STABLE: only " + pointsSinceLastDiscontinuity + " aggregated point(s) of clean history since the most recent discontinuity"
                + " (need at least " + MIN_POINTS_FOR_TREND + ") -- too little post-event data to say whether SoH is trending up or down yet, so this"
                + " does not claim a direction it can't support. Slope/estimate below are still the model's raw current output for reference."
            : "State-space trend over " + points.size() + " raw sessions (grouped into " + aggregated.size()
                + " point(s) of up to " + aggregationWindow + " sessions each to reduce estimator noise); see config for noise-parameter rationale.";
        return BmsTrendResult.of(direction, clampedSoh, slopePerDay, uncertainty, points.size(), note);
    }

    /** Groups consecutive points into buckets of at most {@code windowSize}, averaging SoH,
     * confidence, and elapsed time within each bucket to reduce per-session estimator noise before
     * the Kalman filter runs. A discontinuity always starts a new bucket -- it is never averaged
     * together with the points before it, since that would smear a real reset event into what would
     * look like a gradual (and wrong) change. */
    private static List<BmsTrendPoint> aggregate(List<BmsTrendPoint> points, int windowSize) {
        double[] absoluteHours = new double[points.size()];
        double cumulative = 0;
        for (int i = 0; i < points.size(); i++) { cumulative += points.get(i).hoursSincePrevious; absoluteHours[i] = cumulative; }

        List<BmsTrendPoint> aggregated = new ArrayList<>();
        Double previousBucketHours = null;
        int i = 0;
        while (i < points.size()) {
            int end = Math.min(i + windowSize, points.size());
            for (int j = i + 1; j < end; j++) {
                if (points.get(j).discontinuityBeforePoint) { end = j; break; }
            }
            double sumSoh = 0, sumConfidence = 0, sumHours = 0;
            int n = end - i;
            for (int j = i; j < end; j++) {
                sumSoh += points.get(j).sohPct;
                sumConfidence += points.get(j).confidence;
                sumHours += absoluteHours[j];
            }
            double bucketHours = sumHours / n;
            double hoursSincePrevious = previousBucketHours == null ? 0.0 : bucketHours - previousBucketHours;
            aggregated.add(new BmsTrendPoint(points.get(i).sessionId + (n > 1 ? " (+" + (n - 1) + " more)" : ""),
                sumSoh / n, hoursSincePrevious, points.get(i).discontinuityBeforePoint, sumConfidence / n));
            previousBucketHours = bucketHours;
            i = end;
        }
        return aggregated;
    }
}
