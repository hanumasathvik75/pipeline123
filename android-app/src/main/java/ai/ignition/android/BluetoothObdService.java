package ai.ignition.android;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Android background service managing physical RFCOMM Bluetooth SPP connection
 * to an ELM327 OBD-II adapter.
 */
public class BluetoothObdService extends Service {

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
        if (deviceAddress != null) {
            connectToDevice(deviceAddress);
        }
        return START_STICKY;
    }

    private void connectToDevice(String address) {
        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                isRunning = true;

                initElm327();
                pollTelemetryLoop();
            } catch (Exception e) {
                stopSelf();
            }
        }).start();
    }

    private void initElm327() throws Exception {
        sendCommand("AT Z\r");      // Reset
        Thread.sleep(1000);
        sendCommand("AT E0\r");     // Echo off
        sendCommand("AT L0\r");     // Linefeeds off
        sendCommand("AT SP 0\r");    // Auto protocol
    }

    private void pollTelemetryLoop() {
        while (isRunning) {
            try {
                sendCommand("010C\r"); // RPM
                Thread.sleep(150);
                sendCommand("010D\r"); // Speed
                Thread.sleep(150);
                sendCommand("0105\r"); // Coolant
                Thread.sleep(200);
            } catch (Exception e) {
                break;
            }
        }
    }

    private void sendCommand(String cmd) throws Exception {
        if (outputStream != null) {
            outputStream.write(cmd.getBytes());
            outputStream.flush();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
