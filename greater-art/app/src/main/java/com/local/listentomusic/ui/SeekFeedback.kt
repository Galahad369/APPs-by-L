package com.local.listentomusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun SeekFeedback(deltaMs: Long, event: Long, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(event) { if (event != 0L) { progress.snapTo(0f); progress.animateTo(1f, tween(700)) } }
    if (progress.value >= 1f) return
    val accent = MaterialTheme.colorScheme.secondary
    Box(modifier.width(104.dp).height(100.dp).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val p = progress.value
            drawCircle(accent.copy(alpha = (1f - p) * 0.35f), size.minDimension * (0.25f + p * 0.65f), style = Stroke(2.dp.toPx()))
            repeat(3) { index ->
                val direction = if (deltaMs < 0) -1 else 1
                val x = size.width / 2 + direction * ((index - 1) * 14.dp.toPx() + p * 10.dp.toPx())
                val y = size.height * 0.35f
                val color = accent.copy(alpha = ((1f - p) * (1f - index * 0.2f)).coerceIn(0f, 1f))
                drawLine(color, Offset(x - direction * 5.dp.toPx(), y - 6.dp.toPx()), Offset(x, y), 3.dp.toPx())
                drawLine(color, Offset(x, y), Offset(x - direction * 5.dp.toPx(), y + 6.dp.toPx()), 3.dp.toPx())
            }
        }
        Text("${if (deltaMs < 0) "−" else "+"}${abs(deltaMs) / 1000}s", Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 12.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White.copy(alpha = 1f - progress.value), style = MaterialTheme.typography.labelLarge)
    }
}
