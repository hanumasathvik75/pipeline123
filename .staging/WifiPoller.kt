package com.wifiar.scanner.sensor

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

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val info = wifiManager.connectionInfo
                    if (info != null && info.networkId != -1) {
                        _rssi.value = info.rssi
                        val raw = info.ssid ?: "Unknown"
                        _ssid.value = raw.removeSurrounding("\"") // remove quotes Android adds
                    } else {
                        _rssi.value = -100
                        _ssid.value = "Not connected"
                    }
                } catch (e: Exception) {
                    _rssi.value = -100
                }
                delay(1000L)
            }
        }
    }

    fun stop() { job?.cancel() }
}
