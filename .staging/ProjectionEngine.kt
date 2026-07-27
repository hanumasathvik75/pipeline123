package com.wifiar.scanner.heatmap

import com.wifiar.scanner.data.ScanPoint
import com.wifiar.scanner.sensor.Orientation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class VisiblePoint(
    val screenX: Float,
    val screenY: Float,
    val rssi: Int,
    val distanceMeters: Float,
    val normalizedDistance: Float  // 0=near, 1=far edge of FOV
)

object ProjectionEngine {

    private const val H_FOV_RAD = 1.05f  // ~60 degrees horizontal
    private const val V_FOV_RAD = 0.79f  // ~45 degrees vertical

    fun project(
        points: List<ScanPoint>,
        orientation: Orientation,
        screenWidth: Int,
        screenHeight: Int,
        deviceHeightMeters: Float = 1.5f  // assume phone held at ~1.5m height
    ): List<VisiblePoint> {
        if (points.isEmpty() || screenWidth == 0) return emptyList()

        val azimuth = orientation.azimuthRad
        val pitch = orientation.pitchRad
        val result = mutableListOf<VisiblePoint>()

        for (point in points) {
            // Vector from device to scan point in world space (flat ground plane)
            val worldDx = point.x  // east
            val worldDy = point.y  // north
            val worldDz = -deviceHeightMeters  // scan point is on ground, phone is above

            // Rotate into camera frame using azimuth
            val cosA = cos(-azimuth).toFloat()
            val sinA = sin(-azimuth).toFloat()
            val camX = cosA * worldDx - sinA * worldDy  // camera right
            val camY = worldDz  // camera up (world Z)
            val camZ = sinA * worldDx + cosA * worldDy  // camera forward (depth)

            // Only render points in front of camera
            if (camZ <= 0.1f) continue

            // Apply pitch rotation
            val cosPitch = cos(-pitch).toFloat()
            val sinPitch = sin(-pitch).toFloat()
            val camYPitched = cosPitch * camY - sinPitch * camZ
            val camZPitched = sinPitch * camY + cosPitch * camZ

            if (camZPitched <= 0.1f) continue

            // Compute angular position
            val hAngle = atan2(camX, camZPitched)
            val vAngle = atan2(camYPitched, camZPitched)

            // Clip to FOV
            if (abs(hAngle) > H_FOV_RAD / 2f) continue
            if (abs(vAngle) > V_FOV_RAD / 2f) continue

            // Project to screen
            val sx = screenWidth / 2f + (hAngle / (H_FOV_RAD / 2f)) * (screenWidth / 2f)
            val sy = screenHeight / 2f - (vAngle / (V_FOV_RAD / 2f)) * (screenHeight / 2f)

            val distance = sqrt(worldDx * worldDx + worldDy * worldDy + worldDz * worldDz)
            val normalized = (distance / 10f).coerceIn(0f, 1f)

            result.add(VisiblePoint(sx, sy, point.rssi, distance, normalized))
        }
        return result
    }
}
