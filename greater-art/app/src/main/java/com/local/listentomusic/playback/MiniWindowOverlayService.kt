package com.local.listentomusic.playback

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.MainActivity
import com.local.listentomusic.R
import com.local.listentomusic.model.MiniWindowMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

// A tiny, local-only floating player implemented with WindowManager.
// Plain Android views (NOT Compose) so it needs no LifecycleOwner.
// Drag onto the bottom-center red cross to close + stop the app.
class MiniWindowOverlayService : Service() {
    private var wm: WindowManager? = null
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val isVideo = MutableStateFlow(false)
        private val videoExtensions = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")

        // The visible target sits immediately above the real navigation-bar inset.
        // Visible circle and collision radius are identical. With BOTTOM gravity, larger y is higher.
        private val crossHitSize = 57
        private val crossSize = 25
        private val crossMargin = 11
        private val crossBaseAlpha = 1f
        private val crossRaisePx = 14
        private var crossActive: Boolean? = null
        private var framePending = false
        private var openingApp = false
    private var gestureGeneration = 0
    private val dragFrame = Runnable {
        framePending = false
        updateRootLayout()
        root?.post { if (dragging) updateCrossAppearance(miniOverlapsCross()) }
    }

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    // views
    private var dangerTint: View? = null
    private var videoView: PlayerView? = null
    private var artworkGeneration = 0

    // drag-to-close drop target (red cross at screen bottom-center)
    private var crossView: FrameLayout? = null
    private var crossImg: ImageView? = null
    private var crossParams: WindowManager.LayoutParams? = null

    companion object {
        internal val destinationReady = MutableStateFlow(false)
        // Reuse the media channel so Android accepts the foreground promotion.
        private const val CHANNEL_ID = "greater_art_playback"
        const val EXTRA_STOP_APP = "stop_app"
        const val EXTRA_OPEN_PLAYER = "open_player"
        const val EXTRA_START_X = "start_x"
        const val EXTRA_START_Y = "start_y"
        private const val POSITION_PREFS = "mini_window_position"
        private const val POSITION_X = "x"
        private const val POSITION_Y = "y"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        destinationReady.value = false
        com.local.listentomusic.ui.components.VideoSurfaceOwner.serviceEvent("start")
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setSystemOverlayVisible(
            "mini_window_overlay",
            "MINI_WINDOW",
            true,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createChannel()
                startForeground(2, buildNotification())
                wm = getSystemService(WindowManager::class.java)
                buildView()
                buildCross()
                params = WindowManager.LayoutParams(
                    miniWidthPx(), miniHeightPx(),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.RGBA_8888,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            alpha = 0f
            if (alpha == 0f) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            if (Build.VERSION.SDK_INT >= 30) {
                // Protect the status bar, but deliberately allow the user to drag over
                // the navigation-bar area just like the older mini window.
                setFitInsetsTypes(WindowInsets.Type.statusBars())
                setFitInsetsSides(WindowInsets.Side.TOP)
            }
            val saved = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE)
            x = saved.getInt(POSITION_X, dp(12))
            y = saved.getInt(POSITION_Y, dp(300))
        }
        clampPosition()
        try {
            root?.let { wm?.addView(it, params!!) }
        } catch (t: Throwable) {
            // Keep the user's Mini preference intact. A temporary OEM overlay failure
            // must not silently rewrite Settings to a different floating mode.
            stopSelf()
            return
        }
        connect()
        scope.launch {
            com.local.listentomusic.data.AppPreferences(applicationContext).values.collect {
                compact?.appearance(it)
            }
        }
        scope.launch {
            PlayerWindowVisibility.detachedVisible.collect { visible ->
                params?.let {
                    it.alpha = if (visible && ready) 1f else 0f
                    it.flags = if (visible && ready) it.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                        else it.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                if (!visible) { dragging = false; crossView?.visibility = View.INVISIBLE }
                updateRootLayout()
            }
        }
        val touch = View.OnTouchListener { view, event -> drag(view, event) }
        root?.setOnTouchListener(touch)
        root?.setOnClickListener { openApp() }

        // Coroutine-driven view updates are guarded because destruction can race collection.
        scope.launch {
            isVideo.collect { v ->
                updateMiniWindowSize()
                clampPosition()
                updateRootLayout()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("MINI_WINDOW")
        val layout = params
        val saved = getSharedPreferences(POSITION_PREFS, MODE_PRIVATE)
        if (layout != null && intent?.hasExtra(EXTRA_START_X) == true) {
            layout.x = intent.getIntExtra(EXTRA_START_X, layout.x)
            layout.y = intent.getIntExtra(EXTRA_START_Y, layout.y)
            clampPosition()
            updateRootLayout()
            savePosition()
        }
        if (controller != null) awaitDestinationReady()
        return START_NOT_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mini window active")
            .setContentText("Tap the floating player to open Greater Art")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mini window", NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
    }

    private fun openApp() {
        // Mini Window and full Now Playing are two system-overlay presentations of
        // the same playback session. Expand directly without routing through Activity.
        if (openingApp) return
        openingApp = true
        com.local.listentomusic.ui.components.VideoSurfaceOwner.beginHandoff("NOW_PLAYING")
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, NowPlayingOverlayService::class.java).apply {
                    putExtra(NowPlayingOverlayService.EXTRA_FROM_MINI_WINDOW, true)
                },
            )
            root?.postDelayed({ openingApp = false }, 1000)
        } catch (t: Throwable) {
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("NOW_PLAYING")
            openingApp = false
        }
    }

