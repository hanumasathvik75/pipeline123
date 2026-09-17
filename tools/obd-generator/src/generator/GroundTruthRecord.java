package com.ignitionai.obdgenerator.generator;

import java.util.List;
import java.util.Map;

public class GroundTruthRecord {
    private final long monotonicMs;
    private final Map<String, Double> trueValues;
    private final List<String> activeFaults;

    public GroundTruthRecord(long monotonicMs, Map<String, Double> trueValues, List<String> activeFaults) {
        this.monotonicMs = monotonicMs;
        this.trueValues = trueValues;
        this.activeFaults = activeFaults;
    }

    public long getMonotonicMs() { return monotonicMs; }
    public Map<String, Double> getTrueValues() { return trueValues; }
    public List<String> getActiveFaults() { return activeFaults; }
}
