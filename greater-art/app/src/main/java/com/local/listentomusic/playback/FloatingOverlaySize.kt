package com.local.listentomusic.playback

import kotlin.math.roundToInt

internal data class FloatingOverlaySize(val widthPx: Int, val heightPx: Int)

/** Percentages are applied to the safe display area; dp values are upper bounds, never minimums. */
internal fun floatingOverlaySize(safeWidthPx: Int, safeHeightPx: Int, density: Float, fullscreen: Boolean = false): FloatingOverlaySize {
    val width = safeWidthPx.coerceAtLeast(1)
    val height = safeHeightPx.coerceAtLeast(1)
    val scale = density.coerceAtLeast(0.1f)
    if (fullscreen) return FloatingOverlaySize(width, height)
    return FloatingOverlaySize(
        widthPx = minOf((width * 0.96f).roundToInt(), (620f * scale).roundToInt(), width).coerceAtLeast(1),
        heightPx = minOf((height * 0.90f).roundToInt(), (860f * scale).roundToInt(), height).coerceAtLeast(1),
    )
}
