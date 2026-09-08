package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PracticeRange(val path: String? = null, val start: Long? = null, val end: Long? = null)

/** Transient practice markers. Never exported as listening history. Main-thread owned. */
object PracticeLoop {
    private val mutable = MutableStateFlow(PracticeRange())
    val state = mutable.asStateFlow()
    fun mark(path: String?, position: Long) {
        if (path == null) return
        val old = mutable.value
        mutable.value = when {
            old.path != path || old.start == null -> PracticeRange(path, position)
            old.end == null && position > old.start + 250 -> old.copy(end = position)
            else -> PracticeRange()
        }
    }
    fun clear() { mutable.value = PracticeRange() }
}
