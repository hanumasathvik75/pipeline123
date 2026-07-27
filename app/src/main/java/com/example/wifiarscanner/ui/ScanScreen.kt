package com.example.wifiarscanner.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.wifiarscanner.ScanState
import com.example.wifiarscanner.ScanViewModel
import com.example.wifiarscanner.data.toComposeColor
import com.example.wifiarscanner.data.toSignalLabel
import com.example.wifiarscanner.heatmap.HeatmapOverlayView

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val rssi by viewModel.rssi.collectAsState()
    val ssid by viewModel.ssid.collectAsState()
    val orientation by viewModel.orientation.collectAsState()
    val scanPoints by viewModel.scanPoints.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val pointCount by viewModel.pointCount.collectAsState()
    val scanDuration by viewModel.scanDurationSec.collectAsState()
    val isConnected by viewModel.isWifiConnected.collectAsState()

    // Heatmap overlay reference
    var overlayView by remember { mutableStateOf<HeatmapOverlayView?>(null) }

    // Update overlay whenever orientation or points change
    LaunchedEffect(orientation) {
        overlayView?.currentOrientation = orientation
    }
    LaunchedEffect(scanPoints) {
        overlayView?.currentPoints = scanPoints
    }
    LaunchedEffect(rssi) {
        overlayView?.currentRssi = rssi
    }
    LaunchedEffect(scanState) {
        when (scanState) {
            ScanState.SCANNING -> overlayView?.startRendering()
            ScanState.IDLE -> {
                overlayView?.stopRendering()
                overlayView?.startRendering() // keep live dot rendering
            }
            ScanState.COMPLETE -> {
                overlayView?.stopRendering()
                onNavigateToResults()
            }
        }
    }

    val signalColor = rssi.toComposeColor()
    val signalLabel = rssi.toSignalLabel()

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview + AR overlay
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // CameraX preview
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                    addView(previewView)

                    // AR heatmap overlay
                    val heatmap = HeatmapOverlayView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    }
                    addView(heatmap)
                    overlayView = heatmap
                    heatmap.startRendering()

                    // Bind CameraX
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay at top for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC0A0E1A), Color.Transparent)
                    )
                )
        )

        // Dark gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD0A0E1A))
                    )
                )
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Network name
            Column {
                Text(
                    text = if (isConnected) "📶 $ssid" else "⚠️ No WiFi",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConnected) "$rssi dBm · $signalLabel" else "Connect to WiFi to scan",
                    color = signalColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Signal indicator pill
            Row(
                modifier = Modifier
                    .background(Color(0xBB111827), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalBars(rssi = rssi, color = signalColor)
                Spacer(Modifier.width(6.dp))
                Text("$rssi", color = signalColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(" dBm", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }

            // Settings button
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xBB111827), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Legend
        SignalLegend(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // Scan controls at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats while scanning
            AnimatedVisibility(visible = scanState == ScanState.SCANNING) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    StatChip("📍 $pointCount points")
                    StatChip("⏱ ${formatDuration(scanDuration)}")
                }
            }

            // Scan button
            if (scanState == ScanState.SCANNING) {
                Button(
                    onClick = { viewModel.stopScan() },
                    modifier = Modifier
                        .width(220.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                ) {
                    Text("⏹  Stop & Analyze", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White)
                }
            } else {
                Button(
                    onClick = { if (isConnected) viewModel.startScan() },
                    enabled = isConnected,
                    modifier = Modifier
                        .width(220.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4FC3F7),
                        disabledContainerColor = Color(0xFF2A3548)
                    )
                ) {
                    Text(
                        if (isConnected) "▶  Start Scanning" else "No WiFi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) Color(0xFF0A0E1A) else Color.White.copy(0.4f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isConnected) "Walk through your space while scanning" else "Connect to WiFi first",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        // Scanning pulse ring indicator
        if (scanState == ScanState.SCANNING) {
            ScanningIndicator(modifier = Modifier.align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 80.dp))
        }
    }
}

@Composable
private fun SignalBars(rssi: Int, color: Color) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .background(
                        if (i <= bars) color else Color.White.copy(0.2f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun SignalLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xBB111827), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LegendItem(Color(0xFF00FF88), "Excellent")
        LegendItem(Color(0xFF88FF00), "Good")
        LegendItem(Color(0xFFFFDD00), "Fair")
        LegendItem(Color(0xFFFF8800), "Poor")
        LegendItem(Color(0xFFFF2200), "Bad")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, color = Color.White.copy(0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xBB111827), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScanningIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Row(
        modifier = modifier
            .background(Color(0xBB111827), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF88).copy(alpha = alpha), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("SCANNING", color = Color(0xFF00FF88).copy(alpha = alpha),
            fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
