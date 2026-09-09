package com.local.listentomusic.model

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

internal fun lyricClock(value: String): Long? {
    val clean = value.trim().replace(',', '.')
    if (clean.endsWith("ms")) return clean.removeSuffix("ms").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.toLong()
    if (clean.endsWith("s")) return clean.removeSuffix("s").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.times(1000)?.toLong()
    val parts = clean.split(':').map { it.toDoubleOrNull() ?: return null }
    if (parts.size !in 2..3 || parts.any { !it.isFinite() || it < 0 } || parts.last() >= 60 || (parts.size == 3 && parts[1] >= 60)) return null
    return (parts.fold(0.0) { sum, number -> sum * 60 + number } * 1000).toLong()
}

internal fun parseSrt(content: String): List<LyricLine> = content.replace("\r", "").split(Regex("\n\\s*\n")).mapNotNull { block ->
    val lines = block.lines()
    val index = lines.indexOfFirst { "-->" in it }
    if (index < 0) return@mapNotNull null
    val time = lyricClock(lines[index].substringBefore("-->")) ?: return@mapNotNull null
    val text = lines.drop(index + 1).joinToString("\n").replace(Regex("<[^>]*>"), "").trim()
    text.takeIf { it.isNotEmpty() }?.let { LyricLine(time, it) }
}.sortedBy { it.timeMs }

/** Local bounded TTML. Reject DTD/entities before XML parsing: never resolve external resources. */
internal fun parseTtml(content: String): List<LyricLine> {
    require(content.length <= 1_048_576 && !content.contains("<!DOCTYPE", true) && !content.contains("<!ENTITY", true))
    val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; isExpandEntityReferences = false }
    val document = factory.newDocumentBuilder().parse(InputSource(StringReader(content)))
    val paragraphs = document.getElementsByTagNameNS("*", "p")
    return (0 until paragraphs.length).take(10_000).mapNotNull { index ->
        val p = paragraphs.item(index) as? Element ?: return@mapNotNull null
        val start = lyricClock(p.getAttribute("begin")) ?: return@mapNotNull null
        val words = mutableListOf<LyricWord>()
        val text = StringBuilder()
        fun visit(node: Node, inherited: Long, depth: Int) {
            if (depth > 32) return
            if (node.nodeType == Node.TEXT_NODE) {
                val value = node.nodeValue.replace(Regex("\\s+"), " ")
                if (value.isNotEmpty()) { text.append(value); words += LyricWord(inherited, value) }
            } else {
                val element = node as? Element
                if (element?.localName == "br") { text.append('\n'); words += LyricWord(inherited, "\n"); return }
                val rawTime = element?.getAttribute("begin").orEmpty()
                // Clock timestamps are absolute in common karaoke TTML exports;
                // explicit offset units (s/ms) on spans are relative to the parent.
                val time = lyricClock(rawTime)?.let { if (':' in rawTime) it else inherited + it } ?: inherited
                for (child in 0 until node.childNodes.length) visit(node.childNodes.item(child), time, depth + 1)
            }
        }
        for (child in 0 until p.childNodes.length) visit(p.childNodes.item(child), start, 0)
        text.toString().takeIf { it.isNotBlank() }?.let { LyricLine(start, it, words.sortedBy { word -> word.timeMs }) }
    }.sortedBy { it.timeMs }
}
