package com.local.listentomusic.playback

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MixLayer(val id: String, val title: String, val playing: Boolean, val volume: Float, val path: String)

internal fun mixLevel(level: Float, extraCount: Int): Float =
    (if (level.isFinite()) level.coerceIn(0f, 1f) else 0f) / (extraCount.coerceIn(0, 9) + 1)

/** Main-thread command bridge. Players belong to PlaybackService, never a screen. */
object ParallelPlayback {
    const val MAX_EXTRA_LAYERS = 9 // The main player is the tenth voice.
    private val mutable = MutableStateFlow<List<MixLayer>>(emptyList())
    val layers = mutable.asStateFlow()
    internal var addCommand: ((String) -> Unit)? = null
    internal var removeCommand: ((String) -> Unit)? = null
    internal var toggleCommand: ((String) -> Unit)? = null
    internal var volumeCommand: ((String, Float) -> Unit)? = null
    internal var stopCommand: (() -> Unit)? = null
    fun stopAll() { stopCommand?.invoke() }
    fun add(path: String) { addCommand?.invoke(path) }
    fun remove(id: String) { removeCommand?.invoke(id) }
    fun toggle(id: String) { toggleCommand?.invoke(id) }
    fun volume(id: String, value: Float) { volumeCommand?.invoke(id, if (value.isFinite()) value.coerceIn(0f, 1f) else 0f) }
    internal fun publish(value: List<MixLayer>) { mutable.value = value }
    internal fun detach() { addCommand = null; removeCommand = null; toggleCommand = null; volumeCommand = null; stopCommand = null; mutable.value = emptyList() }
}

internal data class LayerPlayer(val id: String, val title: String, val player: ExoPlayer, val session: MediaSession, var level: Float = 1f)
