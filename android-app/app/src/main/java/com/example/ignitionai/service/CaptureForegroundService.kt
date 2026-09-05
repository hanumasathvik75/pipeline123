package com.example.ignitionai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ignitionai.MainActivity
import com.example.ignitionai.R
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * IgnitionAI Native Android Capture Foreground Service
 * Implements persistent Bluetooth BLE logging and append-only disk storage
 * Survives screen lock, app backgrounding, and incoming phone calls.
 */
class CaptureForegroundService : Service() {

    companion object {
        private const val TAG = "CaptureService"
        private const val CHANNEL_ID = "ignitionai_capture_channel"
        private const val NOTIFICATION_ID = 4040

        const val ACTION_START_CAPTURE = "com.example.ignitionai.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.example.ignitionai.STOP_CAPTURE"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_CAPTURE_ID = "capture_id"

        // IgnitionAI Dongle BLE UUIDs
        val SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val CHAR_DATA_UUID: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var captureFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private var packetCount = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "IgnitionAI:CaptureWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: "00:00:00:00:00:00"
                val captureId = intent.getStringExtra(EXTRA_CAPTURE_ID) ?: "capture_${System.currentTimeMillis()}"
                startCapture(deviceAddress, captureId)
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
            }
        }
        return START_STICKY
    }

    private fun startCapture(deviceAddress: String, captureId: String) {
        val notification = createNotification("Recording live drive: $captureId")
        startForeground(NOTIFICATION_ID, notification)
        wakeLock?.acquire(15 * 60 * 1000L /* 15 minutes timeout */)

        try {
            val dir = File(filesDir, "captures").apply { mkdirs() }
            captureFile = File(dir, "$captureId.raw.bin")
            fileOutputStream = FileOutputStream(captureFile, true)
            Log.i(TAG, "Created append-only capture log: ${captureFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize local file output", e)
        }

        connectToBleDongle(deviceAddress)
    }

    private fun connectToBleDongle(deviceAddress: String) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid Bluetooth address: $deviceAddress", e)
            return
        }

        try {
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            Log.i(TAG, "Initiated BLE connection to $deviceAddress")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permissions for connectGatt", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BLE Connected. Discovering services...")
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException on discoverServices", e)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "BLE Disconnected from dongle.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHAR_DATA_UUID)
                if (characteristic != null) {
                    try {
                        gatt.setCharacteristicNotification(characteristic, true)
                        Log.i(TAG, "Subscribed to live diagnostic data characteristic")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException on setCharacteristicNotification", e)
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { bytes ->
                writeRawPacket(bytes)
            }
        }
    }

    private fun writeRawPacket(bytes: ByteArray) {
        try {
            val timestampMs = System.currentTimeMillis()
            // Format: [8 bytes timestampMs][2 bytes length][payload]
            val buffer = java.nio.ByteBuffer.allocate(10 + bytes.size).apply {
                putLong(timestampMs)
                putShort(bytes.size.toShort())
                put(bytes)
            }
            fileOutputStream?.write(buffer.array())
            fileOutputStream?.flush()
            packetCount++
        } catch (e: Exception) {
            Log.e(TAG, "Error writing raw packet to disk", e)
        }
    }

    private fun stopCapture() {
        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
            fileOutputStream?.close()
            fileOutputStream = null
            wakeLock?.release()
            Log.i(TAG, "Stopped capture. Total packets persisted: $packetCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error during capture teardown", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IgnitionAI Diagnostic Capture Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IgnitionAI Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status during active 10-minute vehicle test drives"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
