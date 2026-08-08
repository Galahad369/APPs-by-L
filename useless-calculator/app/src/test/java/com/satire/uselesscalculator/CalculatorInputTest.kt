package com.satire.uselesscalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorInputTest {
    @Test fun appendsButtonsWithoutCalculating() =
        assertEquals("12+3", appendCalculatorInput("12+", "3"))

    @Test fun clearErasesThePretendExpression() =
        assertEquals("", appendCalculatorInput("123×456", "C"))

    @Test fun subscriptionPriceIsAbsurd() =
        assertEquals("$29.99", MONTHLY_PRICE)

    @Test fun termsArePointlesslyEnormous() =
        assertTrue(termsClauses.size > 300)
}
