package com.ignitionai.degradation.store;

import com.ignitionai.phase4.AnomalyEpisode;
import java.util.*;

/** Copies session-relative episodes onto one phone-receive UTC timeline. Never mutates evidence. */
public final class EpisodeTimeline {
    private EpisodeTimeline() { }
    public static AnomalyEpisode atEpoch(AnomalyEpisode e, long epochAtSessionZero) {
        return new AnomalyEpisode(e.getVehicleId(), e.getSessionId(), e.getSubsystemId(), e.getEpisodeId(),
            Math.addExact(epochAtSessionZero, e.getStartTimeMs()), Math.addExact(epochAtSessionZero, e.getEndTimeMs()),
            e.getSeverity(), e.getDurationMs(), e.getRecurrenceCount(), e.getPersistence(), e.getOperatingRegime(),
            e.getQualityIndicators() == null ? Map.of() : Map.copyOf(e.getQualityIndicators()),
            List.copyOf(e.getEvidenceReferences()), e.getContextVersion(), e.getModelVersion(),
            e.getCalibrationVersion(), e.getMaintenanceAction());
    }
    public static List<AnomalyEpisode> before(AnomalyEpisode current, List<AnomalyEpisode> candidates) {
        // Timeline matching is strict: same vehicle, subsystem, model/calibration and earlier end time.
        Map<String, AnomalyEpisode> unique = new TreeMap<>();
        for (AnomalyEpisode e : candidates) {
            if (e.getVehicleId().equals(current.getVehicleId()) && e.getSubsystemId().equals(current.getSubsystemId())
                && Objects.equals(e.getModelVersion(), current.getModelVersion())
                && Objects.equals(e.getCalibrationVersion(), current.getCalibrationVersion())
                && !e.getEpisodeId().equals(current.getEpisodeId()) && e.getEndTimeMs() < current.getStartTimeMs())
                unique.putIfAbsent(e.getEpisodeId(), e);
        }
        List<AnomalyEpisode> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(AnomalyEpisode::getStartTimeMs).thenComparing(AnomalyEpisode::getEpisodeId));
        return result;
    }
}
