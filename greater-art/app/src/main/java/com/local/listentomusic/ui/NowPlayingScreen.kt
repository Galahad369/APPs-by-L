package com.local.listentomusic.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppLanguage
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    language: AppLanguage,
    controller: MediaController?,
    contentPadding: PaddingValues,
    isPictureInPicture: Boolean,
    onVideoBoundsChanged: (Rect) -> Unit,
    onPictureInPicture: () -> Unit,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
) {
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    if (isPictureInPicture && playback.isVideo) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(controller, onVideoBoundsChanged, Modifier.fillMaxSize())
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .background(MaterialTheme.colorScheme.background),
    ) {
        val landscape = maxWidth > maxHeight
        val immersiveVideo = playback.isVideo && (fullscreen || landscape)
        if (playback.isVideo) FullscreenEffect(enabled = immersiveVideo)
        BackHandler(enabled = fullscreen) { fullscreen = false }

        if (playback.isVideo) {
            val videoPageModifier = if (immersiveVideo) {
                Modifier.fillMaxSize()
            } else {
                // Normal portrait playback reserves a black system-bar strip. Only true
                // fullscreen/landscape video is allowed to draw edge to edge.
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
            }
            Column(videoPageModifier.background(Color(0xFF090B0A))) {
                VideoPlayerStage(
                    playback = playback,
                    controller = controller,
                    immersive = immersiveVideo,
                    onVideoBoundsChanged = onVideoBoundsChanged,
                    onBack = onBack,
                    onPictureInPicture = onPictureInPicture,
                    onFullscreen = { fullscreen = !fullscreen },
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    modifier = if (immersiveVideo) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                            .aspectRatio(playback.videoAspectRatio.coerceIn(0.75f, 2.25f))
                    },
                )
                if (!immersiveVideo) {
                    SecondaryControls(
                        playback = playback,
                        onSpeed = onSpeed,
                        onRepeat = onRepeat,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        } else {
            AudioPlayer(
                playback = playback,
                artwork = artwork,
                language = language,
                onBack = onBack,
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onSpeed = onSpeed,
                onRepeat = onRepeat,
            )
        }
    }
}

@Composable
private fun VideoPlayerStage(
    playback: PlaybackUiState,
    controller: MediaController?,
    immersive: Boolean,
    onVideoBoundsChanged: (Rect) -> Unit,
    onBack: () -> Unit,
    onPictureInPicture: () -> Unit,
    onFullscreen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val maximum = if (hasDuration) playback.durationMs.toFloat() else 1f
    val position = if (seeking) seekPosition else if (hasDuration) playback.positionMs.toFloat() else 0f

    LaunchedEffect(controlsVisible, playback.isPlaying, playback.currentPath) {
        if (controlsVisible && playback.isPlaying) {
            delay(2_500)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier.background(Color.Black).clickable { controlsVisible = !controlsVisible },
        contentAlignment = Alignment.Center,
    ) {
        VideoSurface(controller, onVideoBoundsChanged, Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.68f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f),
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    OverlayIconButton(onClick = onPictureInPicture) {
                        Icon(Icons.Rounded.PictureInPictureAlt, "Floating player", tint = Color.White)
                    }
                    OverlayIconButton(onClick = onFullscreen) {
                        Icon(
                            if (immersive) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            if (immersive) "Exit fullscreen" else "Fullscreen",
                            tint = Color.White,
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(31.dp))
                    }
                    FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(58.dp)) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (playback.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(33.dp),
                        )
                    }
                    OverlayIconButton(onClick = onNext, enabled = playback.hasNext) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(31.dp))
                    }
                }

                val timelineModifier = if (immersive) {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                } else {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                }
                Column(
                    modifier = timelineModifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    CompactSlider(
                        value = position.coerceIn(0f, maximum),
                        onValueChange = { seeking = true; seekPosition = it },
                        onValueChangeFinished = { onSeek(seekPosition.toLong()); seeking = false },
                        valueRange = 0f..maximum,
                        enabled = hasDuration,
                        activeColor = Color(0xFFF3F5F0),
                        inactiveColor = Color.White.copy(alpha = 0.38f),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatDuration(if (seeking) position.toLong() else playback.positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(formatDuration(playback.durationMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayer(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    language: AppLanguage,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    uiText(language, "Back to library", "返回音樂庫"),
                )
            }
        }
        Box(
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp).fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp)).background(
                    Brush.linearGradient(
                        listOf(Color(0xFF111413), Color(0xFF29423C), Color(0xFF1A211F))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = uiText(language, "Album artwork", "專輯封面"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(116.dp),
                    tint = Color(0xFF8BE9D3),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                playback.title.substringBeforeLast('.', playback.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            WaveformTimeline(playback, onSeek, language)
            Transport(playback, onPrevious, onTogglePlay, onNext)
            SecondaryControlRow(playback, onSpeed, onRepeat)
            PlaybackError(playback.errorMessage)
        }
    }
}

@Composable
private fun SecondaryControls(
    playback: PlaybackUiState,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.background(Color(0xFF090B0A))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            playback.title.substringBeforeLast('.', playback.title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFFF3F5F0),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "Local video  •  offline",
            color = Color(0xFF9CA39D),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(30.dp))
        SecondaryControlRow(playback, onSpeed, onRepeat, dark = true)
        PlaybackError(playback.errorMessage)
    }
}

