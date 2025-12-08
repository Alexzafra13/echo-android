package com.echo.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EchoCoral,
    onPrimary = Color.White,
    primaryContainer = EchoCoralDark,
    onPrimaryContainer = Color.White,
    secondary = EchoTeal,
    onSecondary = Color.Black,
    secondaryContainer = EchoTealDark,
    onSecondaryContainer = Color.White,
    tertiary = EchoTeal,
    background = EchoDarkBackground,
    onBackground = EchoTextPrimary,
    surface = EchoDarkSurface,
    onSurface = EchoTextPrimary,
    surfaceVariant = EchoDarkSurfaceVariant,
    onSurfaceVariant = EchoTextSecondary,
    error = EchoError,
    onError = Color.White,
    outline = EchoBorderDark,
    outlineVariant = EchoGlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EchoCoral,
    onPrimary = Color.White,
    primaryContainer = EchoCoralLight,
    onPrimaryContainer = EchoCoralDark,
    secondary = EchoTealDark,
    onSecondary = Color.White,
    secondaryContainer = EchoTeal,
    onSecondaryContainer = Color.Black,
    tertiary = EchoTeal,
    background = EchoLightBackground,
    onBackground = EchoTextPrimaryLight,
    surface = EchoLightSurface,
    onSurface = EchoTextPrimaryLight,
    surfaceVariant = EchoLightSurfaceVariant,
    onSurfaceVariant = EchoTextSecondaryLight,
    error = EchoError,
    onError = Color.White,
    outline = EchoBorderLight,
    outlineVariant = EchoBorderLight
)

@Composable
fun EchoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
