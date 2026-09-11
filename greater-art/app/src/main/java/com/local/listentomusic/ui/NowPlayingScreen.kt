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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
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
import com.local.listentomusic.SleepTimerState
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.model.MediaFile
import com.local.listentomusic.model.MediaKind
import com.local.listentomusic.model.LocalLyrics
import com.local.listentomusic.sleepTimerOptions
import com.local.listentomusic.ui.components.LiquidMetalSurface
import com.local.listentomusic.ui.ParallelMixerDialog
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

internal val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    artwork: Bitmap?,
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    language: AppLanguage,
    controller: MediaController?,
    contentPadding: PaddingValues,
    isPictureInPicture: Boolean,
    onVideoBoundsChanged: (Rect) -> Unit,
    onPictureInPicture: () -> Unit,
    onHome: () -> Unit,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    onPlayQueueItem: (MediaFile) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onLoadWaveform: suspend (String) -> FloatArray?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
) {
    var fullscreen by rememberSaveable { mutableStateOf(false) }

    if (isPictureInPicture && playback.isVideo) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(playback.currentPath, controller, onVideoBoundsChanged, Modifier.fillMaxSize())
        }
        return
    }

    BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(contentPadding)
                .background(Brush.verticalGradient(listOf(Color(0xFF080808), Color(0xFF0C0C0C), Color(0xFF0A0A0A)))),
        ) {
        val landscape = maxWidth > maxHeight
        val portraitVideoHeight = minOf(maxWidth / playback.videoAspectRatio.coerceIn(0.75f, 2.25f), maxHeight * 0.40f)
        val immersiveVideo = playback.isVideo && (fullscreen || landscape)
        FullscreenEffect(enabled = fullscreen || (playback.isVideo && landscape))
        BackHandler(enabled = fullscreen) { fullscreen = false }

        if (playback.isVideo) {
            val (pageBg, pageInsets) = if (immersiveVideo) {
                Color.Black to WindowInsets(0)
            } else {
                // Portrait playback: gutters use the theme so light mode does not
                // turn the whole page into a black slab. The video stage itself
                // keeps its black backdrop below for letterboxing the frame.
                MaterialTheme.colorScheme.background to WindowInsets.statusBars
            }
            val videoPageModifier = Modifier.fillMaxSize().background(pageBg)
                .windowInsetsPadding(pageInsets)
            Column(videoPageModifier) {
                if (!immersiveVideo) {
                    NowPlayingTopBar(
                        language = language,
                        onPictureInPicture = onPictureInPicture,
                        onHome = onHome,
                        onFullscreen = { fullscreen = true },
                        onClose = onClose,
                    )
                }
                VideoPlayerStage(
                    playback = playback,
                    controller = controller,
                    immersive = immersiveVideo,
                    onVideoBoundsChanged = onVideoBoundsChanged,
                    onHome = onHome,
                    onClose = onClose,
                    onPictureInPicture = onPictureInPicture,
                    onFullscreen = { fullscreen = !fullscreen },
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    seekOffsetMs = seekOffsetMs,
                    onSeekBy = onSeekBy,
                    modifier = if (immersiveVideo) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                            .height(portraitVideoHeight)
                    },
                )
                if (!immersiveVideo) {
                    SecondaryControls(
                        playback = playback.copy(appLanguage = language),
                        queue = queue,
                        lyrics = lyrics,
                        showFileDetails = showFileDetails,
                        editableQueue = editableQueue,
                        language = language,
                        onSpeed = onSpeed,
                        onRepeat = onRepeat,
                        onPrevious = onPrevious,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onSleepTimer = onSleepTimer,
                        sleepTimer = sleepTimer,
                        onPlayQueueItem = onPlayQueueItem,
                        onLoadThumbnail = onLoadThumbnail,
                        onSeek = onSeek,
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        } else {
            AudioPlayer(
                playback = playback.copy(appLanguage = language),
                artwork = artwork,
                queue = queue,
                lyrics = lyrics,
                showFileDetails = showFileDetails,
                editableQueue = editableQueue,
                language = language,
                fullscreen = fullscreen,
                onHome = onHome,
                onClose = onClose,
                onPictureInPicture = onPictureInPicture,
                onFullscreen = { fullscreen = !fullscreen },
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onSpeed = onSpeed,
                onRepeat = onRepeat,
                onSleepTimer = onSleepTimer,
                sleepTimer = sleepTimer,
                seekOffsetMs = seekOffsetMs,
                onSeekBy = onSeekBy,
                onPlayQueueItem = onPlayQueueItem,
                onLoadThumbnail = onLoadThumbnail,
                onLoadWaveform = onLoadWaveform,
                onMoveQueueItem = onMoveQueueItem,
                onRemoveQueueItem = onRemoveQueueItem,
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
    onHome: () -> Unit,
    onClose: () -> Unit,
    onPictureInPicture: () -> Unit,
    onFullscreen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    modifier: Modifier,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var seekFeedback by remember { mutableStateOf(0L to 0L) }
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
        modifier = modifier.background(Color.Black).pointerInput(seekOffsetMs) {
            detectTapGestures(
                onTap = { controlsVisible = !controlsVisible },
                onDoubleTap = { offset ->
                    val delta = if (offset.x < size.width / 2) -seekOffsetMs else seekOffsetMs
                    onSeekBy(delta)
                    seekFeedback = delta to android.os.SystemClock.uptimeMillis()
                },
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        VideoSurface(playback.currentPath, controller, onVideoBoundsChanged, Modifier.fillMaxSize())
        SeekFeedback(seekFeedback.first, seekFeedback.second, Modifier.align(if (seekFeedback.first < 0) Alignment.CenterStart else Alignment.CenterEnd))

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
                if (immersive) {
                    NowPlayingTopBar(
                        language = playback.appLanguage,
                        onPictureInPicture = onPictureInPicture,
                        onHome = onHome,
                        onFullscreen = onFullscreen,
                        onClose = onClose,
                        overlay = true,
                        fullscreen = true,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars),
                    )
                }

                if (immersive) Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayIconButton(onClick = onPrevious, enabled = playback.hasPrevious) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(31.dp))
                    }
                    // Keep the center play control available whenever controls are visible.
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (playback.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp),
                            tint = Color.White.copy(alpha = 0.92f),
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
                if (immersive) Column(
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
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    language: AppLanguage,
    fullscreen: Boolean,
    onHome: () -> Unit,
    onClose: () -> Unit,
    onPictureInPicture: () -> Unit,
    onFullscreen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    seekOffsetMs: Long = 5_000L,
    onSeekBy: (Long) -> Unit,
    onPlayQueueItem: (MediaFile) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onLoadWaveform: suspend (String) -> FloatArray?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
) {
    var waveformLoading by remember(playback.currentPath) { mutableStateOf(true) }
    val waveform by produceState<FloatArray?>(null, playback.currentPath) {
        value = null
        // Playback and artwork get the first frame; stale requests are cancelled by
        // produceState when the user skips rapidly.
        delay(350)
        value = playback.currentPath?.let { onLoadWaveform(it) }
        waveformLoading = false
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        val artSize = minOf(maxWidth * 0.72f, maxHeight * 0.30f)
        var seekFeedback by remember { mutableStateOf(0L to 0L) }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        NowPlayingTopBar(
            language = language,
            onPictureInPicture = onPictureInPicture,
            onHome = onHome,
            onFullscreen = onFullscreen,
            onClose = onClose,
            fullscreen = fullscreen,
        )
        LiquidMetalSurface(
            modifier = Modifier.padding(vertical = 4.dp).size(artSize)
                .pointerInput(seekOffsetMs) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val delta = if (offset.x < size.width / 2) -seekOffsetMs else seekOffsetMs
                            onSeekBy(delta)
                            seekFeedback = delta to android.os.SystemClock.uptimeMillis()
                        },
                    )
                },
            shape = RoundedCornerShape(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = uiText(language, "Album artwork", "專輯封面"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(116.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            SeekFeedback(seekFeedback.first, seekFeedback.second, Modifier.align(if (seekFeedback.first < 0) Alignment.CenterStart else Alignment.CenterEnd))
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                com.local.listentomusic.model.mediaTitle(playback.title, playback.currentPath),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryControlRow(
                                                queue = queue,
                                                onLoadThumbnail = onLoadThumbnail,
                                                playback = playback,
                                                onSleepTimer = onSleepTimer,
                                                sleepTimer = sleepTimer,
                                                language = language,
                                            )
            PlaybackError(playback.errorMessage)
            Spacer(Modifier.height(10.dp))
            NowPlayingQueue(
                queue = queue,
                lyrics = lyrics,
                showFileDetails = showFileDetails,
                editableQueue = editableQueue,
                positionMs = playback.positionMs,
                currentPath = playback.currentPath,
                language = language,
                onPlay = onPlayQueueItem,
                onSeek = onSeek,
                onLoadThumbnail = onLoadThumbnail,
                onMoveQueueItem = onMoveQueueItem,
                onRemoveQueueItem = onRemoveQueueItem,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            WaveformTimeline(playback, waveform, onSeek, language, waveformLoading)
            PlayerBottomControls(playback, onRepeat, onPrevious, onTogglePlay, onNext, onSpeed)
        }
        }
    }
}

@Composable
private fun SecondaryControls(
    playback: PlaybackUiState,
    queue: List<MediaFile>,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    language: AppLanguage,
    onSpeed: (Float) -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    onPlayQueueItem: (MediaFile) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onSeek: (Long) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    modifier: Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            com.local.listentomusic.model.mediaTitle(playback.title, playback.currentPath),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = onSurface,
        )
        Spacer(Modifier.height(12.dp))
        SecondaryControlRow(
                                            queue = queue,
                                            onLoadThumbnail = onLoadThumbnail,
                                            playback = playback,
                                            onSleepTimer = onSleepTimer,
                                            sleepTimer = sleepTimer,
                                            language = language,
                                        )
        PlaybackError(playback.errorMessage)
        Spacer(Modifier.height(12.dp))
        NowPlayingQueue(
            queue = queue,
            lyrics = lyrics,
            showFileDetails = showFileDetails,
            editableQueue = editableQueue,
            positionMs = playback.positionMs,
            currentPath = playback.currentPath,
            language = language,
            onPlay = onPlayQueueItem,
            onSeek = onSeek,
            onLoadThumbnail = onLoadThumbnail,
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Timeline(playback, onSeek)
        PlayerBottomControls(playback, onRepeat, onPrevious, onTogglePlay, onNext, onSpeed)
    }
}

@Composable
private fun NowPlayingQueue(
    queue: List<MediaFile>,
    currentPath: String?,
    language: AppLanguage,
    lyrics: LocalLyrics?,
    showFileDetails: Boolean,
    editableQueue: Boolean,
    positionMs: Long,
    onPlay: (MediaFile) -> Unit,
    onSeek: (Long) -> Unit,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = queue.indexOfFirst { it.path == currentPath }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0),
    )
    LaunchedEffect(currentPath, currentIndex, queue.size) {
        if (currentIndex >= 0) listState.animateScrollToItem(currentIndex)
    }
    Column(modifier) {
        if (lyrics != null) {
            SyncedLyricsPanel(
                lyrics = lyrics,
                positionMs = positionMs,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth().height(118.dp),
            )
            Spacer(Modifier.height(6.dp))
        }
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    uiText(language, "No songs in this list", "這個列表沒有歌曲"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(queue, key = { _, file -> file.path }) { index, file ->
                    val selected = file.path == currentPath
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                else Color.Transparent,
                            )
                            .clickable { onPlay(file) }
                            .padding(horizontal = 7.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QueueThumbnail(file = file, onLoadThumbnail = onLoadThumbnail)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                com.local.listentomusic.model.mediaTitle(file.name, file.sourcePath),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                            )
                            if (showFileDetails) {
                                Text(
                                    queueDetails(file),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (selected) {
                            Box(
                                Modifier.size(7.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                            )
                        }
                        if (editableQueue) {
                            IconButton(onClick = { onMoveQueueItem(index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, uiText(language, "Move up", "上移"))
                            }
                            IconButton(onClick = { onMoveQueueItem(index, index + 1) }, enabled = index < queue.lastIndex) {
                                Icon(Icons.Rounded.KeyboardArrowDown, uiText(language, "Move down", "下移"))
                            }
                            IconButton(onClick = { onRemoveQueueItem(index) }, enabled = queue.size > 1) {
                                Icon(Icons.Rounded.RemoveCircleOutline, uiText(language, "Remove", "移除"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsPanel(
    lyrics: LocalLyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSynced = lyrics.lines.any { it.timeMs > 0L }
    val activeIndex = if (isSynced) lyrics.lines.indexOfLast { it.timeMs <= positionMs } else -1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex.coerceAtLeast(0))
    LaunchedEffect(activeIndex, lyrics.sourcePath) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex, scrollOffset = -24)
    }
    LazyColumn(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(
            lyrics.lines,
            key = { index, line -> "${line.timeMs}:$index" },
        ) { index, line ->
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    if (line.words.isEmpty()) append(line.text) else line.words.forEach { word ->
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = if (index == activeIndex && word.timeMs <= positionMs)
                            MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)))
                        append(word.text); pop()
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = isSynced) { onSeek(line.timeMs) }
                    .padding(vertical = 5.dp),
                color = if (index == activeIndex) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f),
                style = if (index == activeIndex) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,
                fontWeight = if (index == activeIndex) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun queueDetails(file: MediaFile): String = buildList {
    file.sourcePath.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.uppercase()?.let(::add)
    file.durationMs.takeIf { it > 0L }?.let { add(formatDuration(it)) }
    file.sizeBytes.takeIf { it > 0L }?.let { add(formatBytes(it)) }
}.joinToString("  •  ")

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

@Composable
internal fun QueueThumbnail(
    file: MediaFile,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
) {
    val thumbnail by produceState<Bitmap?>(null, file.path, "${file.modifiedMs}:${file.coverUri}") {
        value = onLoadThumbnail(file)
    }
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                if (file.kind == MediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Timeline(playback: PlaybackUiState, onSeek: (Long) -> Unit) {
    var seeking by remember(playback.currentPath) { mutableStateOf(false) }
    var seekPosition by remember(playback.currentPath) { mutableFloatStateOf(0f) }
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
    PracticeMarkers(playback)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(if (seeking) position.toLong() else playback.positionMs), style = MaterialTheme.typography.labelMedium)
        Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformTimeline(
    playback: PlaybackUiState,
    waveform: FloatArray?,
    onSeek: (Long) -> Unit,
    language: AppLanguage,
    loading: Boolean,
) {
    var seeking by remember(playback.currentPath) { mutableStateOf(false) }
    var seekFraction by remember(playback.currentPath) { mutableFloatStateOf(0f) }
    val hasDuration = playback.durationMs > 0L
    // Keep the Slider in a stable 0..1 range. Feeding millisecond-sized Float
    // ranges into Material Slider can lose precision on long audio and leave the
    // thumb visually pinned at the end even while playback is healthy.
    val playbackFraction = if (hasDuration) {
        playback.positionMs.toDouble().div(playback.durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(playbackFraction, androidx.compose.animation.core.tween(if (playback.isPlaying) 450 else 0), label = "wave-progress")
    val fraction = if (seeking) seekFraction else animatedProgress
    val active = MaterialTheme.colorScheme.secondary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val displayPeaks = remember(waveform) {
        waveform?.takeIf { it.isNotEmpty() }?.let { raw ->
            // Fixed visible count prevents thousands of tiny lines on a phone.
            val groups = minOf(80, raw.size)
            val peaks = FloatArray(groups) { g ->
                val first = g * raw.size / groups
                val last = ((g + 1) * raw.size / groups).coerceAtMost(raw.size)
                (first until last).maxOfOrNull { raw[it].takeIf(Float::isFinite) ?: 0f } ?: 0f
            }
            // Repository already normalizes PCM. Normalizing it again flattened
            // most bars at 1.0, destroying the real shape of the recording.
            peaks.map { it.coerceIn(0f, 1f) }.toFloatArray()
        }
    }

    Slider(
            value = fraction,
            onValueChange = { seeking = true; seekFraction = it.coerceIn(0f, 1f) },
            onValueChangeFinished = {
                if (hasDuration) {
                    onSeek((playback.durationMs.toDouble() * seekFraction).toLong())
                }
                seeking = false
            },
            valueRange = 0f..1f,
            enabled = hasDuration,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            thumb = {},
        track = {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val bars = displayPeaks?.size ?: 72
                val spacing = size.width / bars
                repeat(bars) { index ->
                    val wave = displayPeaks?.getOrNull(index) ?: 0.025f
                    val barHeight = size.height * (0.06f + wave * 0.90f)
                    val x = spacing * (index + 0.5f)
                    drawLine(
                        color = if ((index + 1f) / bars <= fraction) active else inactive,
                        start = Offset(x, (size.height - barHeight) / 2f),
                        end = Offset(x, (size.height + barHeight) / 2f),
                        strokeWidth = 2.25.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                if (hasDuration) {
                    val x = size.width * fraction
                    drawLine(active.copy(alpha = 0.18f), Offset(x, 0f), Offset(x, size.height), 8.dp.toPx())
                    drawLine(active, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                }
            }
        },
    )
    PracticeMarkers(playback)
    if (waveform == null) Text(
        if (loading) uiText(language, "Waveform is being prepared; seeking is ready", "正在準備波形，仍可拖曳播放位置")
        else uiText(language, "Waveform unavailable; seeking still works", "無法讀取波形，仍可拖曳播放位置"),
        style = MaterialTheme.typography.labelSmall, color = inactive,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatDuration(
                if (seeking && hasDuration) {
                    (playback.durationMs.toDouble() * seekFraction).toLong()
                } else {
                    playback.positionMs
                },
            ),
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
    // Spring-animate the fill so dragging feels fluid rather than stepped.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 340f),
        label = "sliderFill",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(22.dp),
        thumb = { _ ->
            // Soft glow and radial-gradient core keep the thumb visible without visual bulk.
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(activeColor.copy(alpha = 0.22f)))
                Box(
                    Modifier.size(13.dp).clip(CircleShape).background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.95f), activeColor.copy(alpha = 0.55f))
                        )
                    )
                )
            }
        },
        track = { _ ->
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(inactiveColor.copy(alpha = 0.4f))
            ) {
                Box(
                    Modifier.fillMaxWidth(animatedFraction).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(activeColor.copy(alpha = 0.55f), activeColor)
                            )
                        )
                )
            }
        },
    )
}

@Composable
private fun PlayerBottomControls(
    playback: PlaybackUiState,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSpeed: (Float) -> Unit,
) {
    var speedMenuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.secondary
    val cycleLabel = when {
        playback.shuffleEnabled -> uiText(playback.appLanguage, "Random", "隨機")
        playback.repeatMode == Player.REPEAT_MODE_ONE -> uiText(playback.appLanguage, "One", "單曲")
        playback.repeatMode == Player.REPEAT_MODE_ALL -> uiText(playback.appLanguage, "All", "全部")
        else -> uiText(playback.appLanguage, "Off", "關閉")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onRepeat, modifier = Modifier.size(48.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(when {
                    playback.shuffleEnabled -> Icons.Rounded.Shuffle
                    playback.repeatMode == Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }, uiText(playback.appLanguage, "Repeat mode", "重複模式"), Modifier.size(20.dp), tint = accent)
                Text(cycleLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        IconButton(onClick = onPrevious, enabled = playback.hasPrevious || playback.positionMs > 4_000L) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(36.dp))
        }
        LiquidMetalSurface(
                    modifier = Modifier.size(52.dp).clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onTogglePlay
                    ),
            shape = CircleShape,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playback.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onNext, enabled = playback.hasNext) {
            Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(36.dp))
        }
        Box {
                    val speedIcon = when {
                        playback.speed <= 0.5f -> Icons.Rounded.KeyboardArrowUp
                        playback.speed <= 1.5f -> Icons.Rounded.KeyboardArrowUp
                        playback.speed <= 2f -> Icons.AutoMirrored.Rounded.ArrowForward
                        else -> Icons.Rounded.KeyboardArrowDown
                    }
                    IconButton(onClick = { speedMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(speedIcon, uiText(playback.appLanguage, "Playback speed", "播放速度"), Modifier.size(20.dp), tint = accent)
                            Text(speedLabel(playback.speed), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
            DropdownMenu(speedMenuOpen, { speedMenuOpen = false }) {
                playbackSpeeds.forEach { speed -> DropdownMenuItem(
                    text = { Text(if (speed == playback.speed) "✓  ${speedLabel(speed)}" else speedLabel(speed)) },
                    onClick = { speedMenuOpen = false; onSpeed(speed) },
                ) }
            }
        }
    }
}

@Composable
private fun SecondaryControlRow(
    queue: List<MediaFile>,
    onLoadThumbnail: suspend (MediaFile) -> Bitmap?,
    playback: PlaybackUiState,
    onSleepTimer: (Long) -> Unit,
    sleepTimer: SleepTimerState,
    language: AppLanguage,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val practice by com.local.listentomusic.playback.PracticeLoop.state.collectAsState()
    var sleepMenuOpen by remember { mutableStateOf(false) }
    var showMixerDialog by remember { mutableStateOf(false) }
    val outline = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.secondary
    val controlColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
    val sleepLabel = when {
        sleepTimer.endOfTrack -> uiText(playback.appLanguage, "End", "播完")
        sleepTimer.active -> formatSleepRemaining(sleepTimer.remainingMs)
        else -> uiText(playback.appLanguage, "Sleep", "睡眠")
    }
    var mixExpanded by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = { mixExpanded = !mixExpanded }, modifier = Modifier.width(32.dp)) {
        Icon(if (mixExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.ArrowForward,
            uiText(playback.appLanguage, "Mix", "混音"), modifier = Modifier.size(20.dp))
    }
    AnimatedVisibility(mixExpanded, modifier = Modifier.weight(1f)) {
    Button(onClick = { showMixerDialog = true }, modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = controlColors, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 7.dp)) {
        Text(uiText(playback.appLanguage, "Mix", "混音"), style = MaterialTheme.typography.labelMedium)
    }
    }
    if (showMixerDialog) ParallelMixerDialog(queue, language, onLoadThumbnail) { showMixerDialog = false }
    IconButton(onClick = { optionsExpanded = !optionsExpanded }, modifier = Modifier.width(32.dp)) {
        Icon(if (optionsExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.ArrowForward,
            uiText(playback.appLanguage, "Playback options", "播放選項"), modifier = Modifier.size(20.dp))
    }
    AnimatedVisibility(optionsExpanded, modifier = Modifier.weight(1f)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playback.showAbRepeat) Button(
            onClick = { com.local.listentomusic.playback.PracticeLoop.mark(playback.currentPath, playback.positionMs) },
            modifier = Modifier.weight(1f).height(40.dp), colors = controlColors,
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(when { practice.end != null -> "A–B ×"; practice.start != null -> "Set B"; else -> "Set A" }, style = MaterialTheme.typography.labelMedium)
        }
        if (playback.showSleepControl) Box(Modifier.weight(1f)) {
            Button(
                onClick = { sleepMenuOpen = true },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = controlColors,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 7.dp),
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    null,
                    tint = if (sleepTimer.active) activeColor else outline,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(sleepLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(expanded = sleepMenuOpen, onDismissRequest = { sleepMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(uiText(playback.appLanguage, "Off", "關閉")) },
                    onClick = { sleepMenuOpen = false; onSleepTimer(0L) },
                )
                sleepTimerOptions.forEach { minutes ->
                    DropdownMenuItem(
                        text = {
                            val text = when (minutes) {
                                -1L -> uiText(playback.appLanguage, "End of track", "播完這首")
                                else -> uiText(playback.appLanguage, "$minutes min", "$minutes 分鐘")
                            }
                            Text(text)
                        },
                        onClick = { sleepMenuOpen = false; onSleepTimer(minutes) },
                    )
                }
            }
        }
    }
    }
    }
}

@Composable
private fun PracticeMarkers(playback: PlaybackUiState) {
    val range by com.local.listentomusic.playback.PracticeLoop.state.collectAsState()
    if (!playback.showAbRepeat || range.path != playback.currentPath || range.start == null || playback.durationMs <= 0) return
    val color = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxWidth().height(18.dp).padding(horizontal = 10.dp)) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); textSize = 10.sp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD }
        listOf("A" to range.start, "B" to range.end).forEach { (label, time) ->
            if (time != null) {
                val x = (time.toDouble() / playback.durationMs).toFloat().coerceIn(0f, 1f) * size.width
                drawLine(color, Offset(x, 0f), Offset(x, 5.dp.toPx()), 2.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(label, (x - paint.measureText(label) / 2).coerceIn(0f, (size.width - paint.measureText(label)).coerceAtLeast(0f)), size.height - 1.dp.toPx(), paint)
            }
        }
    }
}

