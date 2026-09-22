package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class PlayerWindowVisibilityTest {
    @Test fun detachedOnlyAppearsAfterBothFullPresentationsLeave() {
        assertTrue(showDetachedPlayer(false, false))
        assertFalse(showDetachedPlayer(true, false))
        assertFalse(showDetachedPlayer(false, true))
        assertFalse(showDetachedPlayer(true, true))
    }
    @Test fun switchingHostsNeverBrieflyShowsDetached() {
        PlayerWindowVisibility.library(true)
        PlayerWindowVisibility.expanded(true)
        PlayerWindowVisibility.library(false)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.expanded(false)
        assertTrue(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.library(true)
        assertFalse(PlayerWindowVisibility.detachedVisible.value)
        PlayerWindowVisibility.library(false)
    }
}
