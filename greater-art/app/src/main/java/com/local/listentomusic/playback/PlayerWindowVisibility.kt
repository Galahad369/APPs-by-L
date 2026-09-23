package com.local.listentomusic.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun showDetachedPlayer(libraryVisible: Boolean, expandedVisible: Boolean) =
    !libraryVisible && !expandedVisible

/** Presentation visibility, independent of video-surface readiness during transfer. */
internal object PlayerWindowVisibility {
    private var libraryVisible = false
    private var expandedVisible = false
    private val detached = MutableStateFlow(true)
    private val library = MutableStateFlow(false)
    private val docked = MutableStateFlow(false)
    val detachedVisible = detached.asStateFlow()
    val libraryShowing = library.asStateFlow()
    val dockedVisible = docked.asStateFlow()
    fun library(visible: Boolean) { libraryVisible = visible; library.value = visible; publish() }
    fun expanded(visible: Boolean) { expandedVisible = visible; publish() }
    private fun publish() {
        detached.value = showDetachedPlayer(libraryVisible, expandedVisible)
        docked.value = libraryVisible && !expandedVisible
    }
}
