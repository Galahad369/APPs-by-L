package com.local.listentomusic

import android.Manifest
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.local.listentomusic.ui.GreaterArtApp
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.playback.MiniWindowOverlayService
import com.local.listentomusic.playback.PlaybackService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var isPictureInPicture by mutableStateOf(false)
    private var openPlayerRequest by mutableIntStateOf(0)
    private var playerScreenVisible = false
    private var videoSourceRect = Rect()
    // The fallback receiver lives on MainActivity so it catches the broadcast
    // even if the Composable isn't composed yet (e.g., service starts in background).
    private var fallbackReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Keep the display awake only while this Activity is visible. Android still
        // honors the physical power/lock key, and no wake lock survives the Activity.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (handleStopIntent(intent)) return
        handleOpenPlayerIntent(intent)
        // Register fallback receiver early
        fallbackReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                viewModel.setFloatingWindowMode(FloatingWindowMode.COMPACT)
            }
        }
        ContextCompat.registerReceiver(
            this,
            fallbackReceiver,
            IntentFilter(MiniWindowOverlayService.ACTION_FALLBACK_COMPACT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
                openPlayerRequest = openPlayerRequest,
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
        // The Activity owns the foreground UI. Never leave a stale overlay above it.
        stopService(Intent(this, MiniWindowOverlayService::class.java))
        viewModel.rescan()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleStopIntent(intent)) handleOpenPlayerIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val playback = viewModel.playback.value
        val settings = viewModel.settings.value
        if (
            playback.hasMedia &&
            settings.autoPictureInPicture &&
            settings.floatingWindowMode == FloatingWindowMode.MINI_WINDOW
        ) {
            startMiniWindowIfAllowed()
            return
        }
        if (
            playerScreenVisible &&
            playback.isVideo &&
            playback.isPlaying &&
            settings.autoPictureInPicture &&
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
            viewModel.settings.value.autoPictureInPicture &&
            viewModel.settings.value.floatingWindowMode != FloatingWindowMode.MINI_WINDOW
        setPictureInPictureParams(buildPictureInPictureParams(autoEnter))
    }

    private fun enterVideoPictureInPicture() {
        val playback = viewModel.playback.value
        if (!playback.hasMedia || isInPictureInPictureMode) return
        if (
            !playback.isVideo ||
            viewModel.settings.value.floatingWindowMode == FloatingWindowMode.MINI_WINDOW
        ) {
            if (startMiniWindowIfAllowed(openSettingsWhenMissing = true)) {
                moveTaskToBack(true)
            }
            return
        }
        if (!playerScreenVisible || !playback.isVideo) return
        updatePictureInPictureParams()
        runCatching {
            enterPictureInPictureMode(buildPictureInPictureParams(autoEnter = false))
        }
    }

    private fun startMiniWindowIfAllowed(openSettingsWhenMissing: Boolean = false): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (openSettingsWhenMissing) {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:$packageName".toUri(),
                        ),
                    )
                }
            } else {
                viewModel.setFloatingWindowMode(FloatingWindowMode.COMPACT)
            }
            return false
        }
        return runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, MiniWindowOverlayService::class.java),
            )
            true
        }.getOrElse {
            viewModel.setFloatingWindowMode(FloatingWindowMode.COMPACT)
            false
        }
    }

    private fun handleStopIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(MiniWindowOverlayService.EXTRA_STOP_APP, false) != true) {
            return false
        }
        intent.removeExtra(MiniWindowOverlayService.EXTRA_STOP_APP)
        stopService(Intent(this, MiniWindowOverlayService::class.java))
        stopService(Intent(this, PlaybackService::class.java))
        finishAndRemoveTask()
        return true
    }

    private fun handleOpenPlayerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(MiniWindowOverlayService.EXTRA_OPEN_PLAYER, false) != true) return
        intent.removeExtra(MiniWindowOverlayService.EXTRA_OPEN_PLAYER)
        openPlayerRequest++
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

    override fun onDestroy() {
        fallbackReceiver?.let { receiver -> runCatching { unregisterReceiver(receiver) } }
        fallbackReceiver = null
        super.onDestroy()
    }
}

@Composable
private fun PermissionAwareApp(
    viewModel: MainViewModel,
    openPlayerRequest: Int,
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
        openPlayerRequest = openPlayerRequest,
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
