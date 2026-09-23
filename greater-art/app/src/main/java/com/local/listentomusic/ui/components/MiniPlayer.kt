package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import com.local.listentomusic.model.CompactPlayerMetrics
import com.local.listentomusic.ui.inspectElement
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    artwork: Bitmap?, controller: Player?, settings: com.local.listentomusic.data.UserPreferences,
    onPreviewBoundsChanged: (Rect) -> Unit, onOpen: () -> Unit,
) {
    val inspector = com.local.listentomusic.ui.LocalUiInspector.current
    val keys = remember { List(6) { Any() } }
    val native = remember { arrayOfNulls<CompactPlayerView>(1) }
    DisposableEffect(inspector) { onDispose { keys.forEach { inspector?.remove(it) } } }
    Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
        AndroidView(
            factory = { CompactPlayerView(it).also { view -> native[0] = view } },
            update = {
                it.onOpen = onOpen
                it.appearance(settings)
                it.setArtwork(artwork)
                it.bind(controller, "LIBRARY_MINI")
            },
            onRelease = { it.release() },
            modifier = Modifier.fillMaxWidth().height(CompactPlayerMetrics.HEIGHT_DP.dp)
                .inspectElement("LIBRARY_MINI_PLAYER", "Shared compact player; excludes navigation inset")
                .onGloballyPositioned {
                    val b = it.boundsInWindow()
                    onPreviewBoundsChanged(Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt()))
                    if (inspector != null) {
                        val root = it.boundsInRoot()
                        native[0]?.inspectionBounds()?.forEachIndexed { index, (label, rect) ->
                            inspector.update(keys[index], label, "Shared compact player control",
                                androidx.compose.ui.geometry.Rect(root.left + rect.left, root.top + rect.top,
                                    root.left + rect.right, root.top + rect.bottom))
                        }
                    }
                },
        )
    }
}
