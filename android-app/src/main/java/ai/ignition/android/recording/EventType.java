package ai.ignition.android.recording;

/**
 * Supported event types for the local JSONL session recorder.
 */
public enum EventType {
    SESSION_STARTED,
    SESSION_STOPPED,
    OBD_OBSERVATION,
    PHASE_CHANGED,
    CONNECTION_LOST,
    CONNECTION_RESTORED,
    CAPTURE_ERROR,
    SESSION_FINALIZED
}
