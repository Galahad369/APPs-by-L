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
import androidx.compose.ui.graphics.drawscope.Stroke
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
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) progress.animateTo(1f, tween(650))
            else kotlinx.coroutines.delay(300)
            visible = false
        }
    }
    if (!visible) return
    val accent = MaterialTheme.colorScheme.secondary
    Box(modifier.width(112.dp).height(104.dp).graphicsLayer { alpha = (1f-progress.value).coerceIn(0f,1f) }
        .clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha=.3f)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val p = progress.value
            repeat(2) { ring ->
                val t = (p * 1.4f - ring * .28f).coerceIn(0f, 1f)
                drawCircle(accent.copy(alpha = (1f-t) * .5f), size.minDimension * (.22f + t * .65f), style = Stroke(1.5.dp.toPx()))
            }
            repeat(3) { index ->
                val direction = if (deltaMs < 0) -1 else 1
                val x = size.width / 2 + direction * ((index - 1) * 14.dp.toPx() + p * 10.dp.toPx())
                val y = size.height * 0.35f
                val color = accent.copy(alpha = (.35f + .65f * kotlin.math.sin((p * 2f - index*.18f).coerceIn(0f,1f) * kotlin.math.PI).toFloat()).coerceIn(0f,1f))
                drawLine(color, Offset(x - direction * 5.dp.toPx(), y - 6.dp.toPx()), Offset(x, y), 3.dp.toPx())
                drawLine(color, Offset(x, y), Offset(x - direction * 5.dp.toPx(), y + 6.dp.toPx()), 3.dp.toPx())
            }
        }
        Text("${if (accumulated < 0) "−" else "+"}${abs(accumulated) / 1000}s", Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 12.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
