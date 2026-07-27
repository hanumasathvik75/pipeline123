package com.example.wifiarscanner.heatmap

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

object GradientRenderer {

    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Pre-extract RGB from RSSI color long to avoid repeated bit-ops per frame
    private fun rssiToRGB(rssi: Int): Triple<Int, Int, Int> {
        return when {
            rssi >= -50 -> Triple(0x00, 0xFF, 0x88)  // Vivid Green
            rssi >= -60 -> Triple(0x88, 0xFF, 0x00)  // Lime
            rssi >= -70 -> Triple(0xFF, 0xDD, 0x00)  // Yellow
            rssi >= -80 -> Triple(0xFF, 0x88, 0x00)  // Orange
            else        -> Triple(0xFF, 0x22, 0x00)  // Red
        }
    }

    fun render(
        canvas: Canvas,
        visiblePoints: List<VisiblePoint>,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (visiblePoints.isEmpty()) return

        canvas.save()

        for (vp in visiblePoints) {
            val (r, g, b) = rssiToRGB(vp.rssi)

            // Blob radius: nearer = bigger, farther = smaller
            val baseRadius = screenWidth * 0.28f
            val radius = baseRadius * (1f - vp.normalizedDistance * 0.55f).coerceAtLeast(0.2f)

            // Core: semi-transparent, Edge: fully transparent
            val coreAlpha = (0.65f * (1f - vp.normalizedDistance * 0.3f)).coerceIn(0.25f, 0.72f)
            val coreColor = Color.argb(
                (coreAlpha * 255).toInt().coerceIn(0, 255), r, g, b
            )
            val edgeColor = Color.argb(0, r, g, b)

            blobPaint.shader = RadialGradient(
                vp.screenX, vp.screenY, radius,
                intArrayOf(coreColor, edgeColor),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )

            canvas.drawCircle(vp.screenX, vp.screenY, radius, blobPaint)
        }

        canvas.restore()
    }

    /**
     * Draw a single pulsing live dot for the current position (when not scanning)
     * Shows live signal as a small animated dot in the center-bottom of screen.
     */
    fun renderLiveDot(canvas: Canvas, rssi: Int, screenWidth: Int, screenHeight: Int, pulseScale: Float) {
        val (r, g, b) = rssiToRGB(rssi)
        val cx = screenWidth / 2f
        val cy = screenHeight * 0.72f
        val baseR = screenWidth * 0.10f * pulseScale

        blobPaint.shader = RadialGradient(
            cx, cy, baseR,
            intArrayOf(Color.argb(180, r, g, b), Color.argb(0, r, g, b)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, baseR, blobPaint)
    }
}
