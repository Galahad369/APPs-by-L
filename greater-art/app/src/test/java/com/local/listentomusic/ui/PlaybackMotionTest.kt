package com.local.listentomusic.ui
import org.junit.Assert.*
import org.junit.Test

class PlaybackMotionTest {
    @Test fun missingAndInvalidWaveformsAreQuiet() {
        assertEquals(0f, waveformEnvelope(null, 0, 10), 0f)
        assertEquals(0f, waveformEnvelope(floatArrayOf(Float.NaN), 0, 10), 0f)
    }
    @Test fun envelopeUsesRealPositionAndClampsAtEnd() {
        assertEquals(.8f, waveformEnvelope(floatArrayOf(.1f,.8f), 100, 100), .0001f)
    }
    @Test fun magnetismOnlyUsesNearbyRealMarkers() {
        assertEquals(1000L, snapPracticePosition(1050, 10000, listOf(1000)))
        assertEquals(1500L, snapPracticePosition(1500, 10000, listOf(1000)))
        assertEquals(1050L, snapPracticePosition(1050, 10000, emptyList()))
    }
}
