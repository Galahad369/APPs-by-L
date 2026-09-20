package com.local.listentomusic.playback

/**
 * Same-process bridge for a momentary speed gesture. The service owns the saved
 * speed so a long hold can never overwrite the user's persistent preference.
 */
object TemporaryPlaybackSpeed {
    internal var beginCommand: (() -> Boolean)? = null
    internal var endCommand: (() -> Unit)? = null

    fun begin(): Boolean = beginCommand?.invoke() == true
    fun end() { endCommand?.invoke() }
    internal fun detach() { beginCommand = null; endCommand = null }
}
