package com.local.listentomusic.model

import kotlin.math.max

/** Shared physical size contract for the overlay and the default Library thumbnail. */
object MiniWindowMetrics {
    const val WIDTH_DP = 111
    const val HEIGHT_DP = 64
    // v1.10 already removed 3 physical px; v1.10.1 removes 3 more as requested.
    const val SHRINK_PX = 6

    fun widthPx(density: Float): Int = max(1, (WIDTH_DP * density).toInt() - SHRINK_PX)
    fun heightPx(density: Float): Int = max(1, (HEIGHT_DP * density).toInt() - SHRINK_PX)
}
