package com.local.listentomusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Restrained animated metal: graphite, silver and a narrow teal reflection.
 * It is intentionally reserved for a few important surfaces so the interface
 * stays calm, readable and cheap to render while a list is scrolling.
 */
@Composable
fun LiquidMetalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    baseColor: Color? = null,
    accentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liquidMetal")
    val travel by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "metalReflection",
    )
    val base = baseColor ?: MaterialTheme.colorScheme.surface
    val teal = accentColor ?: MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.clip(shape).background(base),
        contentAlignment = contentAlignment,
    ) {
        // matchParentSize is deliberately non-measuring. fillMaxSize here made
        // this decorative canvas claim every available pixel when the surface
        // was used inside Scaffold.bottomBar, expanding the mini player over
        // the whole library.
        Canvas(Modifier.matchParentSize()) {
            val focus = size.width * travel
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.19f),
                        teal.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Black.copy(alpha = 0.14f),
                    ),
                    start = Offset(focus - size.width * 0.72f, size.height),
                    end = Offset(focus + size.width * 0.72f, 0f),
                ),
            )
        }
        // Transparent/decorative containers must not inherit an arbitrary Activity
        // content color. Metal surfaces are dark by design, so keep controls legible.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}
