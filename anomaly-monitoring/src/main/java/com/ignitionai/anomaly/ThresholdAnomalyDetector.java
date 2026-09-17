package com.ignitionai.anomaly;

import com.ignitionai.phase4.AnomalyState;
import com.ignitionai.phase4.Phase4Observation;

public class ThresholdAnomalyDetector implements AnomalyDetectorPlugin {
    
    private final double threshold;
    
    public ThresholdAnomalyDetector(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getDetectorId() {
        return "threshold_detector";
    }

    @Override
    public String getDetectorVersion() {
        return "1.0";
    }

    @Override
    public Double evaluate(Phase4Observation observation, SignalAnomalyTracker tracker) {
        Double normRes = observation.getNormalizedResidual();
        if (normRes == null) {
            tracker.incrementMissing();
            return tracker.getLastScore();
        }
        
        tracker.resetMissing();
        
        double score = Math.abs(normRes) > threshold ? 1.0 : 0.0;
        tracker.setLastScore(score);
        
        if (score >= 1.0) {
            tracker.setCurrentState(AnomalyState.ANOMALY_ACTIVE);
            tracker.setLastViolationMs(observation.getTimestampMs());
        } else {
            tracker.setCurrentState(AnomalyState.NOMINAL);
            tracker.setLastNominalMs(observation.getTimestampMs());
        }
        
        return score;
    }
}