private fun formatSleepRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun PlaybackError(message: String?) {
    if (message != null) {
        Spacer(Modifier.height(14.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NowPlayingTopBar(
    language: AppLanguage,
    onPictureInPicture: () -> Unit,
    onHome: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
    fullscreen: Boolean = false,
) {
    val foreground = if (overlay) Color.White else MaterialTheme.colorScheme.onSurface
    val background = if (overlay) Color.Black.copy(alpha = 0.34f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    Row(
        modifier = modifier.fillMaxWidth().height(54.dp).background(background)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPictureInPicture) {
            Icon(
                Icons.Rounded.PictureInPictureAlt,
                uiText(language, "Open floating player", "開啟浮動播放器"),
                tint = foreground,
            )
        }
        IconButton(onClick = onHome) {
            Icon(Icons.Rounded.Home, uiText(language, "Home", "首頁"), tint = foreground)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onFullscreen) {
            Icon(
                if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                if (fullscreen) uiText(language, "Exit fullscreen", "離開全螢幕")
                    else uiText(language, "Fullscreen", "全螢幕"),
                tint = foreground,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, uiText(language, "Close player", "關閉播放器"), tint = foreground)
        }
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
internal fun VideoSurface(
    mediaKey: String?,
    controller: MediaController?,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
) {
    key(mediaKey, controller) {
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
                com.local.listentomusic.ui.components.VideoSurfaceOwner.attach(controller, this)
            }
        },
        update = { view -> com.local.listentomusic.ui.components.VideoSurfaceOwner.attach(controller, view) },
        onRelease = com.local.listentomusic.ui.components.VideoSurfaceOwner::detach,
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
}

@Composable
private fun FullscreenEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    val systemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    DisposableEffect(activity, enabled, systemDark) {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (enabled) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insets.isAppearanceLightStatusBars = !systemDark
            insets.isAppearanceLightNavigationBars = !systemDark
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

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "$speed×"
