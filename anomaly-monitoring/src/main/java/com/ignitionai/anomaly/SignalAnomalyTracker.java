package com.ignitionai.anomaly;

import com.ignitionai.phase4.AnomalyState;

public class SignalAnomalyTracker {
    private final String signalId;
    private final String sessionId;
    private AnomalyState currentState = AnomalyState.NOMINAL;
    
    private Long lastNominalMs = 0L;
    private Long firstViolationMs = 0L;
    private Long lastViolationMs = 0L;
    private Long firstRecoveryMs = 0L;
    
    private Double lastScore = 0.0;
    private int consecutiveMissing = 0;

    public SignalAnomalyTracker(String signalId, String sessionId) {
        this.signalId = signalId;
        this.sessionId = sessionId;
    }

    public String getSignalId() { return signalId; }
    public String getSessionId() { return sessionId; }
    
    public AnomalyState getCurrentState() { return currentState; }
    public void setCurrentState(AnomalyState state) { this.currentState = state; }
    
    public Long getLastNominalMs() { return lastNominalMs; }
    public void setLastNominalMs(Long ms) { this.lastNominalMs = ms; }
    
    public Long getFirstViolationMs() { return firstViolationMs; }
    public void setFirstViolationMs(Long ms) { this.firstViolationMs = ms; }
    
    public Long getLastViolationMs() { return lastViolationMs; }
    public void setLastViolationMs(Long ms) { this.lastViolationMs = ms; }
    
    public Long getFirstRecoveryMs() { return firstRecoveryMs; }
    public void setFirstRecoveryMs(Long ms) { this.firstRecoveryMs = ms; }
    
    public Double getLastScore() { return lastScore; }
    public void setLastScore(Double score) { this.lastScore = score; }
    
    public int getConsecutiveMissing() { return consecutiveMissing; }
    public void incrementMissing() { this.consecutiveMissing++; }
    public void resetMissing() { this.consecutiveMissing = 0; }
}
