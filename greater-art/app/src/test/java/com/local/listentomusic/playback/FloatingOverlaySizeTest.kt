package com.local.listentomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingOverlaySizeTest {
    @Test fun portraitPhoneLeavesRealSpaceAroundThePlayer() {
        val size = floatingOverlaySize(1080, 2130, 2.8125f)
        assertEquals(1037, size.widthPx)
        assertEquals(1917, size.heightPx)
    }

    @Test fun smallAndLandscapeDisplaysNeverOverflow() {
        listOf(240 to 300, 600 to 360, 1200 to 650).forEach { (width, height) ->
            val size = floatingOverlaySize(width, height, 3f)
            assertTrue(size.widthPx in 1..width)
            assertTrue(size.heightPx in 1..height)
        }
    }

    @Test fun tabletUsesUpperBoundsNotMinimums() {
        val size = floatingOverlaySize(3000, 2400, 2f)
        assertEquals(1240, size.widthPx)
        assertEquals(1720, size.heightPx)
        assertEquals(FloatingOverlaySize(1, 1), floatingOverlaySize(0, 0, 0f))
    }

    @Test fun fullscreenFillsOnlyTheAlreadyInsetSafeArea() {
        assertEquals(FloatingOverlaySize(1080, 2130), floatingOverlaySize(1080, 2130, 2.8125f, fullscreen = true))
    }
}
