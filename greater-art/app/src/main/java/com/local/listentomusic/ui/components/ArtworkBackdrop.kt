package com.local.listentomusic.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cached artwork only: no extra video decoder, frame readback, or work while scrolling. */
@Composable
internal fun artworkBackdrop(artwork: Bitmap?, light: Boolean): Brush {
    val colors by produceState(artworkGradientColors(intArrayOf(), light), artwork, light) {
        value = withContext(Dispatchers.Default) {
            val pixels = runCatching {
                if (artwork == null || artwork.isRecycled) return@runCatching intArrayOf()
                val readable = if (artwork.config == Bitmap.Config.HARDWARE)
                    artwork.copy(Bitmap.Config.ARGB_8888, false) else artwork
                try {
                    if (readable == null) intArrayOf() else IntArray(144) { index ->
                        readable.getPixel(index % 12 * readable.width / 12, index / 12 * readable.height / 12)
                    }
                } finally { if (readable !== artwork) readable?.recycle() }
            }.getOrDefault(intArrayOf())
            artworkGradientColors(pixels, light)
        }
    }
    return remember(colors) { Brush.verticalGradient(colors) }
}

internal fun artworkGradientColors(pixels: IntArray, light: Boolean): List<Color> {
    val base = if (light) Color(0xFFF6F7F6) else Color(0xFF090D10)
    val usable = pixels.filter { (it ushr 24) >= 128 }
    if (usable.isEmpty()) return listOf(base, base)
    // Quantized RGB histogram gives a representative color, not a grey average.
    // Ignore black letterboxing and near-neutral shadows when real color exists.
    val colorful = usable.filter {
        val channels = listOf((it shr 16) and 255, (it shr 8) and 255, it and 255)
        channels.max() >= 64 && channels.max() - channels.min() >= 32
    }
    val dominant = colorful.ifEmpty { usable }.groupBy { ((it shr 20) and 15) * 256 + ((it shr 12) and 15) * 16 + ((it shr 4) and 15) }
        .maxBy { it.value.size }.value
    val tint = Color(
        red = dominant.map { (it shr 16) and 255 }.average().toFloat() / 255f,
        green = dominant.map { (it shr 8) and 255 }.average().toFloat() / 255f,
        blue = dominant.map { it and 255 }.average().toFloat() / 255f,
    )
    // Conservative tint strengths keep both light and dark foregrounds readable.
    return listOf(lerp(base, tint, if (light) .14f else .30f), lerp(base, tint, if (light) .05f else .10f))
}
