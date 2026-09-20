package com.local.listentomusic.data

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class StoredPathListCodecTest {
    private val cjkPath = "/storage/emulated/0/Download/我が栄光よ.mp4"

    @Test
    fun `round trip keeps CJK paths whose alphabets differ`() {
        val values = listOf(cjkPath, "/storage/emulated/0/Download/song.flac")

        assertEquals(values, StoredPathListCodec.decode(StoredPathListCodec.encode(values)))
    }

    @Test
    fun `reader accepts legacy standard Base64 path lists`() {
        val legacy = Base64.getEncoder().encodeToString(cjkPath.toByteArray(Charsets.UTF_8))
        check('/' in legacy) // Guards the exact standard-vs-URL-safe regression.

        assertEquals(listOf(cjkPath), StoredPathListCodec.decode(legacy))
    }

    @Test
    fun `reader skips corrupt entries without losing valid favorites`() {
        val valid = StoredPathListCodec.encode(listOf(cjkPath))

        assertEquals(listOf(cjkPath), StoredPathListCodec.decode("not base64!\n$valid"))
    }
}
