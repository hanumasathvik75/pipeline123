package com.ignitionai.obdgenerator.config;

import java.util.*;

/** Piecewise-linear signal trajectory. Times are session-relative milliseconds. */
public final class SignalProgram {
    private final String unit;
    private final NavigableMap<Long, Double> knots;

    public SignalProgram(String unit, Map<Long, Double> knots) {
        if (unit == null || unit.isBlank() || knots == null || knots.isEmpty())
            throw new IllegalArgumentException("A signal needs a unit and at least one time:value point");
        this.unit = unit;
        this.knots = new TreeMap<>(knots);
        for (Map.Entry<Long, Double> e : this.knots.entrySet())
            if (e.getKey() < 0 || e.getValue() == null || !Double.isFinite(e.getValue()))
                throw new IllegalArgumentException("Signal times must be nonnegative and values finite");
    }

    public String getUnit() { return unit; }
    public double valueAt(long timeMs) {
        Map.Entry<Long, Double> lo = knots.floorEntry(timeMs), hi = knots.ceilingEntry(timeMs);
        if (lo == null) return knots.firstEntry().getValue();
        if (hi == null || hi.getKey().equals(lo.getKey())) return lo.getValue();
        double fraction = (double)(timeMs - lo.getKey()) / (hi.getKey() - lo.getKey());
        return lo.getValue() + fraction * (hi.getValue() - lo.getValue());
    }
}
