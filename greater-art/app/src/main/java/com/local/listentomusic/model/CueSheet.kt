package com.local.listentomusic.model

import java.io.File

private val cueSuffix = Regex("#ga-cue=(\\d+):(\\d*)$")
fun sourceMediaPath(id: String): String = id.replace(cueSuffix, "")
fun cueBounds(id: String): Pair<Long, Long?>? = cueSuffix.find(id)?.let {
    val start = it.groupValues[1].toLongOrNull() ?: return@let null
    val end = it.groupValues[2].toLongOrNull()
    if (end != null && end <= start) null else start to end
}

internal data class CueEntry(val file: String, val title: String, val artist: String, val album: String, val startMs: Long)

/** INDEX 01 addresses are CD frames (75 per second), not decimal milliseconds. */
internal fun parseCue(text: String): List<CueEntry> {
    var source = ""; var album = ""; var albumArtist = ""; var title = ""; var artist = ""
    var track: Int? = null
    val entries = mutableListOf<CueEntry>()
    text.lineSequence().take(20_000).forEach { raw ->
        val line = raw.trim().removePrefix("\uFEFF")
        val command = line.substringBefore(' ').uppercase()
        val value = line.substringAfter(' ', "").trim()
        when (command) {
            "FILE" -> { source = if (value.startsWith('"')) value.substringAfter('"').substringBefore('"') else value.substringBeforeLast(' '); track = null }
            "TRACK" -> { track = value.substringBefore(' ').toIntOrNull().takeIf { value.endsWith("AUDIO", true) }; title = ""; artist = albumArtist }
            "TITLE" -> if (track == null) album = value.trim('"') else title = value.trim('"')
            "PERFORMER" -> if (track == null) albumArtist = value.trim('"') else artist = value.trim('"')
            "INDEX" -> if (track != null && value.startsWith("01 ") && source.isNotBlank()) {
                val time = value.substringAfter(' ').trim().split(':').map { it.toLongOrNull() }
                if (time.size == 3 && time.all { it != null } && time[0]!! in 0..100_000 && time[1]!! in 0..59 && time[2]!! in 0..74) {
                    entries += CueEntry(source, title.ifBlank { "Track $track" }, artist, album,
                        time[0]!! * 60_000 + time[1]!! * 1_000 + time[2]!! * 1_000 / 75)
                }
            }
        }
    }
    return entries
}

/** Resolve only files already accepted by the scoped scanner, never arbitrary CUE paths. */
internal fun expandCueSheets(files: List<MediaFile>, sheets: List<File>): List<MediaFile> {
    val allowed = files.associateBy { runCatching { File(it.sourcePath).canonicalPath }.getOrDefault(it.sourcePath) }
    val replaced = mutableSetOf<String>(); val tracks = mutableListOf<MediaFile>()
    sheets.sortedBy { it.path }.forEach { cue ->
        val entries = runCatching { if (cue.length() > 1_048_576) emptyList() else parseCue(cue.readText()) }.getOrDefault(emptyList())
        val resolved = entries.mapNotNull { entry ->
            val source = runCatching { File(cue.parentFile, entry.file.replace('\\', '/')).canonicalPath }.getOrNull()
            allowed[source]?.takeIf { it.kind == MediaKind.AUDIO }?.let { entry to it }
        }
        resolved.groupBy { it.second.path }.forEach { (path, group) ->
            if (path in replaced || group.isEmpty() || group.zipWithNext().any { (a, b) -> b.first.startMs <= a.first.startMs }) return@forEach
            group.forEachIndexed { index, (entry, file) ->
                val end = group.getOrNull(index + 1)?.first?.startMs
                tracks += file.copy(path = "$path#ga-cue=${entry.startMs}:${end ?: ""}", name = entry.title,
                    sourcePath = file.sourcePath, clipStartMs = entry.startMs, clipEndMs = end,
                    durationMs = end?.minus(entry.startMs) ?: 0L, artist = entry.artist, album = entry.album)
            }
            replaced += path
        }
    }
    return files.filterNot { it.path in replaced } + tracks
}
