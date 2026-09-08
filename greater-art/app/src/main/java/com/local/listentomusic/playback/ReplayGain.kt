package com.local.listentomusic.playback

import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import kotlin.math.log10
import kotlin.math.min

internal fun safeReplayGainDb(gain: Double, peak: Double?): Double {
    if (!gain.isFinite()) return 0.0
    // Missing peak: attenuation only. Never guess that there is headroom to boost.
    val ceiling = if (peak != null && peak.isFinite() && peak > 0) -20.0 * log10(peak) else 0.0
    return min(gain.coerceIn(-60.0, 12.0), ceiling).coerceAtLeast(-60.0)
}

internal fun replayGainDb(tracks: Tracks): Double {
    val tags = mutableMapOf<String, String>()
    tracks.groups.filter { it.isSelected }.forEach { group ->
        for (track in 0 until group.length) {
            val metadata = group.getTrackFormat(track).metadata ?: continue
            for (index in 0 until metadata.length()) {
                when (val entry = metadata[index]) {
                    is VorbisComment -> tags[entry.key.uppercase(java.util.Locale.ROOT)] = entry.value
                    is TextInformationFrame -> entry.description?.let {
                        tags[it.uppercase(java.util.Locale.ROOT)] = entry.values.firstOrNull().orEmpty()
                    }
                }
            }
        }
    }
    fun number(key: String) = tags[key]?.trim()?.substringBefore(' ')?.toDoubleOrNull()
    val gain = number("REPLAYGAIN_TRACK_GAIN") ?: return 0.0
    return safeReplayGainDb(gain, number("REPLAYGAIN_TRACK_PEAK"))
}
