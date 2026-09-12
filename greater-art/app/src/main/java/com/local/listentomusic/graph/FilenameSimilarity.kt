package com.local.listentomusic.graph

import java.text.Normalizer
import java.util.Locale

/** Filename text only. Display overrides, tags and playback history never enter this engine. */
object FilenameSimilarity {
    private val extensions = setOf("mp3", "mp4", "mkv", "mov", "flac", "m4a", "wav", "aac", "aiff", "aif", "opus", "ogg", "ape", "dsf", "dff", "alac", "wma", "webm", "m4v", "3gp", "ts", "mpeg", "mpg", "flv", "avi", "mka", "oga", "amr", "ac3", "eac3")
    data class Features(val text: String, val tokens: Set<String>, val chars: Map<String, Int>,
        val bigrams: Map<String, Int>, val trigrams: Map<String, Int>, val points: IntArray)

    fun features(filename: String): Features {
        val leaf = filename.substringAfterLast('/').substringAfterLast('\\')
        val stem = if (leaf.substringAfterLast('.', "").lowercase(Locale.ROOT) in extensions)
            leaf.substringBeforeLast('.') else leaf
        val normalized = Normalizer.normalize(stem, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val runs = mutableListOf<String>()
        val run = StringBuilder()
        var wasCjk: Boolean? = null
        fun flush() { if (run.isNotEmpty()) runs.add(run.toString()); run.setLength(0) }
        normalized.codePoints().forEach { cp ->
            if (Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK.toInt()) {
                val script = Character.UnicodeScript.of(cp)
                val cjk = script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA ||
                    script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL ||
                    ((cp == 0x30FC || Character.getType(cp) == Character.NON_SPACING_MARK.toInt()) && wasCjk == true)
                if (wasCjk != null && wasCjk != cjk) flush()
                run.appendCodePoint(cp)
                wasCjk = cjk
            } else { flush(); wasCjk = null }
        }
        flush()
        val points = runs.joinToString("").codePoints().toArray()
        fun grams(n: Int): Map<String, Int> = runs.flatMap { token ->
            val p = token.codePoints().toArray()
            (0..(p.size - n)).map { String(p, it, n) }
        }.groupingBy { it }.eachCount()
        return Features(runs.joinToString(" "), runs.toSet(), grams(1), grams(2), grams(3), points)
    }

    fun score(a: String, b: String): Double = score(features(a), features(b))
    fun score(a: Features, b: Features): Double {
        if (a.points.isEmpty() || b.points.isEmpty()) return 0.0
        if (a.text == b.text) return 1.0
        val tokenUnion = (a.tokens + b.tokens).size
        val tokens = a.tokens.intersect(b.tokens).size.toDouble() / tokenUnion.coerceAtLeast(1)
        var prefix = 0
        while (prefix < minOf(a.points.size, b.points.size) && a.points[prefix] == b.points[prefix]) prefix++
        return (0.35 * tokens + 0.30 * dice(a.bigrams, b.bigrams) + 0.10 * dice(a.trigrams, b.trigrams) +
            0.20 * dice(a.chars, b.chars) + 0.05 * prefix / maxOf(a.points.size, b.points.size)).coerceIn(0.0, 1.0)
    }

    private fun dice(a: Map<String, Int>, b: Map<String, Int>): Double {
        val total = a.values.sum() + b.values.sum()
        if (total == 0) return 0.0
        return 2.0 * a.entries.sumOf { minOf(it.value, b[it.key] ?: 0) } / total
    }
}
