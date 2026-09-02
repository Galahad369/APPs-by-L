package com.local.listentomusic.model

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

data class LyricLine(val timeMs: Long, val text: String)

data class LocalLyrics(val sourcePath: String, val lines: List<LyricLine>)

private val timestampPattern = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
private val offsetPattern = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

internal fun parseLrc(content: String): List<LyricLine> {
    val offsetMs = offsetPattern.find(content)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    return content.lineSequence().flatMap { rawLine ->
        val matches = timestampPattern.findAll(rawLine).toList()
        if (matches.isEmpty()) return@flatMap emptySequence()
        val text = rawLine.substring(matches.last().range.last + 1).trim()
        if (text.isEmpty()) return@flatMap emptySequence()
        matches.asSequence().mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            if (seconds > 59L) return@mapNotNull null
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                1 -> fraction.toLongOrNull()?.times(100L)
                2 -> fraction.toLongOrNull()?.times(10L)
                3 -> fraction.toLongOrNull()
                else -> 0L
            } ?: 0L
            LyricLine(
                (minutes * 60_000L + seconds * 1_000L + fractionMs + offsetMs).coerceAtLeast(0L),
                text,
            )
        }
    }.sortedBy(LyricLine::timeMs).toList()
}

internal fun findMatchingLrc(mediaPath: String): File? {
    val media = File(mediaPath)
    val parent = media.parentFile ?: return null
    val direct = File(parent, "${media.nameWithoutExtension}.lrc")
    if (direct.isFile && direct.canRead()) return direct
    val acceptedStems = setOf(
        media.nameWithoutExtension.lowercase(Locale.ROOT),
        media.name.lowercase(Locale.ROOT),
    )
    return parent.listFiles()?.firstOrNull { candidate ->
        candidate.isFile && candidate.canRead() &&
            candidate.extension.equals("lrc", ignoreCase = true) &&
            candidate.nameWithoutExtension.lowercase(Locale.ROOT) in acceptedStems
    }
}

fun loadLocalLyrics(mediaPath: String?): LocalLyrics? {
    if (mediaPath.isNullOrBlank()) return null
    val file = findMatchingLrc(mediaPath) ?: return null
    val lines = runCatching { parseLrc(decodeLyrics(file.readBytes())) }.getOrNull().orEmpty()
    return lines.takeIf { it.isNotEmpty() }?.let { LocalLyrics(file.absolutePath, it) }
}

private fun decodeLyrics(bytes: ByteArray): String {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return bytes.copyOfRange(3, bytes.size).toString(StandardCharsets.UTF_8)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return bytes.copyOfRange(2, bytes.size).toString(StandardCharsets.UTF_16BE)
    }
    return runCatching {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrElse {
        bytes.toString(Charset.forName("Big5"))
    }
}
