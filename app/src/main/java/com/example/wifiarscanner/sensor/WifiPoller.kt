package com.example.wifiarscanner.sensor

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WifiPoller(context: Context) {

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _rssi = MutableStateFlow(-100)
    val rssi: StateFlow<Int> = _rssi

    private val _ssid = MutableStateFlow("Unknown")
    val ssid: StateFlow<String> = _ssid

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    @Suppress("DEPRECATION")
                    val info = wifiManager.connectionInfo
                    if (info != null && info.networkId != -1) {
                        val rawRssi = info.rssi
                        // Clamp to realistic range
                        _rssi.value = rawRssi.coerceIn(-100, -20)
                        val raw = info.ssid ?: "Unknown"
                        _ssid.value = raw.removeSurrounding("\"")
                        _isConnected.value = true
                    } else {
                        _rssi.value = -100
                        _ssid.value = "Not connected"
                        _isConnected.value = false
                    }
                } catch (e: Exception) {
                    _rssi.value = -100
                    _isConnected.value = false
                }
                delay(1000L)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
