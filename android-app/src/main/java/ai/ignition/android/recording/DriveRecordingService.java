package ai.ignition.android.recording;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import ai.ignition.android.MainActivity;
import ai.ignition.android.obd.AdapterIdentifier;
import ai.ignition.android.obd.ClassicSppTransport;
import ai.ignition.android.obd.ObdTransport;
import ai.ignition.android.obd.TransportType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Android Foreground Service owning the active drive recording lifecycle.
 * Manages the persistent notification, decouples capture from Activity recreation,
 * and ensures safe and idempotent start and stop operations.
 */
public class DriveRecordingService extends Service {

    public static final String ACTION_START_RECORDING = "ai.ignition.android.action.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "ai.ignition.android.action.STOP_RECORDING";
    public static final String EXTRA_DEVICE = "ai.ignition.android.extra.DEVICE";
    public static final String EXTRA_SESSION_ID = "ai.ignition.android.extra.SESSION_ID";

    public static final String NOTIFICATION_CHANNEL_ID = "ignition_drive_recording";
    public static final int NOTIFICATION_ID = 2001;

    // Static service state observable by MainActivity
    private static volatile boolean recordingActive = false;
    private static volatile String activeSessionId = null;
    private static volatile CaptureState activeState = CaptureState.IDLE;
    private static volatile long activeRecordCount = 0;
    private static volatile long activeValidObservations = 0;
    private static volatile long activeUnavailableObservations = 0;
    private static volatile long activeErrorObservations = 0;
    private static volatile long activeCycleCount = 0;
    private static volatile long lastCycleDurationMs = 0;
    private static volatile long lastRequestDurationMs = 0;
    private static volatile long sessionStartTimeMs = 0;
    private static volatile long lastNotificationUpdateMs = 0;
    private static final List<DriveCaptureEngine.CaptureListener> stateListeners = new CopyOnWriteArrayList<>();

    private SessionRepository repository;
    private DriveCaptureEngine captureEngine;
    private NotificationManager notificationManager;
    private BluetoothDevice connectedDevice;

    public static boolean isRecordingActive() { return recordingActive; }
    public static String getActiveSessionId() { return activeSessionId; }
    public static CaptureState getActiveState() { return activeState; }
    public static long getActiveRecordCount() { return activeRecordCount; }
    public static long getActiveValidObservations() { return activeValidObservations; }
    public static long getActiveUnavailableObservations() { return activeUnavailableObservations; }
    public static long getActiveErrorObservations() { return activeErrorObservations; }
    public static long getActiveCycleCount() { return activeCycleCount; }
    public static long getLastCycleDurationMs() { return lastCycleDurationMs; }
    public static long getLastRequestDurationMs() { return lastRequestDurationMs; }
    public static long getSessionStartTimeMs() { return sessionStartTimeMs; }

