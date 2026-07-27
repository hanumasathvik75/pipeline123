package com.example.wifiarscanner.data

data class ScanPoint(
    val x: Float,           // meters from scan origin (east positive)
    val y: Float,           // meters from scan origin (north positive)
    val rssi: Int,          // dBm, e.g. -65
    val timestamp: Long = System.currentTimeMillis()
)

data class DirectionSummary(
    val label: String,
    val avgRssi: Int,
    val minRssi: Int,
    val maxRssi: Int,
    val pointCount: Int
)

data class ScanSummary(
    val points: List<ScanPoint>,
    val directions: List<DirectionSummary>,
    val bestRssi: Int,
    val worstRssi: Int,
    val bestX: Float,
    val bestY: Float,
    val worstX: Float,
    val worstY: Float,
    val totalArea: Float
)

fun Int.toSignalColor(): Long {
    return when {
        this >= -50 -> 0xFF00FF88L  // Vivid Green
        this >= -60 -> 0xFF88FF00L  // Lime
        this >= -70 -> 0xFFFFDD00L  // Yellow
        this >= -80 -> 0xFFFF8800L  // Orange
        else        -> 0xFFFF2200L  // Red
    }
}

fun Int.toSignalLabel(): String {
    return when {
        this >= -50 -> "Excellent"
        this >= -60 -> "Good"
        this >= -70 -> "Fair"
        this >= -80 -> "Poor"
        else        -> "Very Poor"
    }
}

fun Int.toSignalBars(): Int {
    return when {
        this >= -50 -> 4
        this >= -60 -> 3
        this >= -70 -> 2
        this >= -80 -> 1
        else        -> 0
    }
}

fun Int.toComposeColor(): androidx.compose.ui.graphics.Color {
    return when {
        this >= -50 -> androidx.compose.ui.graphics.Color(0xFF00FF88)
        this >= -60 -> androidx.compose.ui.graphics.Color(0xFF88FF00)
        this >= -70 -> androidx.compose.ui.graphics.Color(0xFFFFDD00)
        this >= -80 -> androidx.compose.ui.graphics.Color(0xFFFF8800)
        else        -> androidx.compose.ui.graphics.Color(0xFFFF2200)
    }
}
