package com.local.listentomusic.playback

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.local.listentomusic.MainActivity
import com.local.listentomusic.ui.theme.GreaterArtTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

// ponytail: real Yee-style floating mini player — a WindowManager overlay service,
// not an in-app Compose box (which has a size floor and dies when the app backgrounds).
class MiniWindowOverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: ComposeView
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

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WindowManager::class.java)
        view = ComposeView(this)
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
        view.setContent {
            GreaterArtTheme {
                val t by title.collectAsState()
                val p by playing.collectAsState()
                val v by isVideo.collectAsState()
                val c by controllerFlow.collectAsState()
                MiniContent(t, p, v, c,
                    onOpen = { openApp() },
                    onToggle = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onClose = { stopSelf() },
                )
            }
        }
        wm.addView(view, params)
        connect()
        view.setOnTouchListener { _, event -> drag(event) }
        scope.launch {
            isVideo.collect { v ->
                params.width = dp(if (v) 120 else 140)
                params.height = dp(if (v) 68 else 46)
                wm.updateViewLayout(view, params)
            }
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
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
                    wm.updateViewLayout(view, params)
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

    @Composable
    private fun MiniContent(
        title: String,
        playing: Boolean,
        isVideo: Boolean,
        controller: MediaController?,
        onOpen: () -> Unit,
        onToggle: () -> Unit,
        onClose: () -> Unit,
    ) {
        Box(Modifier.size(if (isVideo) 120.dp else 140.dp, if (isVideo) 68.dp else 46.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isVideo && controller != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setKeepContentOnPlayerReset(true)
                                player = controller
                            }
                        },
                        update = { it.player = controller },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            title,
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = onToggle, Modifier.size(30.dp)) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null,
                                Modifier.size(16.dp),
                                MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onClose, Modifier.size(18.dp).align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.Close, null, Modifier.size(11.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    override fun onDestroy() {
        runCatching { wm.removeViewImmediate(view) }
        controller?.removeListener(listener)
        controller = null
        future = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
