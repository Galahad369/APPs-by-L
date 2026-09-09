package com.local.listentomusic.ui.components

import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import java.lang.ref.WeakReference

/** Only one in-app view may own the shared player's video output. Main thread only. */
object VideoSurfaceOwner {
    private var active = WeakReference<PlayerView>(null)
    fun attach(player: Player?, target: PlayerView) {
        if (player == null) { detach(target); return }
        val previous = active.get()
        if (previous === target && target.player === player) return
        if (previous?.player === player) PlayerView.switchTargetView(player, previous, target)
        else { previous?.player = null; target.player = player }
        active = WeakReference(target)
    }
    fun detach(view: PlayerView) {
        // An outgoing AnimatedContent page must not clear the incoming page's surface.
        view.player = null
        if (active.get() === view) active.clear()
    }
}
