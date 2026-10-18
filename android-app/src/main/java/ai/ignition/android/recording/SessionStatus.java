package ai.ignition.android.recording;

/**
 * Lifecycle and completion status of a diagnostic recording session.
 */
public enum SessionStatus {
    RECORDING,
    COMPLETE,
    PARTIAL,
    INTERRUPTED,
    INVALID
}
