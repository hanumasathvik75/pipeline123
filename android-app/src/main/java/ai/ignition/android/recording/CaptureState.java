package ai.ignition.android.recording;

/**
 * Lifecycle states of the live drive capture engine.
 */
public enum CaptureState {
    IDLE,
    INITIALIZING,
    RECORDING,
    RECONNECTING,
    STOPPING,
    STOPPED,
    ERROR
}
