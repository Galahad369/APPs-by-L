package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow

/** Volatile diagnostics, never persisted or uploaded. Unknown output facts stay unknown. */
object PlaybackDiagnostics {
    val report = MutableStateFlow("Playback service not connected")
    var requestedAtMs = 0L
    var firstFrameDelayMs: Long? = null
}
