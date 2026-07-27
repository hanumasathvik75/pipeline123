package com.example.wifiarscanner

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wifiarscanner.ai.GeminiRepository
import com.example.wifiarscanner.ai.Suggestion
import com.example.wifiarscanner.ai.SuggestionEngine
import com.example.wifiarscanner.data.ScanPoint
import com.example.wifiarscanner.data.ScanSummary
import com.example.wifiarscanner.data.SpatialGrid
import com.example.wifiarscanner.sensor.Orientation
import com.example.wifiarscanner.sensor.OrientationTracker
import com.example.wifiarscanner.sensor.PositionTracker
import com.example.wifiarscanner.sensor.WifiPoller
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScanState { IDLE, SCANNING, COMPLETE }
enum class AiState { IDLE, LOADING, DONE, ERROR }

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app.applicationContext

    // Sensors
    val orientationTracker = OrientationTracker(context)
    val positionTracker = PositionTracker(context, orientationTracker)
    val wifiPoller = WifiPoller(context)

    // Live sensor state
    val orientation: StateFlow<Orientation> = orientationTracker.orientation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Orientation())
    val rssi: StateFlow<Int> = wifiPoller.rssi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -100)
    val ssid: StateFlow<String> = wifiPoller.ssid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unknown")
    val isWifiConnected: StateFlow<Boolean> = wifiPoller.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Spatial grid
    private val spatialGrid = SpatialGrid()
    private val _scanPoints = MutableStateFlow<List<ScanPoint>>(emptyList())
    val scanPoints: StateFlow<List<ScanPoint>> = _scanPoints.asStateFlow()

    // Scan state
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanDurationSec = MutableStateFlow(0)
    val scanDurationSec: StateFlow<Int> = _scanDurationSec.asStateFlow()

    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()

    // Scan summary
    private val _scanSummary = MutableStateFlow<ScanSummary?>(null)
    val scanSummary: StateFlow<ScanSummary?> = _scanSummary.asStateFlow()

    // AI / Suggestions
    private val _offlineSuggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val offlineSuggestions: StateFlow<List<Suggestion>> = _offlineSuggestions.asStateFlow()

    private val _aiState = MutableStateFlow(AiState.IDLE)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private val _geminiResponse = MutableStateFlow<String?>(null)
    val geminiResponse: StateFlow<String?> = _geminiResponse.asStateFlow()

    private val _geminiError = MutableStateFlow<String?>(null)
    val geminiError: StateFlow<String?> = _geminiError.asStateFlow()

    // Settings
    private val _geminiApiKey = MutableStateFlow(loadApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private var scanJob: Job? = null
    private var timerJob: Job? = null

    init {
        orientationTracker.start()
        positionTracker.start()
        wifiPoller.start(viewModelScope)
    }

    fun startScan() {
        if (_scanState.value == ScanState.SCANNING) return
        spatialGrid.clear()
        _scanPoints.value = emptyList()
        _pointCount.value = 0
        _scanDurationSec.value = 0
        _scanSummary.value = null
        _offlineSuggestions.value = emptyList()
        _geminiResponse.value = null
        _aiState.value = AiState.IDLE
        positionTracker.reset()

        _scanState.value = ScanState.SCANNING

        // Timer coroutine
        timerJob = viewModelScope.launch {
            while (isActive && _scanState.value == ScanState.SCANNING) {
                delay(1000L)
                _scanDurationSec.value++
            }
        }

        // Scan data collection coroutine
        scanJob = viewModelScope.launch {
            while (isActive && _scanState.value == ScanState.SCANNING) {
                val pos = positionTracker.position.value
                val rssi = wifiPoller.rssi.value
                if (rssi > -99) {
                    val point = ScanPoint(pos.x, pos.y, rssi)
                    spatialGrid.addPoint(point)
                    _scanPoints.value = spatialGrid.points.toList()
                    _pointCount.value = _scanPoints.value.size
                }
                delay(1000L)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        timerJob?.cancel()
        _scanState.value = ScanState.COMPLETE

        val summary = spatialGrid.getSummary()
        _scanSummary.value = summary
        if (summary != null) {
            _offlineSuggestions.value = SuggestionEngine.analyze(summary)
        }
    }

    fun resetScan() {
        scanJob?.cancel()
        timerJob?.cancel()
        spatialGrid.clear()
        _scanPoints.value = emptyList()
        _pointCount.value = 0
        _scanDurationSec.value = 0
        _scanSummary.value = null
        _offlineSuggestions.value = emptyList()
        _geminiResponse.value = null
        _aiState.value = AiState.IDLE
        _scanState.value = ScanState.IDLE
        positionTracker.reset()
    }

    fun requestGeminiSuggestions() {
        val summary = _scanSummary.value ?: return
        val key = _geminiApiKey.value.trim()
        if (key.isBlank()) {
            _geminiError.value = "No Gemini API key set. Add it in Settings."
            _aiState.value = AiState.ERROR
            return
        }

        _aiState.value = AiState.LOADING
        _geminiError.value = null

        viewModelScope.launch {
            val repo = GeminiRepository(key)
            val result = repo.getSuggestions(summary)
            result.fold(
                onSuccess = {
                    _geminiResponse.value = it
                    _aiState.value = AiState.DONE
                },
                onFailure = {
                    _geminiError.value = it.message ?: "Unknown error"
                    _aiState.value = AiState.ERROR
                }
            )
        }
    }

    fun saveApiKey(key: String) {
        _geminiApiKey.value = key
        context.getSharedPreferences("wifiar_prefs", Context.MODE_PRIVATE)
            .edit().putString("gemini_key", key).apply()
    }

    private fun loadApiKey(): String {
        return context.getSharedPreferences("wifiar_prefs", Context.MODE_PRIVATE)
            .getString("gemini_key", "") ?: ""
    }

    override fun onCleared() {
        super.onCleared()
        orientationTracker.stop()
        positionTracker.stop()
        wifiPoller.stop()
        scanJob?.cancel()
        timerJob?.cancel()
    }
}
