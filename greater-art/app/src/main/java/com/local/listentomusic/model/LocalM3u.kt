package com.local.listentomusic.model

/** Standard paths remain usable by other players; VLC options preserve optional CUE clipping. */
internal fun exportM3u(files: List<MediaFile>): String = buildString {
    appendLine("#EXTM3U")
    files.forEach { file ->
        appendLine("#EXTINF:${if (file.durationMs > 0) file.durationMs / 1000 else -1},${file.name.replace('\n', ' ').replace('\r', ' ')}")
        if (file.path != file.sourcePath) {
            appendLine("#EXTVLCOPT:start-time=${file.clipStartMs / 1000.0}")
            file.clipEndMs?.let { appendLine("#EXTVLCOPT:stop-time=${it / 1000.0}") }
        }
        appendLine(file.sourcePath)
    }
}

internal data class M3uEntry(val path: String, val start: Long? = null, val end: Long? = null)
internal fun parseM3u(lines: List<String>): List<M3uEntry> {
    var start: Long? = null; var end: Long? = null
    val result = mutableListOf<M3uEntry>()
    lines.take(100_000).forEach { raw ->
        val line = raw.trim().removePrefix("\uFEFF")
        fun milliseconds(): Long? = line.substringAfter('=').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.times(1000)?.toLong()
        when {
            line.startsWith("#EXTVLCOPT:start-time=") -> start = milliseconds()
            line.startsWith("#EXTVLCOPT:stop-time=") -> end = milliseconds()
            line.isNotEmpty() && !line.startsWith('#') -> { result += M3uEntry(line, start, end); start = null; end = null }
        }
    }
    return result
}
