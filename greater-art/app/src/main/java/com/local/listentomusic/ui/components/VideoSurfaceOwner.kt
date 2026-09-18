package com.local.listentomusic.ui.components

import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import java.lang.ref.WeakReference

/** Only one in-app view may own the shared player's video output. Main thread only. */
object VideoSurfaceOwner {
    private var active = WeakReference<PlayerView>(null)
    private var activityForeground = false
    fun setActivityForeground(value: Boolean) { activityForeground = value }
    fun attach(player: Player?, target: PlayerView, overlay: Boolean = false) {
        // A queued service callback must not steal output after the Activity resumes.
        if (overlay && activityForeground) { detach(target); return }
        if (player == null) { detach(target); return }
        val previous = active.get()
        if (previous === target && target.player === player) return
        if (com.local.listentomusic.BuildConfig.DEBUG) android.util.Log.d("GreaterArtSurface",
            "attach player=${System.identityHashCode(player)} from=${previous?.tag}:${System.identityHashCode(previous)} to=${target.tag}:${System.identityHashCode(target)} overlay=$overlay")
        if (previous?.player === player) PlayerView.switchTargetView(player, previous, target)
        else { previous?.player = null; target.player = player }
        active = WeakReference(target)
    }
    fun detach(view: PlayerView) {
        if (view.player != null && com.local.listentomusic.BuildConfig.DEBUG) android.util.Log.d("GreaterArtSurface",
            "detach target=${view.tag}:${System.identityHashCode(view)} active=${active.get() === view}")
        // An outgoing AnimatedContent page must not clear the incoming page's surface.
        view.player = null
        if (active.get() === view) active.clear()
    }
}