    public static void addStateListener(DriveCaptureEngine.CaptureListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public static void removeStateListener(DriveCaptureEngine.CaptureListener listener) {
        if (listener != null) {
            stateListeners.remove(listener);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        File baseDir = getFilesDir();
        repository = new SessionRepository(baseDir);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START_RECORDING.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
            String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
            handleStartRecording(device, sessionId);
        } else if (ACTION_STOP_RECORDING.equals(action)) {
            handleStopRecording();
        }

        return START_NOT_STICKY;
    }

    /**
     * Starts drive recording safely and idempotently.
     */
    private synchronized void handleStartRecording(BluetoothDevice device, String requestedSessionId) {
        if (recordingActive) {
            // Strictly idempotent: already running or initializing
            return;
        }

        connectedDevice = device;
        activeSessionId = (requestedSessionId != null && !requestedSessionId.isBlank())
                ? requestedSessionId
                : UUID.randomUUID().toString();
        sessionStartTimeMs = System.currentTimeMillis();
        lastNotificationUpdateMs = sessionStartTimeMs;
        recordingActive = true;
        activeRecordCount = 0;
        activeValidObservations = 0;
        activeUnavailableObservations = 0;
        activeErrorObservations = 0;
        activeCycleCount = 0;
        lastCycleDurationMs = 0;
        lastRequestDurationMs = 0;
        activeState = CaptureState.INITIALIZING;

        // Start foreground notification immediately
        Notification notification = buildNotification("Initializing drive recording…");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Factory to construct transport
        TransportFactory factory = () -> {
            if (connectedDevice != null) {
                AdapterIdentifier.IdentificationResult ident = AdapterIdentifier.identify(getApplicationContext(), connectedDevice);
                if (ident != null && ident.transport != null) {
                    return ident.transport;
                }
                return new ClassicSppTransport(connectedDevice);
            }
            throw new IOException("Cannot create transport: no Bluetooth device specified");
        };

        // Engine callback
        DriveCaptureEngine.CaptureListener engineListener = new DriveCaptureEngine.CaptureListener() {
            @Override
            public void onStateChanged(CaptureState state) {
                activeState = state;
                updateNotificationForState(state);
                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onStateChanged(state); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onObservation(SessionEvent event) {
                if (captureEngine != null) {
                    activeRecordCount = captureEngine.getRecordCount();
                    activeValidObservations = captureEngine.getValidObservationCount();
                    activeUnavailableObservations = captureEngine.getUnavailableObservationCount();
                    activeErrorObservations = captureEngine.getErrorObservationCount();
                    activeCycleCount = captureEngine.getCycleCount();
                    lastCycleDurationMs = captureEngine.getLastCycleDurationMs();
                    lastRequestDurationMs = captureEngine.getLastRequestDurationMs();
                } else {
                    activeRecordCount++;
                }

                // Throttle notification updates: at most once every 3 seconds to avoid system notification flooding
                long now = System.currentTimeMillis();
                if (now - lastNotificationUpdateMs >= 3000L) {
                    lastNotificationUpdateMs = now;
                    updateNotificationForState(activeState);
                }

                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onObservation(event); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onConnectionLost(String reason) {
                activeState = CaptureState.RECONNECTING;
                updateNotificationForState(CaptureState.RECONNECTING);
                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onConnectionLost(reason); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onConnectionRestored(long gapDurationMs) {
                activeState = CaptureState.RECORDING;
                updateNotificationForState(CaptureState.RECORDING);
                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onConnectionRestored(gapDurationMs); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onError(String error) {
                activeState = CaptureState.ERROR;
                updateNotificationForState(CaptureState.ERROR);
                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onError(error); } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFinalized(SessionManifest manifest) {
                activeState = CaptureState.STOPPED;
                recordingActive = false;
                for (DriveCaptureEngine.CaptureListener l : stateListeners) {
                    try { l.onFinalized(manifest); } catch (Exception ignored) {}
                }
                stopForegroundNotificationAndSelf();
            }
        };

        captureEngine = new DriveCaptureEngine(activeSessionId, repository, factory, null, engineListener);
        captureEngine.startCapture(null);
    }

    /**
     * Stops drive recording safely and idempotently.
     */
    private synchronized void handleStopRecording() {
        if (!recordingActive && activeState == CaptureState.STOPPED) {
            // Idempotent: already stopped
            return;
        }

        recordingActive = false;
        activeState = CaptureState.STOPPING;
        updateNotificationForState(CaptureState.STOPPING);

        if (captureEngine != null) {
            captureEngine.stopCapture();
        } else {
            stopForegroundNotificationAndSelf();
        }
    }

    private void stopForegroundNotificationAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private void updateNotificationForState(CaptureState state) {
        String shortId = activeSessionId != null && activeSessionId.length() >= 8
                ? activeSessionId.substring(0, 8)
                : "active";

        String text;
        switch (state) {
            case RECORDING:
                text = "Recording active • Session: " + shortId + " • " + activeValidObservations + " valid obs (" + activeRecordCount + " total)";
                break;
            case RECONNECTING:
                text = "⚠️ Connection lost — Reconnecting adapter…";
                break;
            case STOPPING:
                text = "Finalizing session & validating records…";
                break;
            case ERROR:
                text = "❌ Capture error or reconnect exhausted";
                break;
            default:
                text = "IgnitionAI Drive Recording in progress";
                break;
        }

        Notification notification = buildNotification(text);
        if (notificationManager != null && recordingActive) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String contentText) {
        // Tap action: open MainActivity
        Intent openActivityIntent = new Intent(this, MainActivity.class);
        openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: Stop recording
        Intent stopIntent = new Intent(this, DriveRecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("IgnitionAI Drive Recording")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Drive Recording",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Ongoing drive telemetry capture and session persistence");
            channel.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        handleStopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
