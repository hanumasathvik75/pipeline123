package ai.ignition.android.obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import ai.ignition.android.util.RedactionUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bluetooth Low Energy (BLE GATT) transport for OBD-II adapters.
 * Supports standard Nordic UART (NUS), Vgate/TI (FFF0), and generic HM-10 (FFE0) profiles.
 */
@SuppressLint("MissingPermission")
public class BleGattTransport implements ObdTransport {

    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    // Known BLE OBD-II UART Services and Characteristics
    private static final UUID NUS_SERVICE = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_RX_CHAR = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"); // Phone writes to adapter
    private static final UUID NUS_TX_CHAR = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // Adapter notifies phone

    private static final UUID FFF0_SERVICE = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID FFF1_NOTIFY  = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB");
    private static final UUID FFF2_WRITE   = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB");

    private static final UUID FFE0_SERVICE = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID FFE1_CHAR    = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothDevice device;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeChar;
    private BluetoothGattCharacteristic notifyChar;

    private final Object exchangeLock = new Object();
    private final StringBuilder responseBuffer = new StringBuilder();
    private volatile boolean promptReceived = false;
    private volatile boolean connected = false;
    private volatile String connectionError = null;

    public BleGattTransport(Context context, BluetoothDevice device) {
        this.context = context;
        this.device = device;
    }

    @Override
    public synchronized void connect(Duration timeout) throws IOException {
        if (connected) return;

        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch serviceLatch = new CountDownLatch(1);
        CountDownLatch notifyLatch = new CountDownLatch(1);
        connectionError = null;

        BluetoothGattCallback callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    connected = true;
                    connectLatch.countDown();
                    g.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connected = false;
                    connectionError = "GATT disconnected (status=" + status + ")";
                    connectLatch.countDown();
                    serviceLatch.countDown();
                    notifyLatch.countDown();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt g, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    resolveCharacteristics(g);
                    serviceLatch.countDown();
                } else {
                    connectionError = "Service discovery failed with status " + status;
                    serviceLatch.countDown();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    synchronized (exchangeLock) {
                        for (byte b : data) {
                            char c = (char) b;
                            if (c == '>') {
                                promptReceived = true;
                                exchangeLock.notifyAll();
                                break;
                            }
                            if (c == '\r') {
                                if (responseBuffer.length() > 0 && responseBuffer.charAt(responseBuffer.length() - 1) != '\n') {
                                    responseBuffer.append('\n');
                                }
                            } else if (c == '\n') {
                                if (responseBuffer.length() > 0 && responseBuffer.charAt(responseBuffer.length() - 1) != '\n') {
                                    responseBuffer.append('\n');
                                }
                            } else {
                                responseBuffer.append(c);
                            }
                        }
                    }
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
                notifyLatch.countDown();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
        } else {
            gatt = device.connectGatt(context, false, callback);
        }

        try {
            if (!connectLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS) || !connected) {
                close();
                throw new IOException("BLE connection timed out: " + (connectionError != null ? connectionError : "no response"));
            }

            if (!serviceLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS) || writeChar == null || notifyChar == null) {
                close();
                throw new IOException("Failed to discover compatible OBD-II BLE characteristics on " + getDeviceDescription());
            }

            // Enable notifications on notifyChar
            gatt.setCharacteristicNotification(notifyChar, true);
            BluetoothGattDescriptor descriptor = notifyChar.getDescriptor(CCCD_UUID);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                notifyLatch.await(2000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            throw new IOException("Connection sequence interrupted", e);
        }
    }

    private void resolveCharacteristics(BluetoothGatt g) {
        // 1. Try Nordic UART Service
        BluetoothGattService nus = g.getService(NUS_SERVICE);
        if (nus != null) {
            writeChar = nus.getCharacteristic(NUS_RX_CHAR);
            notifyChar = nus.getCharacteristic(NUS_TX_CHAR);
            if (writeChar != null && notifyChar != null) return;
        }

        // 2. Try FFF0 (Vgate / TI)
        BluetoothGattService fff0 = g.getService(FFF0_SERVICE);
        if (fff0 != null) {
            notifyChar = fff0.getCharacteristic(FFF1_NOTIFY);
            writeChar = fff0.getCharacteristic(FFF2_WRITE);
            if (writeChar != null && notifyChar != null) return;
        }

        // 3. Try FFE0 (HM-10)
        BluetoothGattService ffe0 = g.getService(FFE0_SERVICE);
        if (ffe0 != null) {
            BluetoothGattCharacteristic c = ffe0.getCharacteristic(FFE1_CHAR);
            if (c != null) {
                writeChar = c;
                notifyChar = c;
            }
        }
    }

    @Override
    public String exchange(String command, Duration timeout) throws IOException {
        if (!connected || gatt == null || writeChar == null) {
            throw new IOException("BLE GATT transport is not connected");
        }

        String trimmedUpper = command.trim().toUpperCase();
        if (trimmedUpper.startsWith("04") || (trimmedUpper.startsWith("4") && trimmedUpper.length() == 1)) {
            throw new IllegalArgumentException("Safety violation: Mode 04 code clearing commands are strictly prohibited");
        }
        if (trimmedUpper.startsWith("08")) {
            throw new IllegalArgumentException("Safety violation: Mode 08 actuator/control commands are strictly prohibited");
        }

        synchronized (exchangeLock) {
            responseBuffer.setLength(0);
            promptReceived = false;

            String payload = command.endsWith("\r") ? command : command + "\r";
            writeChar.setValue(payload.getBytes());
            writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            boolean written = gatt.writeCharacteristic(writeChar);
            if (!written) {
                throw new IOException("Failed to send GATT write for command: " + command);
            }

            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (!promptReceived && System.currentTimeMillis() < deadline) {
                long waitTime = deadline - System.currentTimeMillis();
                if (waitTime <= 0) break;
                try {
                    exchangeLock.wait(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Exchange interrupted while awaiting BLE notification", e);
                }
            }

            if (!promptReceived) {
                throw new IOException("Timed out waiting for BLE response to '" + command + "'. Buffer: " + responseBuffer);
            }

            return responseBuffer.toString().trim();
        }
    }

    @Override
    public boolean isConnected() {
        return connected && gatt != null;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.BLE_GATT_CONFIRMED;
    }

    @Override
    public String getDeviceDescription() {
        if (device == null) return "Unknown BLE Device";
        String name = device.getName() != null ? device.getName() : "Unnamed BLE";
        return name + " (" + RedactionUtil.redactMac(device.getAddress()) + ")";
    }

    @Override
    public synchronized void close() {
        connected = false;
        if (gatt != null) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (Exception ignored) {}
            gatt = null;
        }
    }
}
