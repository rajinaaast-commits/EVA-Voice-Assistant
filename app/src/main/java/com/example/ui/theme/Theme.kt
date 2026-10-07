package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EvaDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF9CF4FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF260057),
    secondaryContainer = Color(0xFF4C158A),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = HologramBlue,
    onTertiary = Color(0xFF001E36),
    background = CosmicDarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    error = StatusError,
    onError = Color.White
)

@Composable
fun EvaTheme(
    darkTheme: Boolean = true, // EVA is always optimized for futuristic dark immersion
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EvaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
