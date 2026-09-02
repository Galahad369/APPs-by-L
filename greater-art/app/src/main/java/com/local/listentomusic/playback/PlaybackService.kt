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
import androidx.media3.session.MediaSessionService
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

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var preferences: AppPreferences
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var saveJob: Job? = null
    private var retriedPath: String? = null
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

        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = scheduleSave()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                retriedPath = null
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
            while (isActive) {
                delay(5_000)
                if (player.currentMediaItem != null) saveNow()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveNow()
        // Do not stop: MediaSessionService keeps active audio available in the notification.
    }

    override fun onDestroy() {
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
