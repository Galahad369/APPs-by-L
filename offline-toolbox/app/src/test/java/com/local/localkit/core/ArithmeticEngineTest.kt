package com.local.localkit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArithmeticEngineTest {
    @Test fun precedenceAndParentheses() {
        assertEquals(50.0, ArithmeticEngine.evaluate("2 + 6 × (9 - 1)"), 0.000001)
    }

    @Test fun powersAreRightAssociative() {
        assertEquals(512.0, ArithmeticEngine.evaluate("2^3^2"), 0.000001)
    }

    @Test fun functionsUseDegrees() {
        assertEquals(5.0, ArithmeticEngine.evaluate("sqrt(16) + sin(90)"), 0.000001)
    }

    @Test fun invalidInputIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ArithmeticEngine.evaluate("2 + mystery") }
    }
}

