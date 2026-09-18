package com.ignitionai.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WindowBuffer {
    private final Map<String, List<SensorReading>> buffer = new HashMap<>();

    public void addReading(SensorReading reading) {
        buffer.computeIfAbsent(com.ignitionai.obd.SignalIds.canonical(reading.getSignalId()), k -> new ArrayList<>()).add(reading);
    }

    public List<SensorReading> getReadings(String signalId, Long currentTimestampMs, Long windowSizeMs) {
        signalId = com.ignitionai.obd.SignalIds.canonical(signalId);
        if (!buffer.containsKey(signalId)) return new ArrayList<>();
        
        Long cutoff = currentTimestampMs - windowSizeMs;
        
        return buffer.get(signalId).stream()
            .filter(r -> r.getTimestampMs() >= cutoff && r.getTimestampMs() <= currentTimestampMs)
            .collect(Collectors.toList());
    }
    
    public void cleanup(Long currentTimestampMs, Long maxRetentionMs) {
        Long cutoff = currentTimestampMs - maxRetentionMs;
        for (List<SensorReading> readings : buffer.values()) {
            readings.removeIf(r -> r.getTimestampMs() < cutoff);
        }
    }
}
