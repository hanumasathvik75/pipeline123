package com.wifiar.scanner.heatmap

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.wifiar.scanner.data.toSignalColor

object GradientRenderer {

    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun render(canvas: Canvas, visiblePoints: List<VisiblePoint>, screenWidth: Int, screenHeight: Int) {
        if (visiblePoints.isEmpty()) return

        canvas.save()
        // Render each point as a radial gradient blob
        for (vp in visiblePoints) {
            val colorLong = vp.rssi.toSignalColor()
            val coreColor = Color.valueOf(
                ((colorLong shr 16) and 0xFF) / 255f,
                ((colorLong shr 8) and 0xFF) / 255f,
                (colorLong and 0xFF) / 255f,
                0.70f
            ).toArgb()
            val edgeColor = Color.valueOf(
                ((colorLong shr 16) and 0xFF) / 255f,
                ((colorLong shr 8) and 0xFF) / 255f,
                (colorLong and 0xFF) / 255f,
                0f
            ).toArgb()

            // Blob radius: closer points appear larger
            val baseRadius = (screenWidth * 0.22f)
            val radius = baseRadius * (1f - vp.normalizedDistance * 0.5f)

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
}