@Composable
private fun Timeline(playback: PlaybackUiState, onSeek: (Long) -> Unit) {
    var seeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val maximum = if (hasDuration) playback.durationMs.toFloat() else 1f
    val position = if (seeking) seekPosition else if (hasDuration) playback.positionMs.toFloat() else 0f
    CompactSlider(
        value = position.coerceIn(0f, maximum),
        onValueChange = { seeking = true; seekPosition = it },
        onValueChangeFinished = { onSeek(seekPosition.toLong()); seeking = false },
        valueRange = 0f..maximum,
        enabled = hasDuration,
        activeColor = MaterialTheme.colorScheme.secondary,
        inactiveColor = MaterialTheme.colorScheme.outlineVariant,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(if (seeking) position.toLong() else playback.positionMs), style = MaterialTheme.typography.labelMedium)
        Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformTimeline(
    playback: PlaybackUiState,
    onSeek: (Long) -> Unit,
    language: AppLanguage,
) {
    var seeking by remember(playback.currentPath) { mutableStateOf(false) }
    var seekPosition by remember(playback.currentPath) { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    val maximum = if (hasDuration) playback.durationMs.toFloat() else 1f
    val position = if (seeking) seekPosition else if (hasDuration) playback.positionMs.toFloat() else 0f
    val fraction = if (hasDuration) (position / maximum).coerceIn(0f, 1f) else 0f
    val active = MaterialTheme.colorScheme.secondary
    val inactive = MaterialTheme.colorScheme.outlineVariant

    Slider(
        value = position.coerceIn(0f, maximum),
        onValueChange = { seeking = true; seekPosition = it },
        onValueChangeFinished = {
            if (hasDuration) onSeek(seekPosition.toLong())
            seeking = false
        },
        valueRange = 0f..maximum,
        enabled = hasDuration,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        thumb = {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (hasDuration) active else Color.Transparent))
        },
        track = {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val bars = 72
                val spacing = size.width / bars
                repeat(bars) { index ->
                    val wave = abs(sin(index * 0.67) * 0.62 + sin(index * 0.19) * 0.38)
                    val barHeight = size.height * (0.18f + wave.toFloat() * 0.74f)
                    val x = spacing * (index + 0.5f)
                    drawLine(
                        color = if ((index + 1f) / bars <= fraction) active else inactive,
                        start = Offset(x, (size.height - barHeight) / 2f),
                        end = Offset(x, (size.height + barHeight) / 2f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatDuration(if (seeking) seekPosition.toLong() else playback.positionMs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            if (hasDuration) formatDuration(playback.durationMs) else uiText(language, "Loading duration…", "正在讀取長度…"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    activeColor: Color,
    inactiveColor: Color,
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range > 0f) {
        ((value - valueRange.start) / range).coerceIn(0f, 1f)
    } else 0f
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(22.dp),
        thumb = { _ ->
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(activeColor)
            )
        },
        track = { _ ->
            Box(
                Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(inactiveColor)
            ) {
                Box(
                    Modifier.fillMaxWidth(fraction).fillMaxHeight().background(activeColor)
                )
            }
        },
    )
}

@Composable
private fun Transport(
    playback: PlaybackUiState,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(68.dp)) {
            Icon(
                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playback.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onNext, enabled = playback.hasNext) {
            Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun SecondaryControlRow(
    playback: PlaybackUiState,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    dark: Boolean = false,
) {
    var speedMenuOpen by remember { mutableStateOf(false) }
    val buttonColors = if (dark) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFF202624),
            contentColor = Color(0xFFF3F5F0),
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Button(
                onClick = { speedMenuOpen = true },
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors,
            ) {
                Icon(Icons.Rounded.Speed, null)
                Text("  ${speedLabel(playback.speed)}")
            }
            DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
                playbackSpeeds.forEach { speed ->
                    DropdownMenuItem(
                        text = { Text(if (speed == playback.speed) "✓  ${speedLabel(speed)}" else speedLabel(speed)) },
                        onClick = { speedMenuOpen = false; onSpeed(speed) },
                    )
                }
            }
        }
        Button(onClick = onRepeat, modifier = Modifier.weight(1f), colors = buttonColors) {
            Icon(
                if (playback.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                null,
            )
            Text(
                "  " + when (playback.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "One"
                    Player.REPEAT_MODE_ALL -> "All"
                    else -> "Off"
                }
            )
        }
    }
}

@Composable
private fun PlaybackError(message: String?) {
    if (message != null) {
        Spacer(Modifier.height(14.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
        content = content,
    )
}

@Composable
private fun VideoSurface(
    controller: MediaController?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setKeepContentOnPlayerReset(true)
                player = controller
            }
        },
        update = { it.player = controller },
        modifier = modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            onBoundsChanged(
                Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                )
            )
        },
    )
}

@Composable
private fun FullscreenEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    val systemDark = isSystemInDarkTheme()
    DisposableEffect(activity, enabled, systemDark) {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (enabled) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insets.isAppearanceLightStatusBars = false
            insets.isAppearanceLightNavigationBars = false
        }
        onDispose {
            if (enabled) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
            insets.isAppearanceLightStatusBars = !systemDark
            insets.isAppearanceLightNavigationBars = !systemDark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "$speed×"
