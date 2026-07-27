package com.example.wifiarscanner.heatmap

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.View
import com.example.wifiarscanner.data.ScanPoint
import com.example.wifiarscanner.sensor.Orientation

class HeatmapOverlayView(context: Context) : View(context) {

    @Volatile var currentPoints: List<ScanPoint> = emptyList()
    @Volatile var currentOrientation: Orientation = Orientation()
    @Volatile var currentRssi: Int = -100
    @Volatile var isScanning: Boolean = false

    private var pulsePhase = 0f
    private var isRenderingActive = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isRenderingActive) {
                pulsePhase = (pulsePhase + 0.05f) % (2f * Math.PI.toFloat())
                invalidate()
                // Post next frame — capped at ~30fps for battery/CPU efficiency
                view.postDelayed({
                    if (isRenderingActive) {
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }, 33) // ~30fps
            }
        }
    }

    private val view = this

    fun startRendering() {
        if (!isRenderingActive) {
            isRenderingActive = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stopRendering() {
        isRenderingActive = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val points = currentPoints
        val orientation = currentOrientation
        val rssi = currentRssi

        if (points.isEmpty()) {
            // Show live pulsing dot at current position
            val scale = 0.85f + 0.15f * kotlin.math.sin(pulsePhase)
            GradientRenderer.renderLiveDot(canvas, rssi, width, height, scale)
            return
        }

        val visiblePoints = ProjectionEngine.project(
            points = points,
            orientation = orientation,
            screenWidth = width,
            screenHeight = height
        )

        // Also draw live pulse dot at screen center for current position
        val scale = 0.85f + 0.15f * kotlin.math.sin(pulsePhase)
        GradientRenderer.renderLiveDot(canvas, rssi, width, height, scale)

        GradientRenderer.render(canvas, visiblePoints, width, height)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRenderingActive = false
    }
}
