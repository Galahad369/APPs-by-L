package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackConnectionTest {
    @Test fun handoffBorrowsTheSameConnectionUntilLastPresentationCloses() {
        var created = 0
        var released = 0
        val pool = SharedPlaybackResource<Any> { released++ }
        fun open() = pool.acquire { created++; Any() }
        val library = open()
        val expanded = open()
        assertSame(library.value, expanded.value)
        library.close()
        val mini = open()
        expanded.close()
        expanded.close()
        assertEquals(1, created)
        assertEquals(0, released)
        assertSame(expanded.value, mini.value)
        mini.close()
        assertEquals(1, released)
        val nextSession = open()
        assertNotSame(mini.value, nextSession.value)
        assertEquals(2, created)
        nextSession.close()
        assertEquals(2, released)
    }

    @Test fun failedCreationDoesNotKeepAPhantomBorrower() {
        var released = 0
        val pool = SharedPlaybackResource<Any> { released++ }
        runCatching { pool.acquire { error("Connection failed") } }
        pool.acquire { Any() }.close()
        assertEquals(1, released)
    }
}
