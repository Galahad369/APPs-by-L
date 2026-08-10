package com.local.listentomusic

import com.local.listentomusic.ui.formatDuration
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.media3.common.C

class FormattingTest {
    @Test
    fun durationResolverIgnoresUnknownAndInvalidCandidates() {
        assertEquals(0L, resolveDurationMs(C.TIME_UNSET, 0L, -4L))
        assertEquals(215_000L, resolveDurationMs(C.TIME_UNSET, 0L, 215_000L, 220_000L))
    }
    @Test fun formatsUnknownDuration() = assertEquals("--:--", formatDuration(0))

    @Test fun formatsMinutesAndSeconds() = assertEquals("2:05", formatDuration(125_000))

    @Test fun formatsHours() = assertEquals("1:02:03", formatDuration(3_723_000))

    @Test fun defaultsToTheSmallestLibraryRow() =
        assertEquals(LibraryRowSize.SMALL, UserPreferences().libraryRowSize)
}