    // Dragging onto the center red cross closes the window and stops playback.
    private fun closeAndStopApp() {
        ParallelPlayback.stopAll()
        // Stop media first. Calling stopSelf before this can race onDestroy and release
        // the controller before playback receives the stop command.
        controller?.run {
            stop()
            clearMediaItems()
        }
        stopService(Intent(this, PlaybackService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        try {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_STOP_APP, true)
            })
        } catch (t: Throwable) {
            // Playback and the overlay are already stopped if the Activity cannot launch.
        }
    }

    private var compact: com.local.listentomusic.ui.components.CompactPlayerView? = null
    private var connectionLease: SharedPlaybackResource.Lease<ListenableFuture<MediaController>>? = null
    private var ready = false

    private fun buildView() {
        root = object : FrameLayout(this) {
            override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) drag(this, event)
                if (event.actionMasked == MotionEvent.ACTION_MOVE &&
                    (abs(event.rawX - downX) > android.view.ViewConfiguration.get(context).scaledTouchSlop ||
                     abs(event.rawY - downY) > android.view.ViewConfiguration.get(context).scaledTouchSlop)) return true
                return false
            }
        }
        compact = com.local.listentomusic.ui.components.CompactPlayerView(this).also {
            it.onOpen = { openApp() }
            root!!.addView(it, FrameLayout.LayoutParams(-1, -1))
            videoView = it.video
        }
        dangerTint = View(this).apply {
            setBackgroundColor(0x66FF3B30)
            visibility = View.GONE
            isClickable = false
        }
        root!!.addView(dangerTint, FrameLayout.LayoutParams(-1, -1))
    }

    private fun buildCross() {
            crossView = FrameLayout(this).apply {
                // This circle is the real hit area—not decoration with a different size.
                // Its faint fill makes the exact quit zone visible without shouting.
                background = crossTargetDrawable(active = false)
            }
            crossImg = ImageView(this).apply {
                setImageResource(R.drawable.ic_red_cross)
                setColorFilter(0xFFFFFFFF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            crossView?.addView(crossImg!!, FrameLayout.LayoutParams(dp(crossSize), dp(crossSize)).apply { gravity = Gravity.CENTER })
            crossView?.visibility = View.INVISIBLE
        val layout = WindowManager.LayoutParams(
            dp(crossHitSize), dp(crossHitSize),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.RGBA_8888,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Overlay bounds are already inset from system bars on affected Samsung builds.
            // Adding the navigation inset again placed the X too high and broke collision.
            y = dp(crossMargin) + crossRaisePx
        }
        crossParams = layout
        try {
            crossView?.let { wm?.addView(it, layout) }
        } catch (t: Throwable) {
            // Some OEMs reject a second overlay window. The player is still useful
            // without the drag-to-close target, so keep it alive.
            crossView = null
            crossImg = null
        }
    }

    private fun connect() {
        val lease = PlaybackConnection.acquire(this)
        connectionLease = lease
        val pending = lease.value
        future = pending
        pending.addListener({
            if (future !== pending) {
                return@addListener
            }
            runCatching { pending.get() }.onSuccess { c ->
                controller = c
                c.addListener(listener)
                push(c)
                awaitDestinationReady()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var readinessJob: kotlinx.coroutines.Job? = null
    private val sessionHasMedia = MutableStateFlow(false)

    private fun awaitDestinationReady() {
        readinessJob?.cancel()
        ready = false
        destinationReady.value = false
        params?.let {
            it.alpha = 0f
            it.flags = it.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        updateRootLayout()
        readinessJob = scope.launch {
            val success = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                sessionHasMedia.first { it }
                if (isVideo.value) com.local.listentomusic.ui.components.VideoSurfaceOwner.state.first {
                    it.owner == "MINI_WINDOW" && it.firstFrame
                }
                true
            } == true
            if (!success) {
                com.local.listentomusic.ui.components.VideoSurfaceOwner.serviceEvent("first-frame-timeout")
                stopSelf()
                return@launch
            }
            ready = true
            destinationReady.value = true
            params?.let {
                it.alpha = if (PlayerWindowVisibility.detachedVisible.value) 1f else 0f
                if (it.alpha > 0f) it.flags = it.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            updateRootLayout()
            com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = push(player)
    }

    private fun push(p: Player) {
        compact?.bind(p, "MINI_WINDOW")
        val path = p.currentMediaItem?.mediaId
        isVideo.value = p.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
            path?.substringAfterLast('.').orEmpty().lowercase() in videoExtensions
        updateMiniWindowSize()
        updateArtwork(p.mediaMetadata.artworkData)
        sessionHasMedia.value = p.currentMediaItem != null
    }

    private fun drag(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureGeneration++
                downX = event.rawX
                downY = event.rawY
                startX = params?.x ?: 0
                startY = params?.y ?: 0
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
                if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                    dragging = true
                    updateCrossAppearance(false)
                    crossView?.visibility = View.VISIBLE
                }
                if (dragging) {
                    params?.x = startX + dx.toInt()
                    params?.y = startY + dy.toInt()
                    clampPosition()
                    if (!framePending) {
                        framePending = true
                        root?.postOnAnimation(dragFrame)
                    }
                    return true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val was = dragging
                dragging = false
                if (was) {
                    root?.removeCallbacks(dragFrame)
                    framePending = false
                    updateRootLayout()
                    savePosition()
                    // Read actual on-screen coordinates after the final layout, not
                    // the previous move event's position.
                    val generation = gestureGeneration
                    root?.postOnAnimation {
                        root?.postOnAnimation {
                            if (generation == gestureGeneration) {
                                val overlap = miniOverlapsCross()
                                updateCrossAppearance(false)
                                crossView?.visibility = View.INVISIBLE
                                if (overlap) closeAndStopApp()
                            }
                        }
                    }
                    return true
                }
                view.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                gestureGeneration++
                root?.removeCallbacks(dragFrame)
                framePending = false
                dragging = false
                updateCrossAppearance(false)
                crossView?.visibility = View.INVISIBLE
                return true
            }
        }
        return false
    }

    private fun updateCrossAppearance(active: Boolean) {
        if (crossActive == active) return
        crossActive = active
        crossImg?.alpha = if (active) 1f else crossBaseAlpha
        crossView?.background = crossTargetDrawable(active)
        dangerTint?.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun crossTargetDrawable(active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (active) 0xFFFF3B30.toInt() else 0xFFD92D25.toInt())
        setStroke(dp(2), if (active) 0xFFFF453A.toInt() else 0xDFFF453A.toInt())
    }

    private fun clampPosition() {
        val layout = params ?: return
        val bounds = if (Build.VERSION.SDK_INT >= 30) wm?.currentWindowMetrics?.bounds else null
        val metrics = resources.displayMetrics
        layout.x = layout.x.coerceIn(0, ((bounds?.width() ?: metrics.widthPixels) - layout.width).coerceAtLeast(0))
        // LayoutParams already applies the requested top inset. Subtracting system bars
        // here a second time created the visible bottom "wall" on Samsung devices.
        val height = bounds?.height() ?: metrics.heightPixels
        layout.y = layout.y.coerceIn(0, (height - layout.height).coerceAtLeast(0))
    }

    // Read both overlay locations from Android. Reconstructing either rectangle from
    // displayMetrics drifts on gesture navigation, cutouts and OEM window insets.
    private fun miniOverlapsCross(): Boolean {
        val mini = root ?: return false
        val cross = crossView ?: return false
        if (!mini.isAttachedToWindow || !cross.isAttachedToWindow) return false
        val miniLocation = IntArray(2)
        val crossLocation = IntArray(2)
        mini.getLocationOnScreen(miniLocation)
        cross.getLocationOnScreen(crossLocation)
        val miniBounds = Rect(
            miniLocation[0], miniLocation[1],
            miniLocation[0] + mini.width, miniLocation[1] + mini.height,
        )
        // Match the visible circular target, not its square WindowManager bounds.
        // Rect.intersects previously accepted invisible corner pixels outside the ring.
        val centerX = crossLocation[0] + cross.width / 2f
        val centerY = crossLocation[1] + cross.height / 2f
        val nearestX = centerX.coerceIn(miniBounds.left.toFloat(), miniBounds.right.toFloat())
        val nearestY = centerY.coerceIn(miniBounds.top.toFloat(), miniBounds.bottom.toFloat())
        val dx = nearestX - centerX
        val dy = nearestY - centerY
        val radius = minOf(cross.width, cross.height) / 2f
        return dx * dx + dy * dy <= radius * radius
    }

    private fun updateRootLayout() {
        val view = root ?: return
        val layout = params ?: return
        runCatching { wm?.updateViewLayout(view, layout) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        crossParams?.let { layout ->
            layout.y = dp(crossMargin) + crossRaisePx
            crossView?.let { view -> runCatching { wm?.updateViewLayout(view, layout) } }
        }
        clampPosition()
        updateRootLayout()
        savePosition()
    }

    private fun savePosition() {
        val layout = params ?: return
        getSharedPreferences(POSITION_PREFS, MODE_PRIVATE).edit {
            putInt(POSITION_X, layout.x)
            putInt(POSITION_Y, layout.y)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun miniWidthPx() = com.local.listentomusic.model.CompactPlayerMetrics.widthPx(
        resources.displayMetrics.density, if (Build.VERSION.SDK_INT >= 30)
            wm?.currentWindowMetrics?.bounds?.width() ?: resources.displayMetrics.widthPixels
        else resources.displayMetrics.widthPixels)
    private fun miniHeightPx() = com.local.listentomusic.model.CompactPlayerMetrics.heightPx(resources.displayMetrics.density)

    private fun updateMiniWindowSize() {
        val layout = params ?: return
        val width = miniWidthPx()
        val height = miniHeightPx()
        if (layout.width == width && layout.height == height) return
        layout.width = width
        layout.height = height
        clampPosition()
        updateRootLayout()
    }

    private var lastArtworkData: ByteArray? = null
    private fun updateArtwork(data: ByteArray?) {
        if (data === lastArtworkData || (data != null && data.contentEquals(lastArtworkData))) return
        lastArtworkData = data
        val generation = ++artworkGeneration
        if (data == null) {
            compact?.setArtwork(null)
            updateMiniWindowSize()
            return
        }
        scope.launch {
            val decoded = withContext(Dispatchers.Default) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
                bitmap
            }
            if (generation == artworkGeneration) {
                compact?.setArtwork(decoded)
                updateMiniWindowSize()
            }
        }
    }

    override fun onDestroy() {
        destinationReady.value = false
        scope.cancel()
        com.local.listentomusic.ui.components.VideoSurfaceOwner.serviceEvent("stop")
        com.local.listentomusic.ui.components.VideoSurfaceOwner.setSystemOverlayVisible(
            "mini_window_overlay",
            "MINI_WINDOW",
            false,
        )
        com.local.listentomusic.ui.components.VideoSurfaceOwner.finishHandoff("MINI_WINDOW")
        root?.removeCallbacks(dragFrame)
        root?.let { runCatching { wm?.removeViewImmediate(it) } }
        crossView?.let { runCatching { wm?.removeViewImmediate(it) } }
        videoView?.let(com.local.listentomusic.ui.components.VideoSurfaceOwner::detach)
        controller?.removeListener(listener)
        controller = null
        compact?.release()
        compact = null
        connectionLease?.close()
        connectionLease = null
        future = null
        root = null
        crossView = null
        crossParams = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
