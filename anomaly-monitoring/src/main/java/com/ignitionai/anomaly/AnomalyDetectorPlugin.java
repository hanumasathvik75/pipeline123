package com.ignitionai.anomaly;

import com.ignitionai.phase4.AnomalyState;
import com.ignitionai.phase4.Phase4Observation;

public interface AnomalyDetectorPlugin {
    String getDetectorId();
    String getDetectorVersion();
    
    /**
     * Evaluates the observation and updates the tracker state.
     * Returns the determined anomaly score (0.0 to 1.0).
     */
    Double evaluate(Phase4Observation observation, SignalAnomalyTracker tracker);
}
