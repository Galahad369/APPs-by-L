package com.local.localkit.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionsTest {
    @Test fun milesToKilometres() {
        val length = UnitConversions.categories.first { it.name == "Length" }
        assertEquals(1.609344, UnitConversions.convert(1.0, length.units.first { it.symbol == "mi" }, length.units.first { it.symbol == "km" }), 0.000001)
    }

    @Test fun fahrenheitToCelsius() {
        val temp = UnitConversions.categories.first { it.name == "Temperature" }
        assertEquals(100.0, UnitConversions.convert(212.0, temp.units.first { it.symbol == "°F" }, temp.units.first { it.symbol == "°C" }), 0.000001)
    }
}

