package com.local.listentomusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun SeekFeedback(deltaMs: Long, event: Long, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var accumulated by remember { mutableLongStateOf(0L) }
    LaunchedEffect(event) {
        if (event != 0L) {
            accumulated = if (visible && (accumulated < 0) == (deltaMs < 0)) accumulated + deltaMs else deltaMs
            visible = true
            kotlinx.coroutines.delay(if (android.animation.ValueAnimator.areAnimatorsEnabled()) 460 else 220)
            visible = false
        }
    }
    AnimatedVisibility(visible, modifier, enter = fadeIn(tween(80)), exit = fadeOut(tween(160))) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(Color.Black.copy(alpha = .38f))
            .semantics { contentDescription = if (accumulated < 0) "Rewound ${abs(accumulated) / 1000} seconds" else "Skipped forward ${abs(accumulated) / 1000} seconds" },
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (accumulated < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                    null, tint = Color.White, modifier = Modifier.size(30.dp))
                Text("${abs(accumulated) / 1000} seconds", color = Color.White,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
