package com.local.listentomusic.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** In-Activity sheet: PiP and the shared video surface stay in the Activity window. */
@Composable
internal fun PlayerOverlay(
    state: MutableTransitionState<Boolean>,
    pictureInPicture: Boolean,
    onDismiss: () -> Unit,
    instantReveal: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (pictureInPicture) { Box(Modifier.fillMaxSize()) { content() }; return }
    // One transition owns scrim + sheet completion. The wallpaper must not steal
    // the shared video surface until the longer sheet exit has actually finished.
    AnimatedVisibility(state, enter = EnterTransition.None, exit = ExitTransition.None) {
      Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.matchParentSize()
                .animateEnterExit(
                    enter = if (instantReveal) EnterTransition.None else fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                )
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
        )
        BoxWithConstraints(
            Modifier.fillMaxSize().animateEnterExit(
                enter = if (instantReveal) EnterTransition.None else slideInVertically(tween(240)) { it },
                exit = slideOutVertically(tween(180)) { it },
            ),
        ) {
            val landscape = maxWidth > maxHeight
            val sheetHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            val dismissDistancePx = with(density) { 72.dp.toPx() }
            val finishDistancePx = with(density) { 120.dp.toPx() }
            var pullOffset by remember { mutableFloatStateOf(0f) }
            val pullState = rememberDraggableState { delta ->
                pullOffset = (pullOffset + delta).coerceIn(0f, sheetHeightPx)
            }
            LaunchedEffect(state.targetState) { if (state.targetState) pullOffset = 0f }
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(if (landscape) 1f else 0.97f)
                .graphicsLayer { translationY = pullOffset }
                .inspectElement("NOW_PLAYING_SHEET", "Dismissible Now Playing overlay"),
                shape = RoundedCornerShape(topStart = if (landscape) 0.dp else 22.dp, topEnd = if (landscape) 0.dp else 22.dp),
                color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                Box(Modifier.fillMaxSize()) {
                    content()
                    // The whole sheet follows the pull. Gesture arbitration keeps vertical
                    // pulls available while horizontal pager/seek gestures remain intact.
                    if (!landscape) Box(Modifier.align(Alignment.TopCenter).width(76.dp).height(28.dp)
                        .draggable(
                            state = pullState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                val dismiss = pullOffset >= dismissDistancePx || velocity > 1_100f
                                val motion = Animatable(pullOffset)
                                if (dismiss) {
                                    motion.animateTo(minOf(sheetHeightPx, pullOffset + finishDistancePx), tween(100)) { pullOffset = value }
                                    onDismiss()
                                } else {
                                    motion.animateTo(0f, spring(dampingRatio = .86f, stiffness = 520f)) { pullOffset = value }
                                }
                            },
                        ), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(32.dp).height(3.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
      }
    }
}
