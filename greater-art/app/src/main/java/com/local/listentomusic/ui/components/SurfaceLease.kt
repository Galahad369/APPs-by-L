package com.local.listentomusic.ui.components

internal fun expectedSurfaceOwner(foreground: Boolean, nowPlaying: Boolean, pip: Boolean): String = when {
    pip -> "NOW_PLAYING"
    !foreground -> "MINI_WINDOW"
    nowPlaying -> "NOW_PLAYING"
    else -> "LIBRARY_MINI"
}

/** Platform-independent ownership state. Calls are serialized on main. */
internal data class SurfaceLease(
    val owner: String = "NONE", val view: Int = 0, val generation: Long = 0,
    val sinceMs: Long = 0, val firstFrame: Boolean = false,
    val mediaFirstFrame: Boolean = false, val lastFrameOwner: String = "NONE",
    val lastFrameGeneration: Long = -1, val staleDetaches: Int = 0,
    val framesByOwner: Map<String, Boolean> = emptyMap(),
    val mediaSinceMs: Long = 0,
    val decoder: String = "unknown", val codecError: String? = null, val droppedFrames: Int = 0,
) {
    fun attach(name: String, identity: Int, now: Long) = copy(owner = name, view = identity,
        generation = generation + 1, sinceMs = now, firstFrame = false,
        framesByOwner = framesByOwner + (name to false))
    fun detach(identity: Int, now: Long): SurfaceLease = if (identity != view)
        copy(staleDetaches = staleDetaches + 1) else copy(owner = "NONE", view = 0,
            generation = generation + 1, sinceMs = now, firstFrame = false)
    fun mediaChanged(now: Long) = copy(generation = generation + 1, sinceMs = now,
        firstFrame = false, mediaFirstFrame = false, framesByOwner = emptyMap(), mediaSinceMs = now,
        codecError = null, droppedFrames = 0)
    fun frame(eventMs: Long, outputMatches: Boolean): SurfaceLease {
        if (eventMs < mediaSinceMs) return this
        if (owner == "NONE" || eventMs < sinceMs || !outputMatches) return copy(mediaFirstFrame = true)
        return copy(firstFrame = true, mediaFirstFrame = true, lastFrameOwner = owner,
            lastFrameGeneration = generation, framesByOwner = framesByOwner + (owner to true))
    }
    fun warnings(expected: String, readyVideo: Boolean, now: Long): List<String> {
        if (!readyVideo || now - sinceMs < 2_500L) return emptyList()
        return buildList {
            if (codecError != null) add("VIDEO_CODEC_ERROR")
            if (owner == "NONE") add("NO_SURFACE_OWNER")
            else if (owner != expected) add("SURFACE_OWNER_MISMATCH")
            if (!firstFrame) { add("READY_VIDEO_NO_FRAME"); add("FIRST_FRAME_TIMEOUT") }
        }
    }
}
