package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.BackgroundScaleMode
import com.local.listentomusic.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

internal fun shouldAttachVideoBackground(
    visible: Boolean,
    primaryIsVideo: Boolean,
    primaryFrameReady: Boolean,
): Boolean = visible && (!primaryIsVideo || primaryFrameReady)

internal fun shouldMirrorPrimaryPlayback(
    lifecycleActive: Boolean,
    primaryIsPlaying: Boolean,
): Boolean = lifecycleActive && primaryIsPlaying

internal fun shouldPreferSoftwareBackgroundDecoder(primaryIsVideo: Boolean): Boolean = primaryIsVideo
@Composable
fun AppBackground(
    preferences: UserPreferences,
    currentPath: String?,
    isVideo: Boolean,
    controller: MediaController?,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val mode = preferences.backgroundMode
    val currentVideoUri = currentPath
        ?.takeIf { isVideo }
        ?.let { Uri.fromFile(File(it)) }
    var primaryFrameReady by remember(controller, currentPath, isVideo) { mutableStateOf(!isVideo) }

    // A video wallpaper is a secondary consumer. Let the real MediaController render first
    // so Library/Now Playing gets the primary decoder and surface before we allocate another.
    DisposableEffect(controller, currentPath, isVideo) {
        val primary = controller
        primaryFrameReady = !isVideo
        if (!isVideo || primary == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    primaryFrameReady = true
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    primaryFrameReady = false
                }
            }
            primary.addListener(listener)
            onDispose { primary.removeListener(listener) }
        }
    }

    // onRenderedFirstFrame can race listener registration during session reconnection. Give
    // the foreground view a short head start, then accept an already-ready matching video.
    LaunchedEffect(controller, currentPath, isVideo, visible, primaryFrameReady) {
        val primary = controller ?: return@LaunchedEffect
        if (!visible || !isVideo || primaryFrameReady) return@LaunchedEffect
        delay(PRIMARY_VIDEO_HEAD_START_MS)
        if (
            primary.currentMediaItem?.mediaId == currentPath &&
            primary.playbackState == Player.STATE_READY &&
            primary.videoSize.width > 0 &&
            primary.videoSize.height > 0
        ) {
            primaryFrameReady = true
        }
    }

    val attachVideoBackground = shouldAttachVideoBackground(
        visible = visible,
        primaryIsVideo = isVideo,
        primaryFrameReady = primaryFrameReady,
    )
    val preferSoftwareBackgroundDecoder = shouldPreferSoftwareBackgroundDecoder(isVideo)

    Box(modifier.fillMaxSize().graphicsLayer()) {
        // Avoid an animated full-screen metal pass underneath opaque media wallpaper.
        if (visible && (mode == AppBackgroundMode.DEFAULT || mode == AppBackgroundMode.CURRENT_VIDEO && currentVideoUri == null)) DefaultMetalBackground()
        else Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background))
        when (mode) {
            AppBackgroundMode.DEFAULT -> Unit
            AppBackgroundMode.CUSTOM_IMAGE -> preferences.customBackgroundImageUri
                ?.let(Uri::parse)
                ?.let { BackgroundImage(it, preferences.backgroundScaleMode) }
            AppBackgroundMode.CUSTOM_VIDEO -> if (attachVideoBackground) {
                preferences.customBackgroundVideoUri
                    ?.let(Uri::parse)
                    ?.let {
                        BackgroundVideo(
                            source = it,
                            shouldPlay = true,
                            scaleMode = preferences.backgroundScaleMode,
                            preferSoftwareDecoder = preferSoftwareBackgroundDecoder,
                        )
                    }
            }
            // Keep a separate muted decoder so the wallpaper never steals the primary
            // Media3 surface. When the foreground item is also video, prefer a software
            // decoder for this secondary player so Library/Now Playing keeps hardware first.
            AppBackgroundMode.CURRENT_VIDEO -> if (currentVideoUri != null && attachVideoBackground) {
                BackgroundVideo(
                    source = currentVideoUri,
                    shouldPlay = true,
                    syncController = controller,
                    scaleMode = preferences.backgroundScaleMode,
                    preferSoftwareDecoder = true,
                )
            }
        }
        val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
        val dim = if (mode == AppBackgroundMode.DEFAULT) 0.08f else preferences.backgroundDim
        val veil = if (mode == AppBackgroundMode.DEFAULT && isLight) Color.White else Color.Black
        Box(Modifier.matchParentSize().background(veil.copy(alpha = dim)))
    }
}

@Composable
private fun DefaultMetalBackground() {
    val isLight = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
    LiquidMetalSurface(
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
        baseColor = if (isLight) Color(0xFFF2F4F2) else Color(0xFF080A09),
        accentColor = if (isLight) Color(0xFF94BFB5) else Color(0xFF72D7C0),
    ) {
        // Quiet technical grid inspired by editorial motion graphics. It stays
        // subordinate to content and costs no per-frame layout work.
        Canvas(Modifier.matchParentSize()) {
            val line = if (isLight) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.045f)
            repeat(6) { index ->
                val x = size.width * index / 5f
                drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }
            repeat(9) { index ->
                val y = size.height * index / 8f
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    if (isLight) listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color(0xFFD7DFDC).copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.70f),
                    ) else listOf(
                        Color(0xFF07100E).copy(alpha = 0.70f),
                        Color(0xFF0B0D0C).copy(alpha = 0.34f),
                        Color.Black.copy(alpha = 0.58f),
                    ),
                ),
            ),
        )
    }
}

