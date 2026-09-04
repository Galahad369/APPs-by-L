package com.local.listentomusic.playback

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
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
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.MainActivity
import com.local.listentomusic.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

    private val title = MutableStateFlow("")
    private val playing = MutableStateFlow(false)
    private val isVideo = MutableStateFlow(false)
    private val videoExtensions = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")

    // The visible target sits immediately above the real navigation-bar inset.
    // A larger invisible hit box makes the drop reliable while the visible X stays compact.
    private val crossHitSize = 72
    private val crossSize = 44
    private val crossMargin = 12

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    // views
    private var artBox: FrameLayout? = null
    private var videoView: PlayerView? = null
    private var titleView: TextView? = null
    private var toggleBtn: ImageButton? = null
    private var closeBtn: ImageButton? = null

    // drag-to-close drop target (red cross at screen bottom-center)
    private var crossView: FrameLayout? = null
    private var crossImg: ImageView? = null
    private var crossParams: WindowManager.LayoutParams? = null

    companion object {
        // Reuse the media channel so Android accepts the foreground promotion.
        private const val CHANNEL_ID = "greater_art_playback"
        const val EXTRA_STOP_APP = "stop_app"
        const val EXTRA_OPEN_PLAYER = "open_player"
        const val ACTION_FALLBACK_COMPACT = "com.local.listentomusic.MINI_FALLBACK_COMPACT"
        private const val AUDIO_WIDTH = 124
        private const val AUDIO_HEIGHT = 40
        private const val VIDEO_WIDTH = 108
        private const val VIDEO_HEIGHT = 61
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            sendFallbackCompact()
            stopSelf()
            return
        }
        createChannel()
        startForeground(2, buildNotification())
        wm = getSystemService(WindowManager::class.java)
        buildView()
        buildCross()
        params = WindowManager.LayoutParams(
            dp(AUDIO_WIDTH), dp(AUDIO_HEIGHT),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(300)
        }
        try {
            root?.let { wm?.addView(it, params!!) }
        } catch (t: Throwable) {
            // Any root addView failure means the overlay cannot run; fall back to compact.
            sendFallbackCompact()
            stopSelf()
            return
        }
        connect()
        val touch = View.OnTouchListener { view, event -> drag(view, event) }
        root?.setOnTouchListener(touch)
        root?.setOnClickListener { openApp() }
        videoView?.setOnTouchListener(touch)
        videoView?.setOnClickListener { openApp() }

        // Coroutine-driven view updates are guarded because destruction can race collection.
        scope.launch {
            isVideo.collect { v ->
                artBox?.visibility = if (v) View.GONE else View.VISIBLE
                videoView?.visibility = if (v) View.VISIBLE else View.GONE
                closeBtn?.visibility = if (v) View.GONE else View.VISIBLE
                // Video mode is pure video: no box and no chrome.
                root?.background = if (v) null else ContextCompat.getDrawable(this@MiniWindowOverlayService, R.drawable.mini_player_bg)
                params?.width = dp(if (v) VIDEO_WIDTH else AUDIO_WIDTH)
                params?.height = dp(if (v) VIDEO_HEIGHT else AUDIO_HEIGHT)
                clampPosition()
                updateRootLayout()
            }
        }
        scope.launch { title.collect { titleView?.text = it } }
        scope.launch {
            playing.collect { p ->
                toggleBtn?.setImageResource(if (p) R.drawable.ic_pause else R.drawable.ic_play)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

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

    private fun sendFallbackCompact() {
        // Switch MINI_WINDOW -> COMPACT so playback can still float.
        val ctx = applicationContext
        ctx.sendBroadcast(Intent(ACTION_FALLBACK_COMPACT).setPackage(ctx.packageName))
    }

    private fun openApp() {
        // Tapping the mini window returns to the full player, not the library.
        stopSelf()
        try {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_PLAYER, true)
            })
        } catch (t: Throwable) {
            // The overlay has already stopped even if the Activity cannot be launched.
        }
    }

    // Dragging onto the center red cross closes the window and stops playback.
    private fun closeAndStopApp() {
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

    private fun buildView() {
        root = FrameLayout(this)
        // Dark translucency keeps video letterboxing clean; audio mode uses the chip drawable.
        root?.setBackgroundColor(0xCC0A0C0B.toInt())
        val pad = dp(4)
        root?.setPadding(pad, pad, pad, pad)

        artBox = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val note = ImageView(this).apply {
            setImageResource(R.drawable.ic_music_note)
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(6) }
        }
        titleView = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFFF3F5F0.toInt())
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxLines = 1
            layoutParams = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_play)
            background = null
            setOnClickListener { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(28), dp(28))
        }
        row.addView(note)
        row.addView(titleView!!)
        row.addView(toggleBtn!!)
        artBox?.addView(row)

        videoView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setKeepContentOnPlayerReset(true)
            visibility = View.GONE
        }

        closeBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_close)
            background = null
            setOnClickListener { stopSelf() }
            layoutParams = FrameLayout.LayoutParams(dp(16), dp(16)).apply {
                gravity = Gravity.TOP or Gravity.END
            }
        }

        root?.addView(artBox!!)
        root?.addView(videoView!!)
        root?.addView(closeBtn!!)
    }

    private fun buildCross() {
        crossView = FrameLayout(this).apply {
            // This circle is the real hit area—not decoration with a different size.
            // Its faint fill makes the exact quit zone visible without shouting.
            background = crossTargetDrawable(active = false)
        }
        crossImg = ImageView(this).apply { setImageResource(R.drawable.ic_red_cross) }
        crossView?.addView(crossImg!!, FrameLayout.LayoutParams(dp(crossSize), dp(crossSize)).apply { gravity = Gravity.CENTER })
        crossView?.visibility = View.INVISIBLE
        val layout = WindowManager.LayoutParams(
            dp(crossHitSize), dp(crossHitSize),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Overlay bounds are already inset from system bars on affected Samsung builds.
            // Adding the navigation inset again placed the X too high and broke collision.
            y = dp(crossMargin)
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
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val pending = MediaController.Builder(this, token).buildAsync()
        future = pending
        pending.addListener({
            if (future !== pending) {
                MediaController.releaseFuture(pending)
                return@addListener
            }
            runCatching { pending.get() }.onSuccess { c ->
                controller = c
                c.addListener(listener)
                push(c)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = push(player)
    }

    private fun push(p: Player) {
        val path = p.currentMediaItem?.mediaId
        title.value = p.mediaMetadata.title?.toString()
            ?: path?.substringAfterLast('/')?.substringBeforeLast('.') ?: ""
        playing.value = p.isPlaying
        isVideo.value = p.currentMediaItem?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO ||
            path?.substringAfterLast('.').orEmpty().lowercase() in videoExtensions
        videoView?.player = if (isVideo.value) controller else null
    }

    private fun drag(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
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
                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) {
                    dragging = true
                    updateCrossAppearance(false)
                    crossView?.visibility = View.VISIBLE
                }
                if (dragging) {
                    params?.x = startX + dx.toInt()
                    params?.y = startY + dy.toInt()
                    clampPosition()
                    updateRootLayout()
                    val overlap = miniOverlapsCross()
                    updateCrossAppearance(overlap)
                    return true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val was = dragging
                dragging = false
                if (was) {
                    val overlap = miniOverlapsCross()
                    crossView?.visibility = View.INVISIBLE
                    if (overlap) closeAndStopApp()
                    return true
                }
                view.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                crossView?.visibility = View.INVISIBLE
                return true
            }
        }
        return false
    }

    private fun updateCrossAppearance(active: Boolean) {
        crossImg?.alpha = if (active) 1f else 0.72f
        crossView?.background = crossTargetDrawable(active)
    }

    private fun crossTargetDrawable(active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (active) 0x38FF3B30 else 0x16FF3B30)
        setStroke(dp(if (active) 2 else 1), if (active) 0xCCFF453A.toInt() else 0x70FF453A)
    }

    private fun clampPosition() {
        val layout = params ?: return
        val metrics = resources.displayMetrics
        layout.x = layout.x.coerceIn(0, (metrics.widthPixels - layout.width).coerceAtLeast(0))
        layout.y = layout.y.coerceIn(0, (metrics.heightPixels - layout.height).coerceAtLeast(0))
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

    private fun navigationBarInsetBottom(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return wm?.currentWindowMetrics?.windowInsets
                ?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
                ?.bottom ?: 0
        }
        // Pre-R overlay coordinates are already constrained to the usable display
        // frame. Adding a reflected system dimension double-counts some OEM bars.
        return 0
    }

    private fun updateRootLayout() {
        val view = root ?: return
        val layout = params ?: return
        runCatching { wm?.updateViewLayout(view, layout) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        crossParams?.let { layout ->
            layout.y = dp(crossMargin)
            crossView?.let { view -> runCatching { wm?.updateViewLayout(view, layout) } }
        }
        clampPosition()
        updateRootLayout()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        root?.let { runCatching { wm?.removeViewImmediate(it) } }
        crossView?.let { runCatching { wm?.removeViewImmediate(it) } }
        videoView?.player = null
        controller?.removeListener(listener)
        controller = null
        future?.let(MediaController::releaseFuture)
        future = null
        root = null
        crossView = null
        crossParams = null
        wm = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
