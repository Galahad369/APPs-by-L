package com.local.listentomusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformRepositoryTest {
    @Test
    fun silenceUsesOnlyTheVisualFloor() {
        val result = normalizeWaveformPeaks(FloatArray(96))

        assertTrue(result.all { it == 0.015f })
    }

    @Test
    fun tinyNoiseDoesNotBecomeAFullHeightWaveform() {
        val input = FloatArray(96) { 0.001f }
        input[48] = 0.8f

        val result = normalizeWaveformPeaks(input)

        assertTrue(result.take(40).all { it < 0.2f })
        assertEquals(1f, result[48], 0.0001f)
    }

    @Test
    fun typicalSignalKeepsVisibleDynamicRange() {
        val input = FloatArray(96) { index -> 0.02f + (index % 12) * 0.04f }

        val result = normalizeWaveformPeaks(input)

        assertTrue(result.max() > 0.9f)
        assertTrue(result.min() < 0.15f)
    }
}
