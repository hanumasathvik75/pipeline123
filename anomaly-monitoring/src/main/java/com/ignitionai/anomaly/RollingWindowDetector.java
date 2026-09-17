package com.ignitionai.anomaly;

import com.ignitionai.phase4.AnomalyState;
import com.ignitionai.phase4.Phase4Observation;

public class RollingWindowDetector implements AnomalyDetectorPlugin {
    
    private final double threshold;
    private final long persistenceMs;
    private final long recoveryMs;

    public RollingWindowDetector(double threshold, long persistenceMs, long recoveryMs) {
        this.threshold = threshold;
        this.persistenceMs = persistenceMs;
        this.recoveryMs = recoveryMs;
    }

    @Override
    public String getDetectorId() {
        return "rolling_window_detector";
    }

    @Override
    public String getDetectorVersion() {
        return "1.0";
    }

    @Override
    public Double evaluate(Phase4Observation observation, SignalAnomalyTracker tracker) {
        Double normRes = observation.getNormalizedResidual();
        long currentMs = observation.getTimestampMs();
        
        if (normRes == null) {
            tracker.incrementMissing();
            // Stale data fallback - if too much missing data during active anomaly, it might escalate or close.
            return tracker.getLastScore();
        }
        
        tracker.resetMissing();
        boolean isViolating = Math.abs(normRes) > threshold;
        double currentScore = isViolating ? Math.min(1.0, Math.abs(normRes) / (threshold * 2)) : 0.0; // scale score
        
        AnomalyState state = tracker.getCurrentState();
        
        if (isViolating) {
            tracker.setFirstRecoveryMs(0L);
            tracker.setLastViolationMs(currentMs);
            
            if (state == AnomalyState.NOMINAL || state == AnomalyState.CLOSURE) {
                tracker.setFirstViolationMs(currentMs);
                state = AnomalyState.ANOMALY_ONSET;
            } else if (state == AnomalyState.ANOMALY_ONSET) {
                if (currentMs - tracker.getFirstViolationMs() >= persistenceMs) {
                    state = AnomalyState.ANOMALY_ACTIVE;
                }
            } else if (state == AnomalyState.RECOVERY_ONSET) {
                state = AnomalyState.ANOMALY_ACTIVE; // Relapsed
            }
        } else {
            tracker.setLastNominalMs(currentMs);
            
            if (state == AnomalyState.ANOMALY_ACTIVE || state == AnomalyState.ESCALATION) {
                tracker.setFirstRecoveryMs(currentMs);
                state = AnomalyState.RECOVERY_ONSET;
            } else if (state == AnomalyState.RECOVERY_ONSET) {
                if (currentMs - tracker.getFirstRecoveryMs() >= recoveryMs) {
                    state = AnomalyState.CLOSURE;
                }
            } else if (state == AnomalyState.CLOSURE || state == AnomalyState.ANOMALY_ONSET) {
                state = AnomalyState.NOMINAL;
                tracker.setFirstViolationMs(0L);
            }
        }
        
        tracker.setCurrentState(state);
        tracker.setLastScore(currentScore);
        
        return currentScore;
    }
}
