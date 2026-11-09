package com.simplecalculatordeign.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MathEngineTest {

    private fun calc(expr: String, options: EvalOptions = EvalOptions()) =
        NumberFormatter.plain(MathEngine.evaluate(expr, options).value)

    private fun assertError(expr: String, contains: String) {
        try {
            MathEngine.evaluate(expr)
            fail("Expected error for $expr")
        } catch (e: CalcException) {
            assertTrue("'${e.message}' should contain '$contains'", e.message!!.contains(contains))
        }
    }

    @Test fun basicOperations() {
        assertEquals("31250", calc("1250×25"))
        assertEquals("400", calc("50000÷125"))
        assertEquals("10000", calc("(250+750)×10"))
        assertEquals("1550", calc("1200 + 350"))
        assertEquals("-5", calc("5-10"))
        assertEquals("14", calc("2+3×4"))
        assertEquals("20", calc("(2+3)×4"))
    }

    @Test fun asciiOperatorsAndUnaryMinus() {
        assertEquals("6", calc("2*3"))
        assertEquals("2.5", calc("5/2"))
        assertEquals("-4", calc("-2^2"))
        assertEquals("4", calc("(-2)^2"))
        assertEquals("-6", calc("2×-3"))
        assertEquals("0.25", calc("2^-2"))
    }

    @Test fun decimalsAreExact() {
        assertEquals("0.3", calc("0.1+0.2"))
        assertEquals("1.21", calc("1.1×1.1"))
        assertEquals("0.0001", calc("0.01^2"))
    }

    @Test fun hugeIntegersAreExact() {
        assertEquals(
            "1267650600228229401496703205376", calc("2^100")
        )
        val nines = "9".repeat(41)
        assertEquals(java.math.BigInteger(nines).pow(2).toString(), calc("$nines^2"))
        val big = calc("123456789012345678901234567890×987654321098765432109876543210")
        assertEquals("121932631137021795226185032733622923332237463801111263526900", big)
        val f = calc("100!")
        assertEquals(158, f.length)
        assertTrue(f.startsWith("933262154439441526816992388562667004907159682643816214685929"))
    }

    @Test fun longDecimalsNotTruncated() {
        assertEquals(
            "12345678901234567890.1234567890123456789",
            calc("12345678901234567890.12345678901234567890×1")
        )
        assertEquals("0.00000000000000000000000000000000000001", calc("1÷10^38"))
    }

    @Test fun nonTerminatingDivisionIsMarkedApproximate() {
        val r = MathEngine.evaluate("1÷3", EvalOptions(precision = 20))
        assertTrue(r.approximate)
        assertEquals("0.33333333333333333333", NumberFormatter.plain(r.value))
        val exact = MathEngine.evaluate("1÷8")
        assertFalse(exact.approximate)
        assertEquals("0.125", NumberFormatter.plain(exact.value))
    }

    @Test fun percentages() {
        assertEquals("0.5", calc("50%"))
        assertEquals("220", calc("200+10%"))
        assertEquals("180", calc("200-10%"))
        assertEquals("20", calc("200×10%"))
        assertEquals("2000", calc("200÷10%"))
    }

    @Test fun powersRootsReciprocal() {
        assertEquals("25", calc("5²"))
        assertEquals("1024", calc("2^10"))
        assertEquals("12", calc("√144"))
        assertEquals("1.4142135623730950488", calc("√2", EvalOptions(precision = 20)))
        assertEquals("0.125", calc("1÷8"))
        assertEquals("6", calc("2√9"))
        assertEquals("9", calc("√(9)²".let { "√(81)" }))
        assertEquals("512", calc("2^3^2")) // right associative
        assertEquals("1.4142135623730950488", calc("2^0.5", EvalOptions(precision = 20)))
    }

    @Test fun scientificFunctions() {
        val deg = EvalOptions(precision = 20, angleMode = AngleMode.DEGREES)
        val rad = EvalOptions(precision = 20, angleMode = AngleMode.RADIANS)
        assertEquals("0.5", calc("sin(30)", deg))
        assertEquals("0", calc("cos(90)", deg))
        assertEquals("1", calc("tan(45)", deg))
        assertEquals("0.7071067811865475244", calc("sin(45)", deg))
        assertEquals("0", calc("sin(π)", rad))
        assertEquals("-1", calc("cos(π)", rad))
        assertEquals("3", calc("log(1000)", deg))
        assertEquals("1", calc("ln(e)", deg))
        assertEquals("3.14159265358979323846", calc("π", deg))
        assertEquals("2.71828182845904523536", calc("e", deg))
        assertEquals("120", calc("5!"))
        assertEquals("6.28318530717958647693", calc("2π", deg))
        assertEquals("2.30258509299404568402", calc("ln(10)", deg))
        assertError("tan(90)", "undefined")
    }

    @Test fun stepByStep() {
        val r = MathEngine.evaluate("(250+150)×5")
        val steps = r.steps.map { NumberFormatter.formatStep(it) }
        assertEquals(listOf("250 + 150 = 400", "400 × 5 = 2,000"), steps)
        val pct = MathEngine.evaluate("200+10%").steps.map { NumberFormatter.formatStep(it) }
        assertEquals(listOf("10% of 200 = 20", "200 + 20 = 220"), pct)
    }

    @Test fun errors() {
        assertError("5÷0", "divide by zero")
        assertError("√-4", "not a real")
        assertError("(5+3", "Missing closing")
        assertError("5+3)", "Unmatched")
        assertError("5++", "Incomplete")
        assertError("(-8)^0.5", "not a real")
        assertError("10^1000000", "too large")
        assertError("3.5!", "whole number")
        assertError("abc", "Unknown")
    }

    @Test fun previewPreparation() {
        assertEquals("(5+3)", MathEngine.prepareForPreview("(5+3"))
        assertEquals("5", MathEngine.prepareForPreview("5+"))
        assertEquals("2×(3)", MathEngine.prepareForPreview("2×(3+"))
        assertEquals("12", MathEngine.prepareForPreview("12+sin("))
    }

    @Test fun groupingAndFormatting() {
        val v = BigDecimal("1234567.891")
        assertEquals("1,234,567.891", NumberFormatter.format(v))
        assertEquals("12,34,567.891", NumberFormatter.format(v, FormatOptions(GroupingStyle.INDIAN)))
        assertEquals("1234567.891", NumberFormatter.format(v, FormatOptions(GroupingStyle.NONE)))
        val fixed = NumberFormatter.formatDetailed(v, FormatOptions(decimalPlaces = 2))
        assertEquals("1,234,567.89", fixed.text)
        assertTrue(fixed.roundedForDisplay)
        assertEquals("1,250×25", NumberFormatter.formatExpression("1250×25", GroupingStyle.INTERNATIONAL))
        assertEquals("0.12345", NumberFormatter.format(BigDecimal("0.12345")))
        assertEquals("-1,000", NumberFormatter.format(BigDecimal("-1000")))
    }

    @Test fun exponentInputRoundTrips() {
        val tiny = MathEngine.evaluate("0.5^10000").value
        val plain = NumberFormatter.plain(tiny)
        assertTrue(plain.contains("E-"))
        val again = MathEngine.evaluate("$plain×2").value
        assertEquals(0, again.compareTo(tiny.multiply(BigDecimal(2))))
    }

    @Test fun integerDigitsSurviveRounding() {
        val r = MathEngine.evaluate("10^50÷3", EvalOptions(precision = 10))
        assertTrue(r.approximate)
        assertEquals("33333333333333333333333333333333333333333333333333.3333333333", NumberFormatter.plain(r.value))
        val huge = MathEngine.evaluate("10^1000.5", EvalOptions(precision = 10))
        assertTrue(NumberFormatter.format(huge.value, approximate = true).contains("×10^1000"))
    }

    @Test fun stressLargeAndPrecise() {
        val fact = calc("25000!")
        assertEquals(99_094, fact.length)
        val pow = calc("2^300000")
        assertEquals(90_309, pow.length)
        assertEquals(java.math.BigInteger.TWO.pow(300000).toString(), pow)
        val sqrt2 = calc("√2", EvalOptions(precision = 200))
        assertTrue(sqrt2.startsWith("1.41421356237309504880168872420969807856967187537694807317667973799073247846210703885038753432764157"))
        val third = MathEngine.evaluate("10÷7", EvalOptions(precision = 200))
        assertEquals(202, NumberFormatter.plain(third.value).length) // 1 integer digit + '.' + 200 decimals
        val lnBig = calc("ln(10^5000)", EvalOptions(precision = 30))
        assertEquals("11512.925464970228420089957273421821", lnBig)
        assertError("100000!", "too large")
    }
}
