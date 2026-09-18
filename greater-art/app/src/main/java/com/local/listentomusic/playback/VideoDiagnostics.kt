package com.local.listentomusic.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.local.listentomusic.BuildConfig

/** Debug-only, event-driven diagnostics. Never log filenames, URIs, or metadata. */
internal fun ExoPlayer.installVideoDiagnostics(role: String) {
    if (!BuildConfig.DEBUG) return
    val identity = "$role:${System.identityHashCode(this)}"
    fun report(event: String) = Log.d("GreaterArtVideo", "$identity $event")
    report("created")
    addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) { report("state=$state") }
        override fun onIsPlayingChanged(isPlaying: Boolean) { report("playing=$isPlaying") }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { report("ready=$playWhenReady reason=$reason") }
        override fun onVideoSizeChanged(videoSize: VideoSize) { report("size=${videoSize.width}x${videoSize.height}") }
        override fun onRenderedFirstFrame() { report("firstFrame") }
        override fun onPlayerError(error: PlaybackException) { report("error=${error.errorCodeName}") }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            report("position=${newPosition.positionMs} reason=$reason")
        }
    })
    addAnalyticsListener(object : AnalyticsListener {
        override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String,
            initializedTimestampMs: Long, initializationDurationMs: Long) { report("decoder=$decoderName initMs=$initializationDurationMs") }
        override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
            report("codecError=${videoCodecError.javaClass.simpleName}")
        }
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            report("dropped=$droppedFrames elapsedMs=$elapsedMs")
        }
        override fun onPlayerReleased(eventTime: AnalyticsListener.EventTime) { report("released") }
    })
}
