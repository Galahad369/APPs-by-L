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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun SeekFeedback(deltaMs: Long, event: Long, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    var visible by remember { mutableStateOf(false) }
    var accumulated by remember { mutableLongStateOf(0L) }
    LaunchedEffect(event) {
        if (event != 0L) {
            accumulated = if (visible && (accumulated < 0) == (deltaMs < 0)) accumulated + deltaMs else deltaMs
            visible = true
            progress.snapTo(0f)
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) progress.animateTo(1f, tween(420))
            else kotlinx.coroutines.delay(220)
            visible = false
        }
    }
    if (!visible) return
    val accent = MaterialTheme.colorScheme.secondary
    Box(modifier.width(96.dp).height(82.dp)
        .semantics { contentDescription = if (accumulated < 0) "Rewound ${abs(accumulated) / 1000} seconds" else "Skipped forward ${abs(accumulated) / 1000} seconds" }
        .graphicsLayer {
            alpha = (1f - progress.value * .92f).coerceIn(0f, 1f)
        }
        .clip(RoundedCornerShape(40.dp)).background(Color.Black.copy(alpha=.32f)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val p = progress.value
            val direction = if (deltaMs < 0) -1 else 1
            repeat(3) { index ->
                val phase = (p * 1.45f - index * .16f).coerceIn(0f, 1f)
                val x = size.width / 2 + direction * ((index - 1) * 12.dp.toPx() + phase * 3.dp.toPx())
                val y = size.height * 0.31f
                val color = accent.copy(alpha = (.25f + .75f * kotlin.math.sin(phase * kotlin.math.PI).toFloat()).coerceIn(0f,1f))
                drawLine(color, Offset(x - direction * 5.dp.toPx(), y - 6.dp.toPx()), Offset(x, y), 3.dp.toPx())
                drawLine(color, Offset(x, y), Offset(x - direction * 5.dp.toPx(), y + 6.dp.toPx()), 3.dp.toPx())
            }
        }
        Text("${if (accumulated < 0) "−" else "+"}${abs(accumulated) / 1000}s", Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 10.dp).padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
