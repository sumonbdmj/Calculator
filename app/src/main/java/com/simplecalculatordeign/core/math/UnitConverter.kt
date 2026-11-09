package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class MeasureUnit(val id: String, val name: String, val symbol: String, val toBase: BigDecimal)

enum class UnitCategory(val title: String) {
    LENGTH("Length"), MASS("Weight"), TEMPERATURE("Temperature"), AREA("Area"),
    VOLUME("Volume"), SPEED("Speed"), TIME("Time"), DATA("Data"), ENERGY("Energy"), PRESSURE("Pressure");
}

/** BigDecimal unit conversion. Each unit stores its exact factor to the category's base unit. */
object UnitConverter {

    private val MC = MathContext(34, RoundingMode.HALF_EVEN)

    private fun u(id: String, name: String, symbol: String, factor: String) = MeasureUnit(id, name, symbol, BigDecimal(factor))

    val units: Map<UnitCategory, List<MeasureUnit>> = mapOf(
        UnitCategory.LENGTH to listOf( // base: metre
            u("km", "Kilometre", "km", "1000"), u("m", "Metre", "m", "1"), u("cm", "Centimetre", "cm", "0.01"),
            u("mm", "Millimetre", "mm", "0.001"), u("um", "Micrometre", "µm", "0.000001"),
            u("mi", "Mile", "mi", "1609.344"), u("yd", "Yard", "yd", "0.9144"), u("ft", "Foot", "ft", "0.3048"),
            u("in", "Inch", "in", "0.0254"), u("nmi", "Nautical mile", "nmi", "1852"),
        ),
        UnitCategory.MASS to listOf( // base: kilogram
            u("t", "Tonne", "t", "1000"), u("kg", "Kilogram", "kg", "1"), u("g", "Gram", "g", "0.001"),
            u("mg", "Milligram", "mg", "0.000001"), u("lb", "Pound", "lb", "0.45359237"),
            u("oz", "Ounce", "oz", "0.028349523125"), u("st", "Stone", "st", "6.35029318"),
            u("maund", "Maund (BD)", "md", "37.3242"), u("seer", "Seer (BD)", "seer", "0.933105"),
        ),
        UnitCategory.TEMPERATURE to listOf(
            u("c", "Celsius", "°C", "1"), u("f", "Fahrenheit", "°F", "1"), u("k", "Kelvin", "K", "1"),
        ),
        UnitCategory.AREA to listOf( // base: square metre
            u("km2", "Square kilometre", "km²", "1000000"), u("ha", "Hectare", "ha", "10000"),
            u("m2", "Square metre", "m²", "1"), u("cm2", "Square centimetre", "cm²", "0.0001"),
            u("acre", "Acre", "ac", "4046.8564224"), u("ft2", "Square foot", "ft²", "0.09290304"),
            u("in2", "Square inch", "in²", "0.00064516"), u("decimal", "Decimal (BD)", "dec", "40.468564224"),
            u("katha", "Katha (BD)", "katha", "66.8901888"), u("bigha", "Bigha (BD)", "bigha", "1337.803776"),
        ),
        UnitCategory.VOLUME to listOf( // base: litre
            u("m3", "Cubic metre", "m³", "1000"), u("l", "Litre", "L", "1"), u("ml", "Millilitre", "mL", "0.001"),
            u("gal", "US gallon", "gal", "3.785411784"), u("ukgal", "UK gallon", "gal (UK)", "4.54609"),
            u("qt", "US quart", "qt", "0.946352946"), u("cup", "US cup", "cup", "0.2365882365"),
            u("floz", "US fluid ounce", "fl oz", "0.0295735295625"), u("ft3", "Cubic foot", "ft³", "28.316846592"),
        ),
        UnitCategory.SPEED to listOf( // base: metre per second
            u("mps", "Metre/second", "m/s", "1"), u("kmh", "Kilometre/hour", "km/h", "0.27777777777777777777777777777778"),
            u("mph", "Mile/hour", "mph", "0.44704"), u("kn", "Knot", "kn", "0.51444444444444444444444444444444"),
            u("fps", "Foot/second", "ft/s", "0.3048"),
        ),
        UnitCategory.TIME to listOf( // base: second
            u("ms", "Millisecond", "ms", "0.001"), u("s", "Second", "s", "1"), u("min", "Minute", "min", "60"),
            u("h", "Hour", "h", "3600"), u("d", "Day", "d", "86400"), u("wk", "Week", "wk", "604800"),
            u("yr", "Year (365 d)", "yr", "31536000"),
        ),
        UnitCategory.DATA to listOf( // base: byte
            u("bit", "Bit", "bit", "0.125"), u("B", "Byte", "B", "1"), u("KB", "Kilobyte", "KB", "1000"),
            u("MB", "Megabyte", "MB", "1000000"), u("GB", "Gigabyte", "GB", "1000000000"),
            u("TB", "Terabyte", "TB", "1000000000000"), u("KiB", "Kibibyte", "KiB", "1024"),
            u("MiB", "Mebibyte", "MiB", "1048576"), u("GiB", "Gibibyte", "GiB", "1073741824"),
        ),
        UnitCategory.ENERGY to listOf( // base: joule
            u("J", "Joule", "J", "1"), u("kJ", "Kilojoule", "kJ", "1000"), u("cal", "Calorie", "cal", "4.184"),
            u("kcal", "Kilocalorie", "kcal", "4184"), u("Wh", "Watt-hour", "Wh", "3600"),
            u("kWh", "Kilowatt-hour", "kWh", "3600000"),
        ),
        UnitCategory.PRESSURE to listOf( // base: pascal
            u("Pa", "Pascal", "Pa", "1"), u("kPa", "Kilopascal", "kPa", "1000"), u("bar", "Bar", "bar", "100000"),
            u("atm", "Atmosphere", "atm", "101325"), u("psi", "PSI", "psi", "6894.757293168"),
            u("mmHg", "mm of mercury", "mmHg", "133.322387415"),
        ),
    )

    fun convert(value: BigDecimal, from: MeasureUnit, to: MeasureUnit, category: UnitCategory): BigDecimal {
        val result = if (category == UnitCategory.TEMPERATURE) {
            fromCelsius(toCelsius(value, from.id), to.id)
        } else {
            val base = value.multiply(from.toBase)
            try { base.divide(to.toBase) } catch (e: ArithmeticException) { base.divide(to.toBase, MC) }
        }
        return if (result.signum() == 0) BigDecimal.ZERO else result.round(MC).stripTrailingZeros()
    }

    private val NINE = BigDecimal(9)
    private val FIVE = BigDecimal(5)
    private val THIRTY_TWO = BigDecimal(32)
    private val KELVIN = BigDecimal("273.15")

    private fun toCelsius(v: BigDecimal, id: String): BigDecimal = when (id) {
        "f" -> v.subtract(THIRTY_TWO).multiply(FIVE).divide(NINE, MC)
        "k" -> v.subtract(KELVIN)
        else -> v
    }

    private fun fromCelsius(c: BigDecimal, id: String): BigDecimal = when (id) {
        "f" -> c.multiply(NINE).divide(FIVE, MC).add(THIRTY_TWO)
        "k" -> c.add(KELVIN)
        else -> c
    }
}
