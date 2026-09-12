package com.local.listentomusic.model

import org.junit.Assert.*
import org.junit.Test

class PlaybackLibraryPolicyTest {
    private fun audio(id: String) = MediaFile(id, id, 0, 1, 1, MediaKind.AUDIO)
    @Test fun graphTapOutsidePlaylistStillHasOtherSongs() {
        val a = audio("a"); val b = audio("b"); val c = audio("c")
        assertEquals(listOf(c,a,b), browsingQueue(c, listOf(a,b), listOf(a,b,c)))
    }
    @Test fun explicitPlaylistOrderIsPreserved() {
        val a = audio("a"); val b = audio("b")
        assertEquals(listOf(b,a), browsingQueue(a, listOf(b,a), listOf(a,b)))
    }
    @Test fun warmupWorksBeforePlaybackStarts() {
        assertEquals(listOf("a", "b"), waveformWarmupPaths(emptyList(), listOf(audio("a"),audio("b")), null))
    }
    @Test fun videosDoNotConsumeTheAudioWarmupBudget() {
        val a = audio("a"); val b = audio("b")
        val video = audio("video").copy(kind = MediaKind.VIDEO)
        assertEquals(listOf("b", "a"), waveformWarmupPaths(listOf(a,video,b), emptyList(), "video"))
    }
    @Test fun cueSourcesAreDecodedOnlyOnce() {
        val a = audio("cue1").copy(sourcePath="album.flac")
        assertEquals(listOf("cue1"), waveformWarmupPaths(listOf(a,a.copy(path="cue2")), emptyList(), null))
    }
}
