package com.local.localkit.core

object TextOperations {
    fun stats(text: String): Triple<Int, Int, Int> = Triple(
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() },
        text.length,
        text.lineSequence().count()
    )

    fun titleCase(text: String): String {
        var capitalizeNext = true
        return buildString(text.length) {
            text.lowercase().forEach { char ->
                append(if (capitalizeNext && char.isLetter()) char.titlecase() else char)
                capitalizeNext = char.isWhitespace()
            }
        }
    }

    fun dedupeLines(text: String): String = text.lineSequence().distinct().joinToString("\n")
    fun sortLines(text: String): String = text.lineSequence().sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString("\n")
    fun compactWhitespace(text: String): String = text.trim().replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n")
}
