package com.local.listentomusic

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.local.listentomusic.ui.GreaterArtApp
import com.local.listentomusic.data.FloatingWindowMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var isPictureInPicture by mutableStateOf(false)
    private var playerScreenVisible = false
    private var videoSourceRect = Rect()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playback, viewModel.settings) { playback, settings ->
                        listOf(
                            playback.isVideo,
                            playback.isPlaying,
                            (playback.videoAspectRatio * 100).roundToInt(),
                            settings.floatingWindowMode,
                        )
                    }
                    .distinctUntilChanged()
                    .collect { updatePictureInPictureParams() }
            }
        }
        setContent {
            PermissionAwareApp(
                viewModel = viewModel,
                isPictureInPicture = isPictureInPicture,
                onPlayerScreenChanged = {
                    playerScreenVisible = it
                    updatePictureInPictureParams()
                },
                onVideoBoundsChanged = {
                    if (videoSourceRect != it) {
                        videoSourceRect = Rect(it)
                        updatePictureInPictureParams()
                    }
                },
                onEnterPictureInPicture = ::enterVideoPictureInPicture,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.rescan()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val playback = viewModel.playback.value
        if (
            playerScreenVisible &&
            playback.isVideo &&
            playback.isPlaying &&
            viewModel.settings.value.autoPictureInPicture &&
            !isInPictureInPictureMode
        ) {
            updatePictureInPictureParams()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = false))
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPictureInPicture = isInPictureInPictureMode
    }

    private fun updatePictureInPictureParams() {
        val playback = viewModel.playback.value
        val autoEnter = playerScreenVisible && playback.isVideo && playback.isPlaying &&
            viewModel.settings.value.autoPictureInPicture
        setPictureInPictureParams(buildPictureInPictureParams(autoEnter))
    }

    private fun enterVideoPictureInPicture() {
        val playback = viewModel.playback.value
        if (!playerScreenVisible || !playback.isVideo || isInPictureInPictureMode) return
        updatePictureInPictureParams()
        runCatching {
            enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = false))
        }
    }

    private fun buildPictureInPictureParams(autoEnter: Boolean): PictureInPictureParams {
        val ratio = when (viewModel.settings.value.floatingWindowMode) {
            FloatingWindowMode.COMPACT -> 16f / 9f
            FloatingWindowMode.FOLLOW_VIDEO ->
                viewModel.playback.value.videoAspectRatio.coerceIn(0.5f, 2.0f)
            FloatingWindowMode.MINI_WINDOW -> 16f / 9f
        }
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1_000).toInt(), 1_000))
            .apply {
                if (!videoSourceRect.isEmpty) setSourceRectHint(videoSourceRect)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(autoEnter)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }
}

@Composable
private fun PermissionAwareApp(
    viewModel: MainViewModel,
    isPictureInPicture: Boolean,
    onPlayerScreenChanged: (Boolean) -> Unit,
    onVideoBoundsChanged: (Rect) -> Unit,
    onEnterPictureInPicture: () -> Unit,
) {
    val context = LocalContext.current
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.rescan() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playback still works if notification permission is declined. */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    GreaterArtApp(
        viewModel = viewModel,
        isPictureInPicture = isPictureInPicture,
        onPlayerScreenChanged = onPlayerScreenChanged,
        onVideoBoundsChanged = onVideoBoundsChanged,
        onEnterPictureInPicture = onEnterPictureInPicture,
        onGrantStorageAccess = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appSpecific = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
                runCatching { context.startActivity(appSpecific) }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
    )
}
