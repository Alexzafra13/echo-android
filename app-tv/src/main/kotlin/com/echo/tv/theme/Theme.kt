package com.echo.tv.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Echo brand colors
private val EchoCoral = Color(0xFFFF6B6B)
private val EchoDarkBackground = Color(0xFF1A1A2E)
private val EchoDarkSurface = Color(0xFF16213E)
private val EchoLightText = Color(0xFFE4E4E4)

private val TvDarkColorScheme = darkColorScheme(
    primary = EchoCoral,
    onPrimary = Color.White,
    secondary = EchoCoral,
    onSecondary = Color.White,
    background = EchoDarkBackground,
    onBackground = EchoLightText,
    surface = EchoDarkSurface,
    onSurface = EchoLightText,
    surfaceVariant = Color(0xFF2D2D44),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

/**
 * Echo TV Theme.
 * Uses dark colors optimized for TV viewing distance.
 */
@Composable
fun EchoTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}
