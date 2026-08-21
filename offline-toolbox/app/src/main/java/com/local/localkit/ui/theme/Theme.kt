package com.local.localkit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Acid = Color(0xFFE8FF59)
private val Ink = Color(0xFF12130F)
private val Paper = Color(0xFFF5F5F1)
private val Muted = Color(0xFF62645E)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Color(0xFF5C6500),
    tertiary = Color(0xFF315C52),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8E9E3),
    onSurfaceVariant = Muted,
    primaryContainer = Acid,
    onPrimaryContainer = Ink
)

private val DarkColors = darkColorScheme(
    primary = Acid,
    onPrimary = Ink,
    secondary = Acid,
    tertiary = Color(0xFF8FD8C7),
    background = Color(0xFF0E0F0C),
    onBackground = Color(0xFFF0F1EB),
    surface = Color(0xFF171814),
    onSurface = Color(0xFFF0F1EB),
    surfaceVariant = Color(0xFF292B25),
    onSurfaceVariant = Color(0xFFBFC1B8),
    primaryContainer = Acid,
    onPrimaryContainer = Ink
)

@Composable
fun LocalKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

