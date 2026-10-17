package ai.ignition.android.recording;

import ai.ignition.android.obd.ObdTransport;

import java.io.IOException;

/**
 * Factory for creating or recreating an ObdTransport connection to an ELM327 adapter.
 * Used by DriveCaptureEngine for initial connection and bounded reconnection attempts.
 */
@FunctionalInterface
public interface TransportFactory {

    /**
     * Creates a new, unconnected ObdTransport instance.
     *
     * @return Fresh ObdTransport instance.
     * @throws IOException If device discovery or socket creation fails.
     */
    ObdTransport createTransport() throws IOException;
}
