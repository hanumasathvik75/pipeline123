package com.ignitionai.episode;

import com.ignitionai.phase4.AnomalyState;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EpisodeStore {
    // In-memory append-only log for MVP
    private final List<EpisodeRecord> log = new ArrayList<>();
    // Fast lookup for active episodes by vehicle+signal
    private final Map<String, EpisodeRecord> activeEpisodes = new HashMap<>();

    public EpisodeRecord getOrCreateActiveEpisode(String vehicleId, String sessionId, String signalId, Long startMs, Double score, AnomalyState state) {
        String key = vehicleId + "_" + sessionId + "_" + signalId;
        if (activeEpisodes.containsKey(key)) {
            return activeEpisodes.get(key);
        }
        EpisodeRecord newEpisode = new EpisodeRecord(vehicleId, sessionId, signalId, startMs, score, state);
        log.add(newEpisode);
        activeEpisodes.put(key, newEpisode);
        return newEpisode;
    }

    public void updateEpisode(EpisodeRecord episode, Double score, AnomalyState state, Long currentMs) {
        episode.update(score, state, currentMs);
        if (state == AnomalyState.CLOSURE || state == AnomalyState.NOMINAL) {
            activeEpisodes.remove(episode.getVehicleId() + "_" + episode.getSessionId() + "_" + episode.getSignalId());
        }
    }
    
    public List<EpisodeRecord> getAllEpisodes() {
        return new ArrayList<>(log);
    }
}
