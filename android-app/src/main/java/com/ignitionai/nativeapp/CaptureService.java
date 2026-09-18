package com.ignitionai.nativeapp;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import com.ignitionai.obdinput.adapter.*;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.storage.ObdJsonMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Foreground, cancellable Classic Bluetooth SPP capture. No background web service. */
public final class CaptureService extends Service {
    public static final String STATUS = "com.ignitionai.nativeapp.CAPTURE_STATUS";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private volatile BluetoothSocket socket;
    private volatile boolean running;
    private volatile long lastProgress;
    private String fileName;
    public android.os.IBinder onBind(Intent intent) { return null; }
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("capture", "Vehicle capture", NotificationManager.IMPORTANCE_LOW));
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        if (running) return START_NOT_STICKY;
        Intent open = new Intent(this, MainActivity.class);
        Notification notification = new Notification.Builder(this, "capture").setContentTitle("IgnitionAI vehicle capture")
            .setContentText("Capturing OBD telemetry. Open IgnitionAI to stop.").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)).build();
        startForeground(1, notification);
        running = true; lastProgress = android.os.SystemClock.elapsedRealtime();
        watchdog.scheduleAtFixedRate(() -> {
            if (running && android.os.SystemClock.elapsedRealtime() - lastProgress > 15000) closeSocket();
        }, 5, 5, TimeUnit.SECONDS);
        String address = intent.getStringExtra("address"), vehicle = intent.getStringExtra("vehicle");
        executor.execute(() -> capture(address, vehicle));
        return START_NOT_STICKY;
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void capture(String address, String vehicle) {
        String session = UUID.randomUUID().toString(); fileName = session + ".jsonl";
        File folder = new File(getFilesDir(), "sessions"); folder.mkdirs();
        int count = 0;
        try (BufferedWriter records = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(folder, fileName)), StandardCharsets.UTF_8));
             BufferedWriter raw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(folder, session + ".raw.jsonl")), StandardCharsets.UTF_8))) {
            android.bluetooth.BluetoothAdapter adapter = getSystemService(BluetoothManager.class).getAdapter();
            if (adapter == null || !adapter.isEnabled()) throw new IOException("Enable Bluetooth before connecting");
            socket = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            sendStatus("Connecting…", false); socket.connect(); lastProgress = android.os.SystemClock.elapsedRealtime();
            com.ignitionai.obdinput.adapter.BluetoothAdapter obd = new com.ignitionai.obdinput.adapter.BluetoothAdapter(address, vehicle, socket.getInputStream(), socket.getOutputStream());
            obd.startSession(session);
            while (running && count < 3600) {
                Optional<AdapterResult> poll = obd.pollNextMessage();
                if (poll.isEmpty()) throw new IOException("No complete OBD response; partial session retained");
                raw.write(poll.get().getRawPayload()); raw.newLine(); raw.flush();
                ObdMessage message = poll.get().getNormalizedMessage();
                records.write(ObdJsonMapper.serialize(message)); records.newLine(); records.flush();
                lastProgress = android.os.SystemClock.elapsedRealtime(); count++;
                sendStatus("Captured " + count + " records", false);
                Thread.sleep(250);
            }
            obd.endSession(); sendStatus("Capture saved: " + count + " records", true);
        } catch (Exception e) {
            sendStatus((running ? "Capture ended: " + e.getMessage() : "Capture stopped") + ". Retained " + count + " records.", true);
        } finally { running=false; closeSocket(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
    }
    private void sendStatus(String text, boolean complete) {
        getSharedPreferences("capture", MODE_PRIVATE).edit().putString("status", text).putString("file", fileName).apply();
        sendBroadcast(new Intent(STATUS).setPackage(getPackageName()).putExtra("status", text).putExtra("file", fileName).putExtra("complete", complete));
    }
    private void closeSocket() { try { if (socket != null) socket.close(); } catch (IOException ignored) { } }
    public void onDestroy() { running=false; closeSocket(); executor.shutdownNow(); watchdog.shutdownNow(); super.onDestroy(); }
}
