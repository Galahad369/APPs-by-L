package com.local.listentomusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF161C1A),
    onPrimary = Color(0xFFF3F5F0),
    primaryContainer = Color(0xFFDDF8F1),
    onPrimaryContainer = Color(0xFF10221E),
    secondary = Color(0xFF267B69),
    tertiary = Color(0xFF6C7E1C),
    background = Color(0xFFF5F5F1),
    surface = Color(0xFFFBFBF7),
    surfaceVariant = Color(0xFFE5EAE5),
    onSurface = Color(0xFF111513),
    onSurfaceVariant = Color(0xFF666D68),
    inverseSurface = Color(0xFF181C1B),
    inverseOnSurface = Color(0xFFF3F5F0),
    inversePrimary = Color(0xFF8BE9D3),
    outline = Color(0xFF8D9690),
    outlineVariant = Color(0xFFD7DDD8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF3F5F0),
    onPrimary = Color(0xFF101311),
    primaryContainer = Color(0xFF24453D),
    onPrimaryContainer = Color(0xFFDDF8F1),
    secondary = Color(0xFF8BE9D3),
    tertiary = Color(0xFFD9FF68),
    background = Color(0xFF080A09),
    surface = Color(0xFF151918),
    surfaceVariant = Color(0xFF202624),
    onSurface = Color(0xFFF3F5F0),
    onSurfaceVariant = Color(0xFF9CA39D),
    inverseSurface = Color(0xFFF3F5F0),
    inverseOnSurface = Color(0xFF101311),
    inversePrimary = Color(0xFF267B69),
    outline = Color(0xFF65706A),
    outlineVariant = Color(0xFF323A37),
)

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(9.dp),
    large = RoundedCornerShape(13.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun GreaterArtTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = SharpShapes,
        content = content,
    )
}
