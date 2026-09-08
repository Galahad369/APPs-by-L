package com.local.listentomusic.playback

import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import com.local.listentomusic.data.AppPreferences
import com.local.listentomusic.data.MediaScanner
import com.local.listentomusic.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class PlaybackService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var preferences: AppPreferences
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var saveJob: Job? = null
    private var retriedPath: String? = null
    private var gainEnabled = false
    private var enhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var widgetArtwork: android.graphics.Bitmap? = null
    private var widgetJob: Job? = null
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (::player.isInitialized && player.isPlaying && removedDevices.any(::isBluetoothOutput)) {
                // Never spill private playback through the phone speaker after a
                // Bluetooth headset/speaker disappears.
                player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(applicationContext)
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,
                50_000,
                100,
                250,
            )
            // Local 4K/hi-res files can be huge. Keep a useful read-ahead without
            // letting one item grow the player buffer until the process is killed.
            .setTargetBufferBytes(96 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(5_000, true)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.skipSilenceEnabled = false
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(mainLooper))
        val saved = runBlocking(Dispatchers.IO) {
            runCatching { preferences.current() }.getOrDefault(UserPreferences())
        }
        restoreLastSession(saved)

        mediaSession = MediaLibrarySession.Builder(this, player, LocalLibraryCallback(this, serviceScope)).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                PlaybackWidget.update(this@PlaybackService, player.mediaMetadata.title?.toString() ?: "Greater Art", player.isPlaying, widgetArtwork)
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { applyGain() }
            override fun onAudioSessionIdChanged(audioSessionId: Int) { applyGain() }
            override fun onIsPlayingChanged(isPlaying: Boolean) = scheduleSave()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                retriedPath = null
                widgetArtwork = null
                widgetJob?.cancel()
                val path = mediaItem?.mediaId
                if (path != null && android.appwidget.AppWidgetManager.getInstance(this@PlaybackService)
                    .getAppWidgetIds(android.content.ComponentName(this@PlaybackService, PlaybackWidget::class.java)).isNotEmpty()) {
                    widgetJob = serviceScope.launch {
                        val file = File(path)
                        val item = com.local.listentomusic.model.MediaFile(path, file.name, 0, file.length(), file.lastModified(),
                            if (file.extension.lowercase() in MediaScanner.videoExtensions) com.local.listentomusic.model.MediaKind.VIDEO else com.local.listentomusic.model.MediaKind.AUDIO)
                        widgetArtwork = com.local.listentomusic.data.ThumbnailRepository(this@PlaybackService).load(item)
                        PlaybackWidget.update(this@PlaybackService, player.mediaMetadata.title?.toString() ?: "Greater Art", player.isPlaying, widgetArtwork)
                    }
                }
                enhancer?.release()
                enhancer = null
                player.volume = 1f
                scheduleSave()
            }
            override fun onPlaybackStateChanged(playbackState: Int) = scheduleSave()
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = scheduleSave()
            override fun onRepeatModeChanged(repeatMode: Int) = scheduleSave()
            override fun onPlayerError(error: PlaybackException) {
                val failedPath = player.currentMediaItem?.mediaId
                if (!failedPath.isNullOrBlank() && retriedPath != failedPath) {
                    // One retry covers transient decoder/audio-route failures without looping forever.
                    retriedPath = failedPath
                    val retryPosition = player.currentPosition.coerceAtLeast(0L)
                    player.prepare()
                    player.seekTo(retryPosition)
                    player.playWhenReady = true
                } else if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
                    // A corrupt or unsupported item must not strand the rest of the queue.
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        serviceScope.launch {
            preferences.values.collect { savedPreferences ->
                if (gainEnabled != savedPreferences.replayGainEnabled) {
                    gainEnabled = savedPreferences.replayGainEnabled
                    applyGain()
                }
            }
        }
        serviceScope.launch {
            while (isActive) {
                val range = PracticeLoop.state.value
                if (range.path != null && range.path != player.currentMediaItem?.mediaId) PracticeLoop.clear()
                else if (player.isPlaying && range.start != null && range.end != null && player.currentPosition >= range.end) player.seekTo(range.start)
                delay(50)
            }
        }
        serviceScope.launch {
            while (isActive) {
                delay(5_000)
                if (player.currentMediaItem != null) saveNow()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveNow()
        // Do not stop: MediaSessionService keeps active audio available in the notification.
    }

    override fun onDestroy() {
        enhancer?.release()
        enhancer = null
        saveJob?.cancel()
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        runBlocking(Dispatchers.IO) {
            preferences.savePlayback(
                path = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() },
                positionMs = player.currentPosition,
                speed = player.playbackParameters.speed,
                repeatMode = player.repeatMode,
            )
        }
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun restoreLastSession(saved: UserPreferences) {
        player.playbackParameters = androidx.media3.common.PlaybackParameters(saved.playbackSpeed)
        player.repeatMode = saved.repeatMode
        val path = saved.lastPath ?: return
        val file = File(path)
        if (!file.isFile || !file.canRead() || !MediaScanner.isInsideTarget(file)) return

        val item = MediaItem.Builder()
            .setMediaId(path)
            .setUri(android.net.Uri.fromFile(file))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(file.name).build())
            .build()
        player.setMediaItem(item, if (saved.resumePlayback) saved.lastPositionMs else 0L)
        player.prepare()
        player.playWhenReady = false
    }

    private fun applyGain() {
        enhancer?.release()
        enhancer = null
        player.volume = 1f
        if (!gainEnabled) return
        val gain = replayGainDb(player.currentTracks)
        if (gain <= 0) player.volume = Math.pow(10.0, gain / 20.0).toFloat()
        else if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            runCatching {
                android.media.audiofx.LoudnessEnhancer(player.audioSessionId).also {
                    enhancer = it
                    it.setTargetGain((gain * 100).toInt())
                    it.enabled = true
                }
            }.onFailure { enhancer?.release(); enhancer = null }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = serviceScope.launch {
            delay(400)
            saveNow()
        }
    }

    private fun saveNow() {
        if (!::player.isInitialized) return
        val path = player.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        val position = player.currentPosition
        val speed = player.playbackParameters.speed
        val repeat = player.repeatMode
        serviceScope.launch(Dispatchers.IO) {
            preferences.savePlayback(path, position, speed, repeat)
        }
    }

    private fun isBluetoothOutput(device: AudioDeviceInfo): Boolean = when (device.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID -> device.isSink
        else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.isSink &&
            device.type in setOf(AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER)
    }
}
