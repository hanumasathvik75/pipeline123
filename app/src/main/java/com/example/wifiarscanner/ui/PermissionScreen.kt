package com.example.wifiarscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wifiarscanner.ui.theme.Background
import com.example.wifiarscanner.ui.theme.Primary
import com.example.wifiarscanner.ui.theme.SurfaceVariant
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(permissionsState: MultiplePermissionsState) {
    val anyDeniedWithRationale = permissionsState.permissions.any { perm ->
        val status = perm.status
        status is PermissionStatus.Denied && status.shouldShowRationale
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📡",
                fontSize = 72.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "WiFi AR Scanner",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Visualize your WiFi signal strength in real-time AR",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Required permissions",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                PermissionItem("📷", "Camera", "For the AR overlay view")
                PermissionItem("📍", "Location", "Required by Android 10+ to read WiFi signal")
                PermissionItem("🚶", "Activity Recognition", "To track movement while scanning")
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    "Grant Permissions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0A0E1A)
                )
            }

            if (anyDeniedWithRationale) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Some permissions were denied. Please grant them in Settings to use the app.",
                    color = Color(0xFFFF8800),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(emoji: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
