package com.ignitionai.bms;

import com.ignitionai.phase5.DegradationOutput.TrendDirection;

/** Cross-session Pack SoH trend, or an honest INSUFFICIENT_HISTORY/UNKNOWN when there isn't
 * enough continuous, trustworthy history to say anything -- never a fabricated confident trend. */
public final class BmsTrendResult {
    public final TrendDirection direction;
    public final Double estimatedCurrentSohPct;
    public final Double slopePctPerDay;
    public final Double uncertainty;
    public final int pointsUsed;
    public final String note;

    private BmsTrendResult(TrendDirection direction, Double estimatedCurrentSohPct, Double slopePctPerDay,
            Double uncertainty, int pointsUsed, String note) {
        this.direction = direction;
        this.estimatedCurrentSohPct = estimatedCurrentSohPct;
        this.slopePctPerDay = slopePctPerDay;
        this.uncertainty = uncertainty;
        this.pointsUsed = pointsUsed;
        this.note = note;
    }

    public static BmsTrendResult insufficientHistory(String note, int pointsUsed) {
        return new BmsTrendResult(TrendDirection.UNKNOWN, null, null, null, pointsUsed, note);
    }

    public static BmsTrendResult of(TrendDirection direction, double sohPct, double slopePctPerDay, double uncertainty, int pointsUsed, String note) {
        return new BmsTrendResult(direction, sohPct, slopePctPerDay, uncertainty, pointsUsed, note);
    }
}
