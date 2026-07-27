package com.example.wifiarscanner.data

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

class SpatialGrid {
    private val _points = CopyOnWriteArrayList<ScanPoint>()
    val points: List<ScanPoint> get() = _points

    private val MERGE_DISTANCE = 0.4f

    @Synchronized
    fun addPoint(point: ScanPoint) {
        val nearby = _points.firstOrNull { existing ->
            distanceBetween(existing.x, existing.y, point.x, point.y) < MERGE_DISTANCE
        }
        if (nearby != null) {
            val idx = _points.indexOf(nearby)
            // Weighted average: newer reading counts 40%, older 60%
            val merged = nearby.copy(
                rssi = (nearby.rssi * 3 + point.rssi * 2) / 5,
                timestamp = point.timestamp
            )
            _points[idx] = merged
        } else {
            _points.add(point)
        }
    }

    fun clear() = _points.clear()

    fun getSummary(): ScanSummary? {
        if (_points.size < 2) return null
        val best = _points.maxByOrNull { it.rssi }!!
        val worst = _points.minByOrNull { it.rssi }!!
        val dirs = computeDirectionSummaries()
        val maxX = _points.maxOf { it.x }
        val minX = _points.minOf { it.x }
        val maxY = _points.maxOf { it.y }
        val minY = _points.minOf { it.y }
        val area = ((maxX - minX) * (maxY - minY)).coerceAtLeast(0f)
        return ScanSummary(
            points = _points.toList(),
            directions = dirs,
            bestRssi = best.rssi,
            worstRssi = worst.rssi,
            bestX = best.x,
            bestY = best.y,
            worstX = worst.x,
            worstY = worst.y,
            totalArea = area
        )
    }

    private fun computeDirectionSummaries(): List<DirectionSummary> {
        val cx = _points.map { it.x }.average().toFloat()
        val cy = _points.map { it.y }.average().toFloat()
        val quadrants = mapOf(
            "North" to _points.filter { it.y > cy },
            "South" to _points.filter { it.y <= cy },
            "East"  to _points.filter { it.x > cx },
            "West"  to _points.filter { it.x <= cx }
        )
        return quadrants.mapNotNull { (label, pts) ->
            if (pts.isEmpty()) null
            else DirectionSummary(
                label = label,
                avgRssi = pts.map { it.rssi }.average().toInt(),
                minRssi = pts.minOf { it.rssi },
                maxRssi = pts.maxOf { it.rssi },
                pointCount = pts.size
            )
        }
    }

    private fun distanceBetween(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
