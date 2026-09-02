package com.local.listentomusic.model

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLyricsTest {
    @Test
    fun parsesFractionsMultipleTimestampsAndOffset() {
        val lines = parseLrc(
            """
            [offset:-100]
            [00:01.50]First
            [00:02.005][00:03.5]Echo
            [ar:Ignored metadata]
            """.trimIndent(),
        )

        assertEquals(listOf(1_400L, 1_905L, 3_400L), lines.map(LyricLine::timeMs))
        assertEquals(listOf("First", "Echo", "Echo"), lines.map(LyricLine::text))
    }

    @Test
    fun rejectsInvalidSecondsAndClampsNegativeOffset() {
        val lines = parseLrc("[offset:-500]\n[00:00.10]Start\n[01:88.00]Bad")
        assertEquals(1, lines.size)
        assertEquals(0L, lines.single().timeMs)
    }

    @Test
    fun findsCaseInsensitiveSiblingLrc() {
        val folder = createTempDirectory("greater-art-lyrics-").toFile()
        try {
            val media = File(folder, "Example Song.mp3").apply { writeBytes(byteArrayOf()) }
            val lyrics = File(folder, "EXAMPLE SONG.LRC").apply { writeText("[00:01]Line") }
            assertEquals(lyrics.canonicalPath, findMatchingLrc(media.path)?.canonicalPath)
            assertTrue(loadLocalLyrics(media.path)?.lines?.single()?.text == "Line")
        } finally {
            folder.deleteRecursively()
        }
    }
}
