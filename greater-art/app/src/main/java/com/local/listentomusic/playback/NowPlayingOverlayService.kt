package com.local.listentomusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.local.listentomusic.MainViewModel
import com.local.listentomusic.R
import com.local.listentomusic.ui.AndroidShare
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Focusable system-level Now Playing surface.
 *
 * MainActivity remains the Library host underneath. The player UI itself lives in a
 * TYPE_APPLICATION_OVERLAY window, so Now Playing can stay visible when the Activity
 * is covered or the user switches apps. The overlay owns presentation only; playback
 * remains in PlaybackService.
 */
class NowPlayingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val onBackPressedDispatcher = OnBackPressedDispatcher { stopSelf() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var viewModel: MainViewModel
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var launchedFromMini = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[MainViewModel::class.java]

        VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
        VideoSurfaceOwner.setSystemOverlayVisible(OVERLAY_TOKEN, "NOW_PLAYING", true)

        windowManager = getSystemService(WindowManager::class.java)
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeOnBackPressedDispatcherOwner(this)
        composeView = view

        view.setContent {
            val playback by viewModel.playback.collectAsState()
            val queue by viewModel.queue.collectAsState()
            val library by viewModel.library.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val controller by viewModel.controller.collectAsState()
            val sleepTimer by viewModel.sleepTimer.collectAsState()

            val artwork by produceState<android.graphics.Bitmap?>(
                initialValue = null,
                key1 = playback.currentPath,
                key2 = settings.localOverrides[playback.currentPath],
            ) {
                value = viewModel.loadCurrentArtwork(playback.currentPath)
            }
            val lyrics by produceState<com.local.listentomusic.model.LocalLyrics?>(
                initialValue = null,
                key1 = playback.currentPath,
            ) {
                value = viewModel.loadLyrics(playback.currentPath)
            }

            GreaterArtTheme(
                themeMode = settings.themeMode,
                appFont = settings.appFont,
                silianRail = settings.silianRail,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    tonalElevation = 4.dp,
                ) {
                    NowPlayingScreen(
                        playback = playback,
                        artwork = artwork,
                        queue = queue,
                        lyrics = lyrics,
                        showFileDetails = settings.showFileDetails,
                        editableQueue = settings.editableQueue,
                        blackDiscMode = settings.blackDiscMode,
                        language = settings.appLanguage,
                        controller = controller,
                        contentPadding = PaddingValues(0.dp),
                        isPictureInPicture = false,
                        onVideoBoundsChanged = {},
                        onPictureInPicture = ::shrinkToMini,
                        onHome = ::returnToLibrary,
                        onClose = ::stopSelf,
                        onTogglePlay = viewModel::togglePlayPause,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onSeek = viewModel::seekTo,
                        onSpeed = viewModel::setSpeed,
                        onRepeat = viewModel::cycleRepeatMode,
                        onSleepTimer = viewModel::setSleepTimer,
                        sleepTimer = sleepTimer,
                        seekOffsetMs = settings.seekOffsetMs,
                        onSeekBy = viewModel::seekBy,
                        onPlayQueueItem = viewModel::playQueueItem,
                        onLoadThumbnail = viewModel::loadThumbnail,
                        onLoadWaveform = viewModel::loadWaveform,
                        onMoveQueueItem = viewModel::moveQueueItem,
                        onRemoveQueueItem = viewModel::removeQueueItem,
                        onBeginTemporaryDoubleSpeed = viewModel::beginTemporaryDoubleSpeed,
                        onEndTemporaryDoubleSpeed = viewModel::endTemporaryDoubleSpeed,
                        isFavourite = playback.currentPath in settings.favouritePaths,
                        onToggleFavourite = viewModel::toggleFavourite,
                        onShareCurrentMedia = {
                            val current = queue.firstOrNull { it.path == playback.currentPath }
                                ?: library.files.firstOrNull { it.path == playback.currentPath }
                            if (current != null) {
                                AndroidShare.media(
                                    this@NowPlayingOverlayService,
                                    current,
                                    uiText(settings.appLanguage, "Share media file", "分享媒體檔案"),
                                )
                            }
                        },
                        onShareQueue = {
                            scope.launch {
                                AndroidShare.list(
                                    this@NowPlayingOverlayService,
                                    uiText(settings.appLanguage, "Current queue", "目前播放佇列"),
                                    queue,
                                    uiText(settings.appLanguage, "Share current queue", "分享目前播放佇列"),
                                )
                            }
                        },
                    )
                }
            }
        }

        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics?.bounds
        } else null
        val width = bounds?.width() ?: resources.displayMetrics.widthPixels
        val height = bounds?.height() ?: resources.displayMetrics.heightPixels
        windowParams = WindowManager.LayoutParams(
            minOf((width - dp(16)).coerceAtLeast(dp(280)), dp(720)),
            minOf((height - dp(32)).coerceAtLeast(dp(420)), dp(1000)),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.34f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            windowManager?.addView(view, windowParams)
        } catch (_: Throwable) {
            stopSelf()
            return
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        scope.launch {
            val playback = viewModel.playback.filter { it.connected }.first()
            if (playback.isVideo) {
                withTimeoutOrNull(HANDOFF_TIMEOUT_MS) {
                    VideoSurfaceOwner.state.first {
                        it.owner == "NOW_PLAYING" && it.firstFrame
                    }
                }
            }
            VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            if (launchedFromMini) {
                stopService(Intent(this@NowPlayingOverlayService, MiniWindowOverlayService::class.java))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        launchedFromMini = launchedFromMini ||
            intent?.getBooleanExtra(EXTRA_FROM_MINI_WINDOW, false) == true
        return START_NOT_STICKY
    }

    private fun returnToLibrary() {
        runCatching {
            startActivity(
                Intent(this, com.local.listentomusic.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                },
            )
        }
        stopSelf()
    }

    private fun shrinkToMini() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, MiniWindowOverlayService::class.java),
            )
        }.onFailure {
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            return
        }

        scope.launch {
            val video = viewModel.playback.value.isVideo
            if (video) {
                withTimeoutOrNull(HANDOFF_TIMEOUT_MS) {
                    VideoSurfaceOwner.state.first {
                        it.owner == "MINI_WINDOW" && it.firstFrame
                    }
                }
            }
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            stopSelf()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Greater Art playback",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Now Playing is floating")
            .setContentText("Greater Art player is displayed above other apps")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        composeView?.let { runCatching { windowManager?.removeViewImmediate(it) } }
        composeView = null
        VideoSurfaceOwner.setSystemOverlayVisible(OVERLAY_TOKEN, "NOW_PLAYING", false)
        VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
        scope.cancel()
        viewModelStore.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_FROM_MINI_WINDOW = "from_mini_window"
        private const val OVERLAY_TOKEN = "now_playing_overlay"
        private const val CHANNEL_ID = "greater_art_playback"
        private const val NOTIFICATION_ID = 3
        private const val HANDOFF_TIMEOUT_MS = 1_500L
    }
}
