package com.local.listentomusic.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** In-Activity sheet: PiP and the shared video surface stay in the Activity window. */
@Composable
internal fun PlayerOverlay(state: MutableTransitionState<Boolean>, pictureInPicture: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (pictureInPicture) { Box(Modifier.fillMaxSize()) { content() }; return }
    // One transition owns scrim + sheet completion. The wallpaper must not steal
    // the shared video surface until the longer sheet exit has actually finished.
    AnimatedVisibility(state, enter = EnterTransition.None, exit = ExitTransition.None) {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().animateEnterExit(enter = fadeIn(tween(180)), exit = fadeOut(tween(150)))
            .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onDismiss))
        BoxWithConstraints(Modifier.fillMaxSize().animateEnterExit(enter = slideInVertically(tween(240)) { it }, exit = slideOutVertically(tween(180)) { it })) {
            val landscape = maxWidth > maxHeight
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(if (landscape) 1f else 0.97f)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
                shape = RoundedCornerShape(topStart = if (landscape) 0.dp else 22.dp, topEnd = if (landscape) 0.dp else 22.dp),
                color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    // Dedicated handle avoids intercepting the song list or seek gestures.
                    if (!landscape) Box(Modifier.align(Alignment.TopCenter).width(76.dp).height(24.dp)
                        .pointerInput(Unit) {
                            var distance = 0f
                            detectVerticalDragGestures(onDragStart = { distance = 0f }, onDragEnd = { if (distance > 48.dp.toPx()) onDismiss() }) { change, dy ->
                                change.consume(); distance += dy
                            }
                        }, contentAlignment = Alignment.Center) {
                        Box(Modifier.width(32.dp).height(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
      }
    }
}
