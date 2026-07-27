package com.example.ignitionai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IgnitionColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = BgDarkest,
    tertiary = LightBlue,
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgPanel,
    onSurfaceVariant = TextSecondary,
    error = DangerRed
)

@Composable
fun IgnitionAITheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = IgnitionColorScheme, typography = Typography, content = content)
}
