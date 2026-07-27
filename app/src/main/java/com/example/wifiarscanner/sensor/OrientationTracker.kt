package com.example.wifiarscanner.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Orientation(
    val azimuthRad: Float = 0f,
    val pitchRad: Float = 0f,
    val rollRad: Float = 0f,
    val rotationMatrix: FloatArray = FloatArray(9)
) {
    // FloatArray equality override
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Orientation) return false
        return azimuthRad == other.azimuthRad &&
            pitchRad == other.pitchRad &&
            rollRad == other.rollRad
    }
    override fun hashCode(): Int = azimuthRad.hashCode() * 31 + pitchRad.hashCode()
}

class OrientationTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val orientationAngles = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)

    private val _orientation = MutableStateFlow(Orientation())
    val orientation: StateFlow<Orientation> = _orientation

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_GAME  // ~50Hz — smooth but not CPU-heavy
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            // Remap axes so portrait-held phone maps correctly
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )
            SensorManager.getOrientation(remappedMatrix, orientationAngles)
            _orientation.value = Orientation(
                azimuthRad = orientationAngles[0],
                pitchRad   = orientationAngles[1],
                rollRad    = orientationAngles[2],
                rotationMatrix = rotationMatrix.copyOf()
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
