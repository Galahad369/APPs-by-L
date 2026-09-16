package com.local.listentomusic.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundSyncTest {
    @Test
    fun videoBackgroundWaitsForPrimaryVideoFrame() {
        assertFalse(shouldAttachVideoBackground(visible = true, primaryIsVideo = true, primaryFrameReady = false))
        assertTrue(shouldAttachVideoBackground(visible = true, primaryIsVideo = true, primaryFrameReady = true))
    }

    @Test
    fun audioPlaybackDoesNotBlockIndependentVideoBackground() {
        assertTrue(shouldAttachVideoBackground(visible = true, primaryIsVideo = false, primaryFrameReady = false))
    }

    @Test
    fun hiddenBackgroundNeverAllocatesVideoOutput() {
        assertFalse(shouldAttachVideoBackground(visible = false, primaryIsVideo = false, primaryFrameReady = true))
        assertFalse(shouldAttachVideoBackground(visible = false, primaryIsVideo = true, primaryFrameReady = true))
    }

    @Test
    fun currentVideoBackgroundMirrorsPrimaryPauseState() {
        assertTrue(shouldMirrorPrimaryPlayback(lifecycleActive = true, primaryIsPlaying = true))
        assertFalse(shouldMirrorPrimaryPlayback(lifecycleActive = true, primaryIsPlaying = false))
        assertFalse(shouldMirrorPrimaryPlayback(lifecycleActive = false, primaryIsPlaying = true))
    }


    @Test
    fun backgroundDecoderYieldsHardwarePriorityToPrimaryVideo() {
        assertTrue(shouldPreferSoftwareBackgroundDecoder(primaryIsVideo = true))
        assertFalse(shouldPreferSoftwareBackgroundDecoder(primaryIsVideo = false))
    }
}
