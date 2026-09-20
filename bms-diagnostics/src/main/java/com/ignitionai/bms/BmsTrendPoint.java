package com.ignitionai.bms;

/** One session's contribution to the cross-session SoH trend. Caller is responsible for ordering
 * these correctly and for setting discontinuityBeforePoint honestly -- this class does not infer
 * ordering or discontinuities itself, since both require external knowledge (a trustworthy
 * timestamp field, or a known real-world event like a pack swap) this class cannot see. */
public final class BmsTrendPoint {
    public final String sessionId;
    public final double sohPct;
    public final double hoursSincePrevious;
    /** True when this session follows a known break in continuity (e.g. a pack swap or BMS
     * recalibration) -- the trend model resets its uncertainty here instead of fitting a slope
     * across an event that was never a gradual physical change. */
    public final boolean discontinuityBeforePoint;
    public final double confidence;

    public BmsTrendPoint(String sessionId, double sohPct, double hoursSincePrevious, boolean discontinuityBeforePoint, double confidence) {
        this.sessionId = sessionId;
        this.sohPct = sohPct;
        this.hoursSincePrevious = hoursSincePrevious;
        this.discontinuityBeforePoint = discontinuityBeforePoint;
        this.confidence = confidence;
    }
}
