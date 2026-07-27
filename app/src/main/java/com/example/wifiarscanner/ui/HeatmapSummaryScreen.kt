package com.example.wifiarscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifiarscanner.ScanViewModel
import com.example.wifiarscanner.data.DirectionSummary
import com.example.wifiarscanner.data.ScanSummary
import com.example.wifiarscanner.data.toComposeColor
import com.example.wifiarscanner.data.toSignalLabel
import com.example.wifiarscanner.ui.theme.Background
import com.example.wifiarscanner.ui.theme.Surface
import com.example.wifiarscanner.ui.theme.SurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapSummaryScreen(
    viewModel: ScanViewModel,
    onNavigateToSuggestions: () -> Unit,
    onNavigateBack: () -> Unit,
    onRescan: () -> Unit
) {
    val summary by viewModel.scanSummary.collectAsState()
    val pointCount by viewModel.pointCount.collectAsState()
    val scanDuration by viewModel.scanDurationSec.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Background
    ) { padding ->
        if (summary == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scan data. Go back and scan your space.", color = Color.White.copy(0.5f))
            }
            return@Scaffold
        }

        val s = summary!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Overview card
            item {
                ScanOverviewCard(s, pointCount, scanDuration)
            }

            // Best / Worst callouts
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SignalCallout(
                        modifier = Modifier.weight(1f),
                        emoji = "🟢",
                        label = "Best Spot",
                        rssi = s.bestRssi,
                        position = "${formatMeters(s.bestX, s.bestY)}"
                    )
                    SignalCallout(
                        modifier = Modifier.weight(1f),
                        emoji = "🔴",
                        label = "Weakest Spot",
                        rssi = s.worstRssi,
                        position = "${formatMeters(s.worstX, s.worstY)}"
                    )
                }
            }

            // Direction breakdown
            item {
                Text(
                    "Signal by Direction",
                    color = Color.White.copy(0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(s.directions) { dir ->
                DirectionCard(dir)
            }

            // Mini heatmap grid (visual representation)
            item {
                Text(
                    "Spatial Map",
                    color = Color.White.copy(0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                MiniHeatmapGrid(summary = s)
            }

            // Action buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToSuggestions,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
                    ) {
                        Text("🧠 Get AI Suggestions", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0E1A))
                    }
                    OutlinedButton(
                        onClick = onRescan,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🔄 Scan Again", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanOverviewCard(summary: ScanSummary, points: Int, duration: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan Summary", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("📍", "$points", "data points", Modifier.weight(1f))
            MiniStat("⏱", formatDur(duration), "duration", Modifier.weight(1f))
            MiniStat("📐", "${"%.0f".format(summary.totalArea)} m²", "area", Modifier.weight(1f))
        }
        // Signal range bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${summary.worstRssi} dBm", color = Color(0xFFFF2200), fontSize = 12.sp)
                Text("${summary.bestRssi} dBm", color = Color(0xFF00FF88), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color(0xFFFF2200), Color(0xFFFF8800), Color(0xFFFFDD00),
                                Color(0xFF88FF00), Color(0xFF00FF88))
                        ),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun SignalCallout(
    modifier: Modifier,
    emoji: String,
    label: String,
    rssi: Int,
    position: String
) {
    val color = rssi.toComposeColor()
    Column(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("$emoji $label", color = Color.White.copy(0.7f), fontSize = 12.sp)
        Text("$rssi dBm", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(rssi.toSignalLabel(), color = color, fontSize = 12.sp)
        Text(position, color = Color.White.copy(0.45f), fontSize = 11.sp)
    }
}

@Composable
private fun DirectionCard(dir: DirectionSummary) {
    val color = dir.avgRssi.toComposeColor()
    val fraction = ((dir.avgRssi + 100f) / 80f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val arrow = when (dir.label) {
            "North" -> "⬆"; "South" -> "⬇"; "East" -> "➡"; else -> "⬅"
        }
        Text(arrow, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Text(dir.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "avg ${dir.avgRssi}dBm · ${dir.pointCount} pts",
                color = Color.White.copy(0.5f), fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = color,
                trackColor = Color.White.copy(0.1f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            dir.avgRssi.toSignalLabel(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniHeatmapGrid(summary: ScanSummary) {
    if (summary.points.size < 2) return

    val minX = summary.points.minOf { it.x }
    val maxX = summary.points.maxOf { it.x }
    val minY = summary.points.minOf { it.y }
    val maxY = summary.points.maxOf { it.y }
    val rangeX = (maxX - minX).coerceAtLeast(0.1f)
    val rangeY = (maxY - minY).coerceAtLeast(0.1f)

    val COLS = 8
    val ROWS = 8

    // Build grid cells
    val grid = Array(ROWS) { row ->
        Array(COLS) { col ->
            val cellMinX = minX + (col.toFloat() / COLS) * rangeX
            val cellMaxX = minX + ((col + 1f) / COLS) * rangeX
            val cellMinY = minY + (row.toFloat() / ROWS) * rangeY
            val cellMaxY = minY + ((row + 1f) / ROWS) * rangeY

            val inCell = summary.points.filter {
                it.x in cellMinX..cellMaxX && it.y in cellMinY..cellMaxY
            }
            if (inCell.isEmpty()) null else inCell.map { it.rssi }.average().toInt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("N ↑", color = Color.White.copy(0.4f), fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        for (row in ROWS - 1 downTo 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (col in 0 until COLS) {
                    val rssi = grid[row][col]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                if (rssi != null) rssi.toComposeColor().copy(alpha = 0.75f)
                                else Color.White.copy(0.05f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
        Text("S ↓", color = Color.White.copy(0.4f), fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun formatMeters(x: Float, y: Float): String {
    val dist = kotlin.math.sqrt(x * x + y * y)
    return "%.1fm from start".format(dist)
}

private fun formatDur(sec: Int): String {
    val m = sec / 60; val s = sec % 60
    return if (m > 0) "${m}m${s}s" else "${s}s"
}
