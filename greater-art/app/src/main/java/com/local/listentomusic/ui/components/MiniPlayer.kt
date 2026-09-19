package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MiniWindowMetrics
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.ui.inspectElement
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    controller: Player?,
    videoPreviewActive: Boolean,
    language: AppLanguage,
    onPreviewBoundsChanged: (Rect) -> Unit,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val previewHeight = with(density) { MiniWindowMetrics.heightPx(this.density).toDp() }
    val squarePreview = when {
        playback.isVideo -> MiniWindowMetrics.isSquareAspect(playback.videoAspectRatio)
        artwork != null -> MiniWindowMetrics.isSquareAspect(artwork.width.toFloat() / artwork.height.coerceAtLeast(1))
        else -> true
    }
    val previewWidth = if (squarePreview) previewHeight
        else with(density) { MiniWindowMetrics.widthPx(this.density).toDp() }
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f
    // Translucent scrim with no hard card edge.
    Surface(
        modifier = Modifier.fillMaxWidth()
            .inspectElement("LIBRARY_MINI_PLAYER", "Tap to open Now Playing")
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        LiquidMetalSurface(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpen)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(previewHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(previewWidth, previewHeight)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInWindow()
                                onPreviewBoundsChanged(Rect(
                                    bounds.left.roundToInt(), bounds.top.roundToInt(),
                                    bounds.right.roundToInt(), bounds.bottom.roundToInt(),
                                ))
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (playback.isVideo && videoPreviewActive && controller != null) {
                            InlineVideoPreview(controller)
                        } else if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Text(
                        text = playback.title.substringBeforeLast('.', playback.title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    )
                    IconButton(onClick = onPrevious, enabled = playback.hasPrevious, modifier = Modifier.size(34.dp).inspectElement("MINI_PREVIOUS_BUTTON", "Previous media")) {
                        Icon(Icons.Rounded.SkipPrevious, uiText(language, "Previous", "上一首"), modifier = Modifier.size(21.dp))
                    }
                    IconButton(onClick = onTogglePlay, modifier = Modifier.size(38.dp).inspectElement("MINI_PLAY_PAUSE_BUTTON", if (playback.isPlaying) "Pause" else "Play")) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            uiText(language, if (playback.isPlaying) "Pause" else "Play", if (playback.isPlaying) "暫停" else "播放"),
                            modifier = Modifier.size(25.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    IconButton(onClick = onNext, enabled = playback.hasNext, modifier = Modifier.size(34.dp).inspectElement("MINI_NEXT_BUTTON", "Next media")) {
                        Icon(Icons.Rounded.SkipNext, uiText(language, "Next", "下一首"), modifier = Modifier.size(21.dp))
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun InlineVideoPreview(player: Player) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    AndroidView(
        factory = { context ->
            (android.view.LayoutInflater.from(context).inflate(
                com.local.listentomusic.R.layout.background_video,
                android.widget.FrameLayout(context), false,
            ) as PlayerView).apply {
                tag = "LIBRARY_MINI_PLAYER"
                useController = false
                isClickable = false
                isFocusable = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setKeepContentOnPlayerReset(true)
            }
        },
        update = { if (resumed) VideoSurfaceOwner.attach(player, it) else VideoSurfaceOwner.detach(it) },
        // Release this exact view, not a mutable reference in an effect keyed by itself.
        // null -> created-view used to dispose that effect and detach the new surface.
        onRelease = VideoSurfaceOwner::detach,
        modifier = Modifier.fillMaxSize(),
    )
}
