package com.local.listentomusic.model

import java.text.Normalizer
import java.util.Locale

/** Local display changes only. The source file is never rewritten. */
data class LocalOverride(val title: String = "", val coverUri: String = "")

data class PlaylistRule(val folder: String = "", val extension: String = "", val text: String = "") {
    fun matches(file: MediaFile, root: String): Boolean {
        val relative = file.sourcePath.removePrefix(root.trimEnd('/') + "/")
        val wanted = folder.trim().trim('/').replace('\\', '/')
        return (wanted.isEmpty() || relative.startsWith("$wanted/", true)) &&
            (extension.isBlank() || file.sourcePath.substringAfterLast('.').equals(extension.trim().removePrefix("."), true)) &&
            (text.isBlank() || searchText(file.name).contains(searchText(text.trim())))
    }
}

fun searchText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

fun mediaTitle(title: String, path: String?): String {
    val extension = path?.let(::sourceMediaPath)?.substringAfterLast('.', "").orEmpty()
    return if (extension.isNotBlank() && title.endsWith(".$extension", true)) title.dropLast(extension.length + 1) else title
}

/** Number runs compare without integer conversion, so very long filenames cannot overflow. */
val naturalNames: Comparator<String> = Comparator { left, right ->
    val a = searchText(left); val b = searchText(right)
    var i = 0; var j = 0; var result = 0
    while (i < a.length && j < b.length && result == 0) {
        if (a[i] in '0'..'9' && b[j] in '0'..'9') {
            val ai = i; val bj = j
            while (i < a.length && a[i] in '0'..'9') i++
            while (j < b.length && b[j] in '0'..'9') j++
            val x = a.substring(ai, i).trimStart('0').ifEmpty { "0" }
            val y = b.substring(bj, j).trimStart('0').ifEmpty { "0" }
            result = x.length.compareTo(y.length).takeIf { it != 0 } ?: x.compareTo(y)
        } else { result = a[i++].compareTo(b[j++]) }
    }
    if (result != 0) result else (a.length - i).compareTo(b.length - j).takeIf { it != 0 }
        ?: left.compareTo(right)
}
