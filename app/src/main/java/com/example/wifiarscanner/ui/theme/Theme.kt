package com.example.wifiarscanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Signal Colors
val SignalExcellent = Color(0xFF00FF88)
val SignalGood      = Color(0xFF88FF00)
val SignalFair      = Color(0xFFFFDD00)
val SignalPoor      = Color(0xFFFF8800)
val SignalBad       = Color(0xFFFF2200)

// App palette — dark, premium
val Background     = Color(0xFF0A0E1A)
val Surface        = Color(0xFF111827)
val SurfaceVariant = Color(0xFF1C2539)
val OnSurface      = Color(0xFFE8EDF5)
val Primary        = Color(0xFF4FC3F7)
val PrimaryDark    = Color(0xFF0288D1)
val Accent         = Color(0xFF00E5FF)
val Divider        = Color(0xFF2A3548)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Accent,
    background = Background,
    surface = Surface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
)

@Composable
fun WiFiARTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
