package com.local.listentomusic.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
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

// ponytail: real Yee-style floating mini player — a WindowManager overlay service.
// Built with plain Android views (NOT Compose) so it needs no LifecycleOwner.
class MiniWindowOverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var root: FrameLayout
    private lateinit var params: WindowManager.LayoutParams
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val title = MutableStateFlow("")
    private val playing = MutableStateFlow(false)
    private val isVideo = MutableStateFlow(false)
    private val controllerFlow = MutableStateFlow<MediaController?>(null)
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "m4v", "mkv", "webm", "3gp", "ts", "mpeg", "mpg", "flv", "avi")

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    // views
    private lateinit var artBox: FrameLayout
    private lateinit var videoView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var toggleBtn: ImageButton
    private lateinit var closeBtn: ImageButton

    companion object {
        // ponytail: reuse the media channel so Android accepts the foreground promotion.
        private const val CHANNEL_ID = "greater_art_playback"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createChannel()
        startForeground(2, buildNotification())
        wm = getSystemService(WindowManager::class.java)
        buildView()
        params = WindowManager.LayoutParams(
            dp(140), dp(46),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(300)
        }
        wm.addView(root, params)
        connect()
        val touch = View.OnTouchListener { _, event -> drag(event) }
        root.setOnTouchListener(touch)
        root.setOnClickListener { if (!dragging) openApp() }
        videoView.setOnTouchListener(touch)
        videoView.setOnClickListener { if (!dragging) openApp() }
        scope.launch {
            isVideo.collect { v ->
                artBox.visibility = if (v) View.GONE else View.VISIBLE
                videoView.visibility = if (v) View.VISIBLE else View.GONE
                closeBtn.visibility = if (v) View.GONE else View.VISIBLE
                // ponytail: video mode is pure video — no box, no chrome
                root.background = if (v) null else getDrawable(R.drawable.mini_player_bg)
                params.width = dp(if (v) 120 else 140)
                params.height = dp(if (v) 68 else 46)
                wm.updateViewLayout(root, params)
            }
        }
        scope.launch { title.collect { titleView.text = it } }
        scope.launch {
            playing.collect { p ->
                toggleBtn.setImageResource(if (p) R.drawable.ic_pause else R.drawable.ic_play)
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

    private fun openApp() {
        // ponytail: tapping the mini window goes home and hides it (user request)
        stopSelf()
        startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    private fun buildView() {
        root = FrameLayout(this)
        // ponytail: black translucent so video letterboxing looks clean; audio mode swaps to the light chip
        root.setBackgroundColor(0xCC0A0C0B.toInt())
        val pad = dp(4)
        root.setPadding(pad, pad, pad, pad)

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
            setTextColor(0xFF0A0C0B.toInt())
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
        row.addView(titleView)
        row.addView(toggleBtn)
        artBox.addView(row)

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

        root.addView(artBox)
        root.addView(videoView)
        root.addView(closeBtn)
    }

    private fun connect() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        future = MediaController.Builder(this, token).buildAsync()
        future?.addListener({
            runCatching { future?.get() }.onSuccess { c ->
                controller = c
                c?.addListener(listener)
                if (c != null) controllerFlow.value = c
                if (c != null) push(c)
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
        isVideo.value = path?.substringAfterLast('.').orEmpty().lowercase() in VIDEO_EXTENSIONS
        if (isVideo.value) {
            videoView.player = controller
        }
    }

    private fun drag(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                dragging = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) dragging = true
                if (dragging) {
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    wm.updateViewLayout(root, params)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                val was = dragging
                dragging = false
                return was
            }
        }
        return false
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        runCatching { wm.removeViewImmediate(root) }
        controller?.removeListener(listener)
        controller = null
        future = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
