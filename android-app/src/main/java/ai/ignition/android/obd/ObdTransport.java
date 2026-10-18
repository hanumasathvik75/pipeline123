package ai.ignition.android.obd;

import java.io.IOException;
import java.time.Duration;

/**
 * Common abstraction for communicating with an ELM327 OBD-II adapter over
 * Bluetooth Classic SPP or Bluetooth Low Energy (BLE GATT).
 */
public interface ObdTransport extends AutoCloseable {

    /**
     * Connects to the underlying physical adapter within the specified timeout.
     */
    void connect(Duration timeout) throws IOException;

    /**
     * Sends a command (with appropriate carriage return framing) and blocks until
     * the prompt boundary ('>') is reached or the timeout expires.
     *
     * @param command Raw command, e.g. "ATZ" or "0100"
     * @param timeout Maximum duration to wait for response
     * @return Trimmed response string excluding the prompt character
     * @throws IOException If disconnected, timed out, or communication fails
     */
    String exchange(String command, Duration timeout) throws IOException;

    /**
     * Checks if the transport connection is currently open and healthy.
     */
    boolean isConnected();

    /**
     * Returns the verified transport type.
     */
    TransportType getTransportType();

    /**
     * Returns a human-readable description of the device (name and redacted address).
     */
    String getDeviceDescription();

    @Override
    void close();
}
