package com.example.wifiarscanner.heatmap

import com.example.wifiarscanner.data.ScanPoint
import com.example.wifiarscanner.sensor.Orientation
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
    val normalizedDistance: Float  // 0 = close, 1 = far
)

object ProjectionEngine {

    // Typical smartphone camera FOV
    private const val H_FOV_RAD = 1.09f  // ~62.5° horizontal
    private const val V_FOV_RAD = 0.84f  // ~48° vertical

    // How high phone is held above the floor (meters)
    private const val PHONE_HEIGHT = 1.4f

    fun project(
        points: List<ScanPoint>,
        orientation: Orientation,
        screenWidth: Int,
        screenHeight: Int
    ): List<VisiblePoint> {
        if (points.isEmpty() || screenWidth == 0 || screenHeight == 0) return emptyList()

        val azimuth = orientation.azimuthRad
        val pitch = orientation.pitchRad

        val cosA = cos(-azimuth).toFloat()
        val sinA = sin(-azimuth).toFloat()
        val cosPitch = cos(-pitch).toFloat()
        val sinPitch = sin(-pitch).toFloat()

        val result = mutableListOf<VisiblePoint>()

        for (point in points) {
            // World-space direction vector from phone to floor point
            val worldDx = point.x
            val worldDy = point.y
            val worldDz = -PHONE_HEIGHT  // floor is below the phone

            // Rotate by azimuth (yaw) into camera-facing frame
            val camX = cosA * worldDx - sinA * worldDy   // camera right
            val camY = worldDz                             // camera up = world up
            val camZ = sinA * worldDx + cosA * worldDy   // camera depth (forward)

            if (camZ <= 0.05f) continue  // behind camera

            // Apply pitch rotation
            val camYp = cosPitch * camY - sinPitch * camZ
            val camZp = sinPitch * camY + cosPitch * camZ

            if (camZp <= 0.05f) continue

            // Angular position within FOV
            val hAngle = atan2(camX, camZp)
            val vAngle = atan2(camYp, camZp)

            val hHalf = H_FOV_RAD / 2f
            val vHalf = V_FOV_RAD / 2f

            // Only render if within FOV (allow slight bleed for soft edges)
            if (abs(hAngle) > hHalf * 1.3f) continue
            if (abs(vAngle) > vHalf * 1.3f) continue

            // Map angle → screen pixels
            val sx = screenWidth / 2f + (hAngle / hHalf) * (screenWidth / 2f)
            val sy = screenHeight / 2f - (vAngle / vHalf) * (screenHeight / 2f)

            val distance = sqrt(
                worldDx * worldDx + worldDy * worldDy + (PHONE_HEIGHT * PHONE_HEIGHT)
            )
            val normalized = (distance / 12f).coerceIn(0f, 1f)

            result.add(VisiblePoint(sx, sy, point.rssi, distance, normalized))
        }
        return result
    }
}
