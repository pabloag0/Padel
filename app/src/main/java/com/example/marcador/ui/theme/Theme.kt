package com.example.marcador.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = CourtGreen,
    onPrimary = SoftIce,
    secondary = AquaAccent,
    onSecondary = NightGreen,
    tertiary = LimeGlow,
    background = NightGreen,
    onBackground = SoftIce,
    surface = SurfaceGreen,
    onSurface = SoftIce,
    surfaceVariant = SurfaceGreenLight,
    onSurfaceVariant = Color(0xFFB8D8CF),
    error = DangerCoral,
    onError = SoftIce
)

@Composable
fun MarcadorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
