package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.listentomusic.PlaybackUiState
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max

@Composable
fun AppBackground(
    preferences: UserPreferences,
    playback: PlaybackUiState,
    controller: MediaController?,
    modifier: Modifier = Modifier,
) {
    val mode = preferences.backgroundMode
    val currentVideoUri = playback.currentPath
        ?.takeIf { playback.isVideo }
        ?.let { Uri.fromFile(File(it)) }

    Box(modifier.fillMaxSize()) {
        DefaultMetalBackground()
        when (mode) {
            AppBackgroundMode.DEFAULT -> Unit
            AppBackgroundMode.CUSTOM_IMAGE -> preferences.customBackgroundImageUri
                ?.let(Uri::parse)
                ?.let { BackgroundImage(it) }
            AppBackgroundMode.CUSTOM_VIDEO -> preferences.customBackgroundVideoUri
                ?.let(Uri::parse)
                ?.let { BackgroundVideo(source = it, shouldPlay = true) }
            AppBackgroundMode.CURRENT_VIDEO -> currentVideoUri?.let {
                BackgroundVideo(
                    source = it,
                    shouldPlay = playback.isPlaying,
                    syncPositionMs = playback.positionMs,
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
private fun BackgroundImage(source: Uri) {
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
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BackgroundVideo(
    source: Uri,
    shouldPlay: Boolean,
    syncPositionMs: Long? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    val backgroundPlayer = remember(source) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
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
    LaunchedEffect(backgroundPlayer, shouldPlay, lifecycleActive) {
        backgroundPlayer.playWhenReady = shouldPlay && lifecycleActive
    }
    LaunchedEffect(backgroundPlayer, syncPositionMs) {
        val target = syncPositionMs ?: return@LaunchedEffect
        if (abs(backgroundPlayer.currentPosition - target) > 2_000L) backgroundPlayer.seekTo(target)
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setKeepContentOnPlayerReset(true)
                player = backgroundPlayer
            }
        },
        update = { it.player = backgroundPlayer },
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

private const val MAX_BACKGROUND_PIXELS = 1_600
