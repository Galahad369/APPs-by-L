package com.local.listentomusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun SeekFeedback(deltaMs: Long, event: Long, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var accumulated by remember { mutableLongStateOf(0L) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(event) {
        if (event != 0L) {
            accumulated = if (visible && (accumulated < 0) == (deltaMs < 0)) accumulated + deltaMs else deltaMs
            visible = true
            sweep.snapTo(0f)
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) sweep.animateTo(1f, tween(420))
            delay(300)
            visible = false
        }
    }
    AnimatedVisibility(
        visible,
        modifier,
        enter = fadeIn(tween(80)),
        exit = fadeOut(tween(180)),
    ) {
        val backwards = accumulated < 0
        Box(Modifier.width(116.dp).height(82.dp)
            .background(Color.Black.copy(alpha = .48f), RoundedCornerShape(16.dp))
            .semantics { contentDescription = if (backwards) "Rewound ${abs(accumulated) / 1000} seconds" else "Skipped forward ${abs(accumulated) / 1000} seconds" },
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
                    repeat(3) { index ->
                        val phase = (sweep.value * 3f - index).coerceIn(0f, 1f)
                        Icon(if (backwards) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            null, tint = Color.White,
                            modifier = Modifier.size(30.dp).graphicsLayer {
                                alpha = .32f + phase * .68f
                                translationX = (if (backwards) 1f else -1f) * (1f - phase) * 4.dp.toPx()
                            })
                    }
                }
                Text("${abs(accumulated) / 1000}s", color = Color.White,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
