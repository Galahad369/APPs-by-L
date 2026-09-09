package com.local.listentomusic

import com.local.listentomusic.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class CueSheetTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun cdFramesAreSeventyFivePerSecond() {
        val entries = parseCue("""
            TITLE "Album"
            PERFORMER "Artist"
            FILE "album.flac" WAVE
              TRACK 01 AUDIO
                TITLE "First"
                INDEX 00 00:00:00
                INDEX 01 00:01:37
              TRACK 02 AUDIO
                TITLE "Second"
                INDEX 01 03:00:00
        """.trimIndent())
        assertEquals(2, entries.size)
        assertEquals(1493L, entries[0].startMs)
        assertEquals("Album", entries[0].album)
        assertEquals("Artist", entries[0].artist)
    }
    @Test fun invalidFramesAndDataTracksAreIgnored() {
        assertTrue(parseCue("FILE a.flac WAVE\nTRACK 01 AUDIO\nINDEX 01 00:00:75\nTRACK 02 MODE1/2352\nINDEX 01 01:00:00").isEmpty())
    }
    @Test fun scopedExpansionCreatesClipsWithoutChangingSource() {
        val audio = temporary.newFile("album.flac").apply { writeText("source unchanged") }
        val cue = temporary.newFile("album.cue").apply { writeText("FILE \"album.flac\" WAVE\nTRACK 01 AUDIO\nTITLE \"A\"\nINDEX 01 00:00:00\nTRACK 02 AUDIO\nTITLE \"B\"\nINDEX 01 01:00:00") }
        val tracks = expandCueSheets(listOf(MediaFile(audio.path, audio.name, 0, audio.length(), 0, MediaKind.AUDIO)), listOf(cue))
        assertEquals(2, tracks.size)
        assertEquals(60_000L, tracks[0].clipEndMs)
        assertEquals(audio.path, tracks[1].sourcePath)
        assertEquals(audio.path, sourceMediaPath(tracks[1].path))
        assertEquals("source unchanged", audio.readText())
    }
    @Test fun cueCannotIntroduceUnscannedFiles() {
        val cue = temporary.newFile("outside.cue").apply { writeText("FILE \"../private.flac\" WAVE\nTRACK 01 AUDIO\nINDEX 01 00:00:00") }
        assertTrue(expandCueSheets(emptyList(), listOf(cue)).isEmpty())
    }
    @Test fun encodedBoundsRejectBackwardsRange() {
        assertNull(cueBounds("a.flac#ga-cue=20:10"))
        assertEquals(20L to null, cueBounds("a.flac#ga-cue=20:"))
    }
    @Test fun m3uPreservesCueClipsAndRealPaths() {
        val file = MediaFile("/Download/album.flac#ga-cue=1000:5000", "Song", 4000, 100, 0, MediaKind.AUDIO,
            sourcePath = "/Download/album.flac", clipStartMs = 1000, clipEndMs = 5000)
        val exported = exportM3u(listOf(file))
        assertFalse(exported.contains("#ga-cue="))
        val entry = parseM3u(exported.lines()).single()
        assertEquals(file.sourcePath, entry.path)
        assertEquals(1000L, entry.start)
        assertEquals(5000L, entry.end)
    }
}