@Composable
private fun BackgroundImage(source: Uri, scaleMode: BackgroundScaleMode) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = source) {
        // Do not leave the previous image visible if replacement decoding fails.
        value = null
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(context.contentResolver, source)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = when (scaleMode) {
                BackgroundScaleMode.FIT -> ContentScale.Fit
                BackgroundScaleMode.STRETCH -> ContentScale.FillBounds
                BackgroundScaleMode.CROP -> ContentScale.Crop
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BackgroundVideo(
    source: Uri,
    shouldPlay: Boolean,
    scaleMode: BackgroundScaleMode = BackgroundScaleMode.CROP,
    syncController: MediaController? = null,
    preferSoftwareDecoder: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    val backgroundPlayer = remember(source, preferSoftwareDecoder) {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)
            .apply {
                if (preferSoftwareDecoder) {
                    setMediaCodecSelector(MediaCodecSelector.PREFER_SOFTWARE)
                }
            }
        ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_000, 5_000, 100, 200).setTargetBufferBytes(4 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false).build()).build().apply {
            addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    Log.i(
                        BACKGROUND_VIDEO_TAG,
                        "decoder=$decoderName preferSoftware=$preferSoftwareDecoder source=${source.lastPathSegment}",
                    )
                }

                override fun onVideoCodecError(
                    eventTime: AnalyticsListener.EventTime,
                    videoCodecError: Exception,
                ) {
                    Log.w(
                        BACKGROUND_VIDEO_TAG,
                        "codecError preferSoftware=$preferSoftwareDecoder source=${source.lastPathSegment}",
                        videoCodecError,
                    )
                }
            })
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setMaxVideoSize(640, 360)
                .build()
            setMediaItem(MediaItem.fromUri(source))
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> lifecycleActive = true
                Lifecycle.Event.ON_STOP -> lifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(backgroundPlayer) {
        onDispose { backgroundPlayer.release() }
    }

    LaunchedEffect(backgroundPlayer, shouldPlay, lifecycleActive, syncController) {
        val primary = syncController
        if (!shouldPlay || !lifecycleActive) {
            backgroundPlayer.playWhenReady = false
            return@LaunchedEffect
        }
        if (primary == null) {
            backgroundPlayer.playWhenReady = true
            return@LaunchedEffect
        }

        while (true) {
            val mirrorPlaying = shouldMirrorPrimaryPlayback(
                lifecycleActive = lifecycleActive,
                primaryIsPlaying = primary.isPlaying,
            )
            if (!mirrorPlaying) backgroundPlayer.playWhenReady = false

            val primarySpeed = primary.playbackParameters.speed
            if (abs(backgroundPlayer.playbackParameters.speed - primarySpeed) > 0.001f) {
                backgroundPlayer.setPlaybackSpeed(primarySpeed)
            }

            val target = primary.currentPosition.coerceAtLeast(0L)
            val tolerance = if (mirrorPlaying) PLAYING_SYNC_TOLERANCE_MS else PAUSED_SYNC_TOLERANCE_MS
            if (abs(backgroundPlayer.currentPosition - target) > tolerance) {
                backgroundPlayer.seekTo(target)
            }
            backgroundPlayer.playWhenReady = mirrorPlaying
            delay(BACKGROUND_SYNC_INTERVAL_MS)
        }
    }

    AndroidView(
        factory = { viewContext ->
            (android.view.LayoutInflater.from(viewContext).inflate(com.local.listentomusic.R.layout.background_video, android.widget.FrameLayout(viewContext), false) as PlayerView).apply {
                useController = false
                this.resizeMode = when (scaleMode) {
                    BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
                setKeepContentOnPlayerReset(true)
                player = backgroundPlayer
            }
        },
        update = {
            it.resizeMode = when (scaleMode) {
                BackgroundScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                BackgroundScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                BackgroundScaleMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            it.player = backgroundPlayer
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun decodeSampledBitmap(
    resolver: android.content.ContentResolver,
    source: Uri,
): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val longestSide = max(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (longestSide / sampleSize > MAX_BACKGROUND_PIXELS) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

private const val BACKGROUND_VIDEO_TAG = "GreaterArtBackground"
private const val PRIMARY_VIDEO_HEAD_START_MS = 300L
private const val BACKGROUND_SYNC_INTERVAL_MS = 250L
private const val PLAYING_SYNC_TOLERANCE_MS = 350L
private const val PAUSED_SYNC_TOLERANCE_MS = 80L
private const val MAX_BACKGROUND_PIXELS = 1_600
