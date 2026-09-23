package com.local.listentomusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.animation.ValueAnimator
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import com.local.listentomusic.ui.UiInspectorHost
import com.local.listentomusic.ui.UiInspectorState
import com.local.listentomusic.ui.DeveloperDiagnostics
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
import com.local.listentomusic.ui.ShareProxyActivity
import com.local.listentomusic.ui.NowPlayingScreen
import com.local.listentomusic.ui.theme.GreaterArtTheme
import com.local.listentomusic.ui.uiText
import com.local.listentomusic.model.mediaFileFromSession
import com.local.listentomusic.ui.components.VideoSurfaceOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

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
    override val onBackPressedDispatcher = OnBackPressedDispatcher { shrinkToMini() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var viewModel: MainViewModel
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var launchedFromMini = false
    private var sharing = false
    private var closing = false
    private var shrinking = false
    private var originalWindowFlags = 0
    private var windowAnimator: ValueAnimator? = null
    private val shareFinished = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ShareProxyActivity.ACTION_FINISHED) restoreAfterShare()
        }
    }
    private val systemHome = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Receive only; never send the privileged CLOSE_SYSTEM_DIALOGS action.
            if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS &&
                shouldShrinkForSystemReason(intent.getStringExtra("reason"))) {
                shrinkToMini()
            }
        }
    }

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
        viewModel.useSessionPresentationOnly()
        ContextCompat.registerReceiver(
            this, shareFinished, IntentFilter(ShareProxyActivity.ACTION_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(this, systemHome, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS),
            ContextCompat.RECEIVER_NOT_EXPORTED)

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
            val inspector = remember { UiInspectorState() }

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
                UiInspectorHost(settings.developerMode, inspector) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    tonalElevation = 4.dp,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(32.dp).pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { windowAnimator?.cancel() },
                                onDragEnd = { finishPull() },
                                onDragCancel = { resetPull() },
                                onVerticalDrag = { change, amount -> change.consume(); dragWindow(amount) },
                            )
                        }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(36.dp, 4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant,
                                androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                        }
                        Box(Modifier.fillMaxWidth().weight(1f)) {
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
                        onClose = ::shrinkToMini,
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
                                ?: controller?.currentMediaItem?.let(::mediaFileFromSession)
                            if (current == null) shareError(settings.appLanguage)
                            else AndroidShare.mediaChooser(
                                this@NowPlayingOverlayService, current,
                                uiText(settings.appLanguage, "Share media file", "分享媒體檔案"),
                            ).onSuccess(::launchShare).onFailure { shareError(settings.appLanguage) }
                        },
                        onShareQueue = {
                            scope.launch {
                                AndroidShare.listChooser(
                                    this@NowPlayingOverlayService,
                                    uiText(settings.appLanguage, "Current queue", "目前播放佇列"),
                                    queue,
                                    uiText(settings.appLanguage, "Share current queue", "分享目前播放佇列"),
                                ).onSuccess(::launchShare).onFailure { shareError(settings.appLanguage) }
                            }
                        },
                        systemOverlay = true,
                    )
                        }
                    }
                }
                if (settings.developerMode) {
                    val engine by PlaybackDiagnostics.report.collectAsState()
                    val thumbs by viewModel.thumbnailStats.collectAsState()
                    val waveform by viewModel.waveformDiagnostics.collectAsState()
                    DeveloperDiagnostics(
                        report = "version=${com.local.listentomusic.BuildConfig.VERSION_NAME}\nscreen=NOW_PLAYING\nplaying=${playback.isPlaying}\nplayerState=${controller?.playbackState}\nposition=${playback.positionMs} duration=${playback.durationMs}\nvideo=${playback.videoWidth}x${playback.videoHeight}\nqueue=${queue.size}\n${VideoSurfaceOwner.describe()}\nthumbs=$thumbs\nwaveform=$waveform\nAUDIO ENGINE\n$engine",
                        regions = inspector.regions.values.map { "${it.label}: ${it.detail}" },
                        warning = playback.errorMessage != null, inspector = inspector, systemOverlay = true,
                    )
                }
                }
            }
        }

        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics
        } else null
        val bounds = metrics?.bounds
        val rawWidth = bounds?.width() ?: resources.displayMetrics.widthPixels
        val rawHeight = bounds?.height() ?: resources.displayMetrics.heightPixels
        val systemInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && metrics != null) {
            metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
        } else null
        val availableWidth = (rawWidth - (systemInsets?.left ?: 0) - (systemInsets?.right ?: 0))
            .coerceAtLeast(1)
        val availableHeight = (rawHeight - (systemInsets?.top ?: 0) - (systemInsets?.bottom ?: 0))
            .coerceAtLeast(1)
        // Floating, but intentionally close to Yee's efficient use of phone space:
        // media + queue get room while transport controls stay permanently visible.
        val targetSize = floatingOverlaySize(availableWidth, availableHeight)
        windowParams = WindowManager.LayoutParams(
            targetSize.widthPx,
            targetSize.heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            // Start transparent. The source presentation stays visible until this
            // overlay has real playback state and, for video, a rendered frame.
            alpha = 0f
            dimAmount = 0f
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
            // Never expose the empty "Nothing playing" intermediate state. Keep the
            // source visible until the session has media and the destination video
            // surface has produced a frame.
            val playback = withTimeoutOrNull(5_000L) { viewModel.playback
                .filter { it.connected && it.currentPath != null }
                .first() } ?: run { stopSelf(); return@launch }
            if (playback.isVideo) {
                withTimeoutOrNull(HANDOFF_TIMEOUT_MS) {
                    VideoSurfaceOwner.state.first {
                        it.owner == "NOW_PLAYING" && it.firstFrame
                    }
                }
            }
            if (closing || shrinking) return@launch
            VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            revealOverlay {
                if (launchedFromMini) {
                    stopService(Intent(this@NowPlayingOverlayService, MiniWindowOverlayService::class.java))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHRINK) shrinkToMini()
        launchedFromMini = launchedFromMini ||
            intent?.getBooleanExtra(EXTRA_FROM_MINI_WINDOW, false) == true
        return START_NOT_STICKY
    }

    private fun returnToLibrary() {
        if (closing || shrinking || sharing) return
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
        dismissOverlay()
    }

    private fun revealOverlay(afterReveal: () -> Unit = {}) {
        animateWindow(
            from = windowParams?.alpha ?: 0f,
            to = 1f,
            durationMs = 150L,
            after = afterReveal,
        )
    }

    private fun dismissOverlay(afterDismiss: () -> Unit = { stopSelf() }) {
        if (closing) return
        closing = true
        animateWindow(
            from = windowParams?.alpha ?: 1f,
            to = 0f,
            durationMs = 110L,
            after = afterDismiss,
        )
    }

    private fun animateWindow(
        from: Float,
        to: Float,
        durationMs: Long,
        after: () -> Unit,
    ) {
        val view = composeView
        val params = windowParams
        if (view == null || params == null || !view.isAttachedToWindow) {
            after()
            return
        }
        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                params.alpha = progress
                params.dimAmount = TARGET_DIM_AMOUNT * progress
                runCatching { windowManager?.updateViewLayout(view, params) }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!cancelled) after()
                }
            })
            start()
        }
    }

    private fun launchShare(chooser: Intent) {
        if (sharing || closing || shrinking) return
        val view = composeView ?: return
        val params = windowParams ?: return
        sharing = true
        originalWindowFlags = params.flags
        runCatching {
            // A TYPE_APPLICATION_OVERLAY otherwise sits above Android's chooser and
            // makes sharing appear broken. Keep the player/session alive underneath.
            windowAnimator?.cancel()
            params.alpha = 0f
            params.dimAmount = 0f
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            windowManager?.updateViewLayout(view, params)
            startActivity(Intent(this, ShareProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(ShareProxyActivity.EXTRA_CHOOSER, chooser)
            })
        }.onFailure {
            restoreAfterShare()
            shareError(viewModel.settings.value.appLanguage)
        }
    }

    private fun restoreAfterShare() {
        if (!sharing) return
        sharing = false
        val view = composeView ?: return
        windowParams?.let { params ->
            params.flags = originalWindowFlags
            params.alpha = 1f
            params.dimAmount = TARGET_DIM_AMOUNT
            runCatching { windowManager?.updateViewLayout(view, params) }
        }
    }

    private fun shareError(language: com.local.listentomusic.data.AppLanguage) {
        Toast.makeText(this, uiText(language, "Could not open sharing", "無法開啟分享"), Toast.LENGTH_LONG).show()
    }

    private fun shrinkToMini() {
        if (shrinking || closing || sharing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        shrinking = true
        VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, MiniWindowOverlayService::class.java),
            )
        }.onFailure {
            shrinking = false
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            return
        }

        scope.launch {
            val ready = withTimeoutOrNull(2500L) { MiniWindowOverlayService.destinationReady.first { it } }
            if (ready != true) {
                shrinking = false
                stopService(Intent(this@NowPlayingOverlayService, MiniWindowOverlayService::class.java))
                VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
                return@launch
            }
            VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
            sendBroadcast(Intent(com.local.listentomusic.MainActivity.ACTION_BACKGROUND_PLAYER).setPackage(packageName))
            dismissOverlay()
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
        windowAnimator?.cancel()
        windowAnimator = null
        runCatching { unregisterReceiver(shareFinished) }
        runCatching { unregisterReceiver(systemHome) }
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

    private fun dragWindow(delta: Float) {
        if (closing || shrinking || sharing) return
        val params = windowParams ?: return
        params.y = (params.y + delta.roundToInt()).coerceIn(0, params.height.coerceAtLeast(1))
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        composeView?.let { runCatching { windowManager?.updateViewLayout(it, params) } }
    }

    private fun finishPull() {
        val params = windowParams ?: return
        if (!pullDismissReached(params.y, resources.displayMetrics.density)) { resetPull(); return }
        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofInt(params.y, params.height.coerceAtLeast(params.y)).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener { params.y = it.animatedValue as Int
                composeView?.let { view -> runCatching { windowManager?.updateViewLayout(view, params) } } }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: android.animation.Animator) { cancelled = true }
                override fun onAnimationEnd(animation: android.animation.Animator) { if (!cancelled) returnToLibrary() }
            })
            start()
        }
    }

    private fun resetPull() {
        val params = windowParams ?: return
        if (closing || shrinking) return
        windowAnimator?.cancel()
        windowAnimator = ValueAnimator.ofInt(params.y, 0).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener { params.y = it.animatedValue as Int
                composeView?.let { view -> runCatching { windowManager?.updateViewLayout(view, params) } } }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (params.y == 0) {
                        params.flags = params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
                        composeView?.let { runCatching { windowManager?.updateViewLayout(it, params) } }
                    }
                }
            })
            start()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowParams?.let { params ->
            // Let WindowManager fit the new safe frame (including navigation/IME).
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.y = 0
            composeView?.let { runCatching { windowManager?.updateViewLayout(it, params) } }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_SHRINK = "com.local.listentomusic.SHRINK_NOW_PLAYING"
        const val EXTRA_FROM_MINI_WINDOW = "from_mini_window"
        private const val OVERLAY_TOKEN = "now_playing_overlay"
        private const val CHANNEL_ID = "greater_art_playback"
        private const val NOTIFICATION_ID = 3
        private const val HANDOFF_TIMEOUT_MS = 1_500L
        private const val TARGET_DIM_AMOUNT = 0.14f
    }
}

internal fun shouldShrinkForSystemReason(reason: String?): Boolean = reason == "homekey" || reason == "recentapps"
