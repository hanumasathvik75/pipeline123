package com.wifiar.scanner.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

data class Position(val x: Float = 0f, val y: Float = 0f, val stepCount: Int = 0)

class PositionTracker(context: Context, private val orientationTracker: OrientationTracker) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val STRIDE_LENGTH = 0.70f // meters per step

    private var _x = 0f
    private var _y = 0f
    private var _steps = 0

    private val _position = MutableStateFlow(Position())
    val position: StateFlow<Position> = _position

    fun start() {
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun reset() {
        _x = 0f; _y = 0f; _steps = 0
        _position.value = Position()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val azimuth = orientationTracker.orientation.value.azimuthRad
            // Move in direction of current azimuth
            _x += STRIDE_LENGTH * sin(azimuth)
            _y += STRIDE_LENGTH * cos(azimuth)
            _steps++
            _position.value = Position(_x, _y, _steps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
