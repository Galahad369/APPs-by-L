package com.local.listentomusic.data

import java.util.Base64

/**
 * Stable codec for newline-delimited paths stored in Preferences DataStore.
 *
 * Early settings used standard Base64 while Favorites and excluded folders briefly
 * wrote URL-safe Base64. Read both so existing custom orders remain valid and paths
 * whose encoding contains '/' or '_' (common with CJK filenames) do not disappear.
 */
internal object StoredPathListCodec {
    fun encode(values: List<String>): String = values.distinct().joinToString("\n") { value ->
        Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    fun decode(encoded: String): List<String> = encoded.lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull(::decodeValue)
        .toList()

    private fun decodeValue(value: String): String? {
        val bytes = runCatching { Base64.getUrlDecoder().decode(value) }
            .recoverCatching { Base64.getDecoder().decode(value) }
            .getOrNull()
            ?: return null
        return bytes.toString(Charsets.UTF_8)
    }
}
