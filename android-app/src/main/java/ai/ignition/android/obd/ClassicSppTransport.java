package ai.ignition.android.obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import ai.ignition.android.util.RedactionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Physical Bluetooth Classic RFCOMM (Serial Port Profile) transport for ELM327 adapters.
 * Implements bounded blocking reads with overall command deadline and prompt framing.
 */
public class ClassicSppTransport implements ObdTransport {

    public static final UUID STANDARD_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MAX_RESPONSE_BYTES = 16384;

    private final BluetoothDevice device;
    private final UUID serviceUuid;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final String mockDescription;

    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean connected = false;

    public ClassicSppTransport(BluetoothDevice device) {
        this(device, STANDARD_SPP_UUID);
    }

    public ClassicSppTransport(BluetoothDevice device, UUID serviceUuid) {
        this.device = device;
        this.serviceUuid = serviceUuid != null ? serviceUuid : STANDARD_SPP_UUID;
        this.mockDescription = null;
    }

    /**
     * Package-private constructor for unit testing with test doubles.
     */
    ClassicSppTransport(InputStream in, OutputStream out, String description) {
        this.device = null;
        this.serviceUuid = STANDARD_SPP_UUID;
        this.inputStream = in;
        this.outputStream = out;
        this.connected = true;
        this.mockDescription = description;
    }

    @Override
    @SuppressLint("MissingPermission")
    public synchronized void connect(Duration timeout) throws IOException {
        if (connected) return;
        if (device == null) {
            throw new IOException("Cannot connect: BluetoothDevice is null");
        }

        Future<?> connectFuture = ioExecutor.submit(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(serviceUuid);
                socket.connect();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                connected = true;
            } catch (Exception e) {
                close();
                throw new IOException("Failed to establish Classic SPP connection to " + getDeviceDescription() + ": " + e.getMessage(), e);
            }
            return null;
        });

        try {
            connectFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            connectFuture.cancel(true);
            close();
            throw new IOException("Connection timed out after " + timeout.toMillis() + "ms for " + getDeviceDescription(), te);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException("Connection error: " + (cause != null ? cause.getMessage() : ee.getMessage()), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            close();
            throw new IOException("Connection interrupted", ie);
        }
    }

    @Override
    public synchronized String exchange(String command, Duration timeout) throws IOException {
        if (!connected || outputStream == null || inputStream == null) {
            throw new IOException("Transport is not connected");
        }

        // Safety guardrails: explicitly prohibit Mode 04 (clear codes) and Mode 08 (actuator control)
        String trimmedUpper = command.trim().toUpperCase();
        if (trimmedUpper.startsWith("04") || (trimmedUpper.startsWith("4") && trimmedUpper.length() == 1)) {
            throw new IllegalArgumentException("Safety violation: Mode 04 code clearing commands are strictly prohibited");
        }
        if (trimmedUpper.startsWith("08")) {
            throw new IllegalArgumentException("Safety violation: Mode 08 actuator/control commands are strictly prohibited");
        }

        Future<String> exchangeFuture = ioExecutor.submit(() -> {
            // Delimit and send command
            String payload = command.endsWith("\r") ? command : command + "\r";
            outputStream.write(payload.getBytes());
            outputStream.flush();

            StringBuilder sb = new StringBuilder();
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            boolean promptFound = false;

            byte[] chunk = new byte[512];

            // Bounded blocking read loop with deadline enforcement
            while (System.currentTimeMillis() < deadline) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Command exchange cancelled");
                }

                int count = inputStream.read(chunk);
                if (count == -1) {
                    throw new IOException("Unexpected end of stream: ELM327 adapter disconnected");
                }

                for (int i = 0; i < count; i++) {
                    char c = (char) (chunk[i] & 0xFF);
                    if (c == '>') {
                        promptFound = true;
                        break;
                    }
                    if (sb.length() >= MAX_RESPONSE_BYTES) {
                        throw new IOException("Response exceeded maximum buffer limit (" + MAX_RESPONSE_BYTES + " bytes)");
                    }
                    if (c == '\r') {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                            sb.append('\n');
                        }
                    } else if (c == '\n') {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                            sb.append('\n');
                        }
                    } else {
                        sb.append(c);
                    }
                }

                if (promptFound) break;
            }

            if (!promptFound) {
                throw new IOException("Timed out waiting for ELM prompt '>' after " + timeout.toMillis() + "ms. Partial buffer: " + sb);
            }

            return sb.toString().trim();
        });

        try {
            return exchangeFuture.get(timeout.toMillis() + 500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            exchangeFuture.cancel(true);
            throw new IOException("Command '" + command + "' timed out after " + timeout.toMillis() + "ms", te);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException("Command execution failed: " + (cause != null ? cause.getMessage() : ee.getMessage()), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Exchange interrupted", ie);
        }
    }

    @Override
    public boolean isConnected() {
        return connected && (socket == null || socket.isConnected());
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.CLASSIC_SPP_CONFIRMED;
    }

    @Override
    @SuppressLint("MissingPermission")
    public String getDeviceDescription() {
        if (mockDescription != null) return mockDescription;
        if (device == null) return "Unknown Device";
        String name = device.getName() != null ? device.getName() : "Unnamed";
        return name + " (" + RedactionUtil.redactMac(device.getAddress()) + ")";
    }

    @Override
    public synchronized void close() {
        connected = false;
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (Exception ignored) {}
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (Exception ignored) {}
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception ignored) {}
        ioExecutor.shutdownNow();
    }
}
