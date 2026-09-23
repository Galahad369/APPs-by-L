package com.local.listentomusic.model

import kotlin.math.roundToInt

/** Content only: system navigation insets belong to the host, never the player. */
object CompactPlayerMetrics {
    const val HEIGHT_DP = 61.5f
    const val DETACHED_WIDTH_DP = 360f
    fun heightPx(density: Float) = (HEIGHT_DP * density).roundToInt().coerceAtLeast(1)
    fun widthPx(density: Float, available: Int) =
        (DETACHED_WIDTH_DP * density).roundToInt().coerceAtMost(available).coerceAtLeast(1)
}
