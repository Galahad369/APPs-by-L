package com.local.listentomusic.playback


internal data class FloatingOverlaySize(val widthPx: Int, val heightPx: Int)

/** The expanded player fills the already-inset frame: no second set of gutters. */
internal fun floatingOverlaySize(safeWidthPx: Int, safeHeightPx: Int): FloatingOverlaySize {
    val width = safeWidthPx.coerceAtLeast(1)
    val height = safeHeightPx.coerceAtLeast(1)
    return FloatingOverlaySize(width, height)
}

internal fun pullDismissReached(distancePx: Int, density: Float): Boolean =
    distancePx >= (72f * density.coerceAtLeast(.1f)).toInt()
