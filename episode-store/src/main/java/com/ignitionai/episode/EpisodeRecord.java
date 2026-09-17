package com.ignitionai.episode;

import com.ignitionai.phase4.AnomalyState;

public class EpisodeRecord {
    private final String episodeId;
    private final String vehicleId;
    private final String sessionId;
    private final String signalId;
    private final Long startTimestampMs;
    private Long endTimestampMs;
    private Double maxAnomalyScore;
    private AnomalyState currentState;
    
    public EpisodeRecord(String vehicleId, String sessionId, String signalId, Long startTimestampMs, Double initialScore, AnomalyState initialState) {
        this.episodeId = vehicleId + "-" + sessionId + "-" + signalId + "-" + startTimestampMs;
        this.vehicleId = vehicleId;
        this.sessionId = sessionId;
        this.signalId = signalId;
        this.startTimestampMs = startTimestampMs;
        this.maxAnomalyScore = initialScore;
        this.currentState = initialState;
    }

    public String getEpisodeId() { return episodeId; }
    public String getVehicleId() { return vehicleId; }
    public String getSessionId() { return sessionId; }
    public String getSignalId() { return signalId; }
    public Long getStartTimestampMs() { return startTimestampMs; }
    public Long getEndTimestampMs() { return endTimestampMs; }
    public Double getMaxAnomalyScore() { return maxAnomalyScore; }
    public AnomalyState getCurrentState() { return currentState; }

    public void update(Double score, AnomalyState newState, Long currentMs) {
        if (score != null && score > this.maxAnomalyScore) {
            this.maxAnomalyScore = score;
        }
        this.currentState = newState;
        if (newState == AnomalyState.CLOSURE || newState == AnomalyState.NOMINAL) {
            this.endTimestampMs = currentMs;
        }
    }
}
