package com.ignitionai.application;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Reconstructs version-current episodes from durable normalized sessions, independent of file order. */
public final class AssessmentHistory {
    private final File sensors;
    private final Path sessions;
    public AssessmentHistory(File sensors, Path sessions) { this.sensors = sensors; this.sessions = sessions; }

    public InspectionResult assess(List<ObdMessage> input) throws Exception {
        if (input.isEmpty()) throw new IllegalArgumentException("No OBD records to assess");
        InspectionService service = new InspectionService(sensors);
        List<InspectionResult> prior = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long start = firstTime(input);
        long historyBytes = 0;
        if (Files.isDirectory(sessions)) {
            List<Path> files;
            try (var paths = Files.list(sessions)) {
                files = paths.filter(p -> p.getFileName().toString().endsWith(".jsonl") && !p.getFileName().toString().endsWith(".raw.jsonl"))
                    .sorted().collect(java.util.stream.Collectors.toList());
            }
            // Fail visibly rather than silently dropping history; prevents unbounded device work.
            if (files.size() > 500) throw new IOException("History exceeds 500 sessions; archive older sessions before assessment");
            for (Path p : files) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Assessment cancelled");
                try {
                    historyBytes += Files.size(p);
                    if (historyBytes > 16L * 1024 * 1024) throw new IllegalStateException("History exceeds the 16 MB analysis budget; archive older sessions first");
                    List<ObdMessage> records = SessionFiles.read(p);
                    if (eligible(input, records, start)) prior.add(service.assess(records));
                } catch (IOException | IllegalArgumentException e) {
                    warnings.add("History file excluded (unreadable or invalid): " + p.getFileName());
                }
            }
        }
        InspectionResult result = service.assess(input, prior);
        if (warnings.isEmpty()) return result;
        List<String> notes = new ArrayList<>(result.notes); notes.addAll(warnings);
        notes.add("Historical evidence is incomplete; excluded files must be repaired or re-imported.");
        return new InspectionResult(result.messages, result.episodes, result.degradation, result.observations, notes, result.certificate);
    }
    static boolean eligible(List<ObdMessage> current, List<ObdMessage> candidate, long start) {
        if (candidate.isEmpty()) return false;
        ObdMessage a = current.get(0), b = candidate.get(0);
        long end = candidate.stream().mapToLong(m -> m.getReceivedAt().toEpochMilli()).max().orElseThrow();
        return a.getVehicleRef().getVehicleId().equals(b.getVehicleRef().getVehicleId())
            && a.getSourceType() == b.getSourceType() && !a.getSessionId().equals(b.getSessionId())
            && end < start && end >= start - 365L * 24 * 3600000;
    }
    static long firstTime(List<ObdMessage> records) {
        return records.stream().mapToLong(m -> m.getReceivedAt().toEpochMilli()).min().orElseThrow();
    }
    static long epochAtZero(List<ObdMessage> records) {
        ObdMessage first = records.stream().filter(m -> !m.getSensorReadings().isEmpty()).findFirst().orElse(records.get(0));
        long elapsed = first.getSensorReadings().stream().mapToLong(r -> r.getMonotonicMs()).min().orElse(0);
        return Math.subtractExact(first.getReceivedAt().toEpochMilli(), elapsed);
    }
}
