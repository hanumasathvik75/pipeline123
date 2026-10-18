package ai.ignition.android.recording;

/**
 * Status of an individual sensor observation or OBD parameter.
 * Used to explicitly represent unavailable or degraded data without substituting zero.
 */
public enum ObservationStatus {
    VALID,
    UNSUPPORTED,
    NO_DATA,
    ERROR
}
