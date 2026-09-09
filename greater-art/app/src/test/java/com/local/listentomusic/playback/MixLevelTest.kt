package com.local.listentomusic.playback

import org.junit.Assert.*
import org.junit.Test

class MixLevelTest {
    @Test fun tenFullScaleVoicesHaveUnityCombinedGain() {
        assertEquals(1f, mixLevel(1f, 9) * 10, 0.00001f)
    }
    @Test fun invalidInputsCannotAmplifyOrProduceNan() {
        assertEquals(0f, mixLevel(Float.NaN, 4), 0f)
        assertEquals(0f, mixLevel(-1f, 4), 0f)
        assertEquals(0.1f, mixLevel(20f, 40), 0.00001f)
        assertEquals(1f, mixLevel(1f, -1), 0f)
    }
    @Test fun removingAllLayersRestoresMainLevel() {
        assertEquals(1f, mixLevel(1f, 0), 0f)
        assertEquals(0f, mixLevel(0f, 9), 0f)
    }
}
