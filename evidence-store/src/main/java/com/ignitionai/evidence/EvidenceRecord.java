package com.ignitionai.evidence;

import com.ignitionai.phase4.Phase4Observation;
import java.util.UUID;

public class EvidenceRecord {
    private final String evidenceId;
    private final String episodeId;
    private final Long timestampMs;
    private final Phase4Observation observation;
    
    public EvidenceRecord(String episodeId, Long timestampMs, Phase4Observation observation) {
        this.evidenceId = UUID.randomUUID().toString();
        this.episodeId = episodeId;
        this.timestampMs = timestampMs;
        this.observation = observation;
    }

    public String getEvidenceId() { return evidenceId; }
    public String getEpisodeId() { return episodeId; }
    public Long getTimestampMs() { return timestampMs; }
    public Phase4Observation getObservation() { return observation; }
}
