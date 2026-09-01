package com.local.listentomusic

import com.local.listentomusic.ui.formatDuration
import com.local.listentomusic.data.AppLanguage
import com.local.listentomusic.data.FloatingWindowMode
import com.local.listentomusic.data.LibraryRowSize
import com.local.listentomusic.data.UserPreferences
import com.local.listentomusic.data.ThemeMode
import com.local.listentomusic.data.AppBackgroundMode
import com.local.listentomusic.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import androidx.media3.common.C
import androidx.media3.common.Player

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

    @Test fun requestedPlaybackAndPresentationDefaultsRemainStable() {
        val defaults = UserPreferences()
        assertEquals(Player.REPEAT_MODE_ONE, defaults.repeatMode)
        assertEquals(FloatingWindowMode.FOLLOW_VIDEO, defaults.floatingWindowMode)
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(AppLanguage.ENGLISH, defaults.appLanguage)
        assertTrue(defaults.preloadThumbnails)
        assertFalse(defaults.showFileDetails)
        assertEquals(AppBackgroundMode.DEFAULT, defaults.backgroundMode)
        assertEquals(0.55f, defaults.backgroundDim, 0.001f)
        assertEquals(
            setOf(SortMode.CUSTOM, SortMode.NAME_ASC, SortMode.NAME_DESC),
            SortMode.entries.toSet(),
        )
    }
}
