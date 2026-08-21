package com.local.localkit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SecretGeneratorTest {
    @Test fun requestedLengthIsHonored() {
        assertEquals(32, SecretGenerator.password(32, true).length)
    }

    @Test fun sequentialResultsDiffer() {
        assertNotEquals(SecretGenerator.password(32, true), SecretGenerator.password(32, true))
    }
}

