package com.local.listentomusic

import com.local.listentomusic.model.*
import com.local.listentomusic.data.UserPreferences
import org.junit.Assert.*
import org.junit.Test

class LibraryToolsTest {
    @Test fun numericRunsSortNaturallyWithoutOverflow() {
        val actual = listOf("Track 10", "Track 2", "Track 1000000000000000000000000", "Track 1").sortedWith(naturalNames)
        assertEquals(listOf("Track 1", "Track 2", "Track 10", "Track 1000000000000000000000000"), actual)
    }
    @Test fun normalizationMatchesAccentsButPreservesCjk() {
        assertEquals(searchText("Sigur Rós"), searchText("sigur ros"))
        assertEquals("廣東歌", searchText("廣東歌"))
    }
    @Test fun rulesRequireEveryFilledFieldAndRealFolderBoundary() {
        val file = MediaFile("/Download/Concerts/Ado.flac", "Ado live", 0, 100, 1, MediaKind.AUDIO)
        assertTrue(PlaylistRule("Concerts", ".FLAC", "ado").matches(file, "/Download"))
        assertFalse(PlaylistRule("Concert", "", "").matches(file, "/Download"))
        assertFalse(PlaylistRule("Concerts", "mp3", "").matches(file, "/Download"))
    }
    @Test fun optionalControlsStayOffAndOverridesAreEmpty() {
        val defaults = UserPreferences()
        assertFalse(defaults.showAbRepeat)
        assertFalse(defaults.extendedSearch)
        assertTrue(defaults.localOverrides.isEmpty())
    }
}
