package com.local.listentomusic

import com.local.listentomusic.model.*
import org.junit.Assert.*
import org.junit.Test

class TimedLyricsTest {
    @Test fun enhancedLrcRetainsWordTimesAndOffsets() {
        val line = parseLrc("[offset:100]\n[00:01.00]<00:01.00>Hello <00:01.50>world").single()
        assertEquals("Hello world", line.text)
        assertEquals(listOf(1100L, 1600L), line.words.map { it.timeMs })
    }
    @Test fun ordinaryLrcHasNoInventedWordTiming() {
        assertTrue(parseLrc("[00:01]ordinary lyrics").single().words.isEmpty())
    }
    @Test fun srtAcceptsCommaMillisecondsAndMultiline() {
        val line = parseSrt("1\n00:01:02,300 --> 00:01:05,000\nHello\nworld").single()
        assertEquals(62_300L, line.timeMs)
        assertEquals("Hello\nworld", line.text)
    }
    @Test fun ttmlHandlesNamespacedTimedWords() {
        val line = parseTtml("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:00:01.000"><span begin="00:00:01.100">Hello </span><span begin="00:00:01.500">world</span></p></div></body></tt>""").single()
        assertEquals(1000L, line.timeMs)
        assertEquals(listOf(1100L, 1500L), line.words.map { it.timeMs })
    }
    @Test(expected = IllegalArgumentException::class) fun externalEntitiesAreRejected() {
        parseTtml("<!DOCTYPE tt [<!ENTITY x SYSTEM 'file:///private'>]><tt><p begin='1s'>&x;</p></tt>")
    }
    @Test fun malformedClockDoesNotOverflowOrAcceptNan() {
        assertNull(lyricClock("NaNs")); assertNull(lyricClock("00:99:00"))
        assertEquals(1250L, lyricClock("1.25s"))
    }
}
