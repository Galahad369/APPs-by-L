package com.local.localkit.core

data class UnitDefinition(val symbol: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double)
data class UnitCategory(val name: String, val units: List<UnitDefinition>)

object UnitConversions {
    private fun linear(symbol: String, factor: Double) = UnitDefinition(symbol, { it * factor }, { it / factor })

    val categories = listOf(
        UnitCategory("Length", listOf(linear("mm", .001), linear("cm", .01), linear("m", 1.0), linear("km", 1000.0), linear("in", .0254), linear("ft", .3048), linear("yd", .9144), linear("mi", 1609.344))),
        UnitCategory("Mass", listOf(linear("mg", .000001), linear("g", .001), linear("kg", 1.0), linear("oz", .028349523125), linear("lb", .45359237))),
        UnitCategory("Temperature", listOf(
            UnitDefinition("°C", { it }, { it }),
            UnitDefinition("°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            UnitDefinition("K", { it - 273.15 }, { it + 273.15 })
        )),
        UnitCategory("Data", listOf(linear("B", 1.0), linear("KB", 1_000.0), linear("MB", 1_000_000.0), linear("GB", 1_000_000_000.0), linear("KiB", 1024.0), linear("MiB", 1048576.0), linear("GiB", 1073741824.0))),
        UnitCategory("Volume", listOf(linear("mL", .001), linear("L", 1.0), linear("tsp", .00492892159375), linear("tbsp", .01478676478125), linear("cup", .2365882365), linear("fl oz", .0295735295625), linear("gal", 3.785411784))),
        UnitCategory("Speed", listOf(linear("m/s", 1.0), linear("km/h", 1.0 / 3.6), linear("mph", .44704), linear("knot", .514444)))
    )

    fun convert(value: Double, from: UnitDefinition, to: UnitDefinition): Double = to.fromBase(from.toBase(value))
}

