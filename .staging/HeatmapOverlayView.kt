package com.wifiar.scanner.heatmap

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.View
import com.wifiar.scanner.data.ScanPoint
import com.wifiar.scanner.data.toSignalColor
import com.wifiar.scanner.sensor.Orientation

class HeatmapOverlayView(context: Context) : View(context) {

    // Updated by ViewModel
    @Volatile var currentPoints: List<ScanPoint> = emptyList()
    @Volatile var currentOrientation: Orientation = Orientation()
    @Volatile var isScanning: Boolean = false

    private val choreographerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isScanning) {
                invalidate()
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun startRendering() {
        isScanning = true
        Choreographer.getInstance().postFrameCallback(choreographerCallback)
    }

    fun stopRendering() {
        isScanning = false
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
        invalidate() // Final draw with all points
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val points = currentPoints
        val orientation = currentOrientation

        if (points.isEmpty()) {
            // Show a live pulsing circle for current position with current RSSI
            return
        }

        val visiblePoints = ProjectionEngine.project(
            points = points,
            orientation = orientation,
            screenWidth = width,
            screenHeight = height
        )

        GradientRenderer.render(canvas, visiblePoints, width, height)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isScanning = false
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
    }
}
