package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max

/**
 * Arbitrary-precision transcendental functions built only on BigDecimal, so they
 * work on every Android version (BigDecimal.sqrt only exists on API 33+).
 *
 * All functions return values correct to [precision] significant digits; they use
 * extra guard digits internally.
 */
class BigMath(
    val precision: Int,
    private val checkCancelled: () -> Unit = {},
) {
    private val guard = 12
    private val wmc = MathContext(precision + guard, RoundingMode.HALF_EVEN)
    private val outMc = MathContext(precision, RoundingMode.HALF_EVEN)

    private fun epsilon(digits: Int): BigDecimal = BigDecimal.ONE.movePointLeft(digits)

    fun round(x: BigDecimal): BigDecimal = x.round(outMc)

    // ---------------------------------------------------------------- constants

    fun pi(): BigDecimal = piWorking(precision + guard).round(outMc)

    private fun piWorking(digits: Int): BigDecimal = PI_CACHE.getOrPut(digits) {
        // Machin: π = 16·atan(1/5) − 4·atan(1/239)
        val mc = MathContext(digits + 5, RoundingMode.HALF_EVEN)
        val a = atanInverse(5, mc)
        val b = atanInverse(239, mc)
        (a.multiply(BigDecimal(16)) - b.multiply(BigDecimal(4))).round(MathContext(digits, RoundingMode.HALF_EVEN))
    }

    private fun atanInverse(n: Int, mc: MathContext): BigDecimal {
        val nBig = BigDecimal(n)
        val n2 = BigDecimal(n.toLong() * n)
        var term = BigDecimal.ONE.divide(nBig, mc)
        var sum = term
        val eps = epsilon(mc.precision + 2)
        var k = 1
        while (true) {
            checkCancelled()
            term = term.divide(n2, mc)
            val contribution = term.divide(BigDecimal(2 * k + 1), mc)
            if (contribution.abs() < eps) break
            sum = if (k % 2 == 1) sum - contribution else sum + contribution
            k++
        }
        return sum
    }

    fun e(): BigDecimal = exp(BigDecimal.ONE)

    // ---------------------------------------------------------------- roots

    fun sqrt(x: BigDecimal): BigDecimal {
        if (x.signum() < 0) throw CalcException("Square root of a negative number is not a real number")
        if (x.signum() == 0) return BigDecimal.ZERO
        // Initial guess from double, scaled to stay inside double range.
        val magnitude = x.precision() - x.scale() // ≈ number of integer digits
        val half = Math.floorDiv(magnitude, 2)
        val mantissa = x.movePointLeft(2 * half)
        var guess = BigDecimal(Math.sqrt(mantissa.toDouble())).movePointRight(half)
        if (guess.signum() == 0) guess = BigDecimal.ONE.movePointRight(half)
        val two = BigDecimal(2)
        val eps = epsilon(wmc.precision)
        repeat(500) {
            checkCancelled()
            val next = guess.add(x.divide(guess, wmc)).divide(two, wmc)
            val diff = next.subtract(guess).abs()
            guess = next
            if (diff.signum() == 0 || diff.divide(guess, wmc) < eps) return guess.round(outMc)
        }
        return guess.round(outMc)
    }

    // ---------------------------------------------------------------- exp / ln

    fun exp(x: BigDecimal): BigDecimal {
        if (x.signum() == 0) return BigDecimal.ONE
        if (x > MAX_EXP_ARG) throw CalcException("Result is too large (more than ${MathEngine.MAX_DIGITS} digits)")
        if (x < MAX_EXP_ARG.negate()) return BigDecimal.ZERO
        val absD = x.abs().toDouble()
        val k = if (absD < 0.5) 0 else ceil(ln(absD / 0.5) / ln(2.0)).toInt() + 1
        val wp = precision + guard + (k * 0.302).toInt() + (x.precision() - x.scale()).coerceAtLeast(0)
        val mc = MathContext(wp, RoundingMode.HALF_EVEN)
        val r = if (k == 0) x else x.divide(BigDecimal(2).pow(k), mc)
        var sum = BigDecimal.ONE
        var term = BigDecimal.ONE
        val eps = epsilon(wp + 2)
        var i = 1
        while (true) {
            checkCancelled()
            term = term.multiply(r, mc).divide(BigDecimal(i), mc)
            sum = sum.add(term, mc)
            if (term.abs() < eps) break
            i++
        }
        repeat(k) {
            checkCancelled()
            sum = sum.multiply(sum, mc)
        }
        return sum.round(outMc)
    }

    fun ln(x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) throw CalcException("Logarithm is only defined for positive numbers")
        if (x.compareTo(BigDecimal.ONE) == 0) return BigDecimal.ZERO
        // x = m × 10^k with 1 ≤ m < 10
        val k = x.precision() - x.scale() - 1
        val m = x.movePointLeft(k)
        val lnM = lnNewton(m)
        val result = if (k == 0) lnM else lnM.add(ln10().multiply(BigDecimal(k), wmc), wmc)
        return result.round(outMc)
    }

    fun ln10(): BigDecimal = LN10_CACHE.getOrPut(precision + guard) { lnNewton(BigDecimal.TEN) }

    /** Halley iteration on exp, valid for moderate m (1 ≤ m ≤ 10). */
    private fun lnNewton(m: BigDecimal): BigDecimal {
        val inner = BigMath(precision + guard, checkCancelled)
        val mc = MathContext(precision + guard + 4, RoundingMode.HALF_EVEN)
        var y = BigDecimal(ln(m.toDouble()))
        val eps = epsilon(precision + guard)
        val two = BigDecimal(2)
        repeat(100) {
            checkCancelled()
            val ey = inner.exp(y)
            val delta = two.multiply(m.subtract(ey), mc).divide(m.add(ey), mc)
            y = y.add(delta, mc)
            if (delta.abs() < eps) return y
        }
        return y
    }

    fun log10(x: BigDecimal): BigDecimal {
        if (x.signum() <= 0) throw CalcException("Logarithm is only defined for positive numbers")
        val stripped = x.stripTrailingZeros()
        if (stripped.unscaledValue() == java.math.BigInteger.ONE) {
            return BigDecimal(-stripped.scale()) // exact power of ten
        }
        val lnx = BigMath(precision + 4, checkCancelled).ln(x)
        return lnx.divide(BigMath(precision + 4, checkCancelled).ln10(), wmc).round(outMc)
    }

    /** x^y for x > 0 and non-integer y. */
    fun pow(x: BigDecimal, y: BigDecimal): BigDecimal {
        if (x.signum() <= 0) throw CalcException("Fractional power needs a positive base")
        val wide = BigMath(precision + guard, checkCancelled)
        val exponent = y.multiply(wide.ln(x), wmc)
        return exp(exponent)
    }

    // ---------------------------------------------------------------- trig (radians)

    private fun reduceAngle(x: BigDecimal): Pair<BigDecimal, Int> {
        val extra = max(0, x.precision() - x.scale())
        val digits = precision + guard + extra
        val mc = MathContext(digits, RoundingMode.HALF_EVEN)
        val twoPi = piWorking(digits).multiply(BigDecimal(2), mc)
        val turns = x.divide(twoPi, mc).setScale(0, RoundingMode.HALF_EVEN)
        val r = x.subtract(twoPi.multiply(turns, mc), mc)
        return r to digits
    }

    fun sin(x: BigDecimal): BigDecimal {
        val (r, digits) = reduceAngle(x)
        val result = sinSeries(r, MathContext(digits, RoundingMode.HALF_EVEN))
        return snap(result, x)
    }

    fun cos(x: BigDecimal): BigDecimal {
        val (r, digits) = reduceAngle(x)
        val result = cosSeries(r, MathContext(digits, RoundingMode.HALF_EVEN))
        return snap(result, x)
    }

    fun tan(x: BigDecimal): BigDecimal {
        val (r, digits) = reduceAngle(x)
        val mc = MathContext(digits, RoundingMode.HALF_EVEN)
        val c = cosSeries(r, mc)
        if (c.abs() < epsilon(precision)) throw CalcException("tan is undefined at this angle")
        val s = sinSeries(r, mc)
        return snap(s.divide(c, mc), x)
    }

    private fun sinSeries(r: BigDecimal, mc: MathContext): BigDecimal {
        val r2 = r.multiply(r, mc)
        var term = r
        var sum = r
        val eps = epsilon(mc.precision + 2)
        var i = 1
        while (true) {
            checkCancelled()
            term = term.multiply(r2, mc).divide(BigDecimal((2L * i) * (2L * i + 1)), mc).negate()
            sum = sum.add(term, mc)
            if (term.abs() < eps) break
            i++
        }
        return sum
    }

    private fun cosSeries(r: BigDecimal, mc: MathContext): BigDecimal {
        val r2 = r.multiply(r, mc)
        var term = BigDecimal.ONE
        var sum = BigDecimal.ONE
        val eps = epsilon(mc.precision + 2)
        var i = 1
        while (true) {
            checkCancelled()
            term = term.multiply(r2, mc).divide(BigDecimal((2L * i - 1) * (2L * i)), mc).negate()
            sum = sum.add(term, mc)
            if (term.abs() < eps) break
            i++
        }
        return sum
    }

    /** sin(π) with a rounded π gives ~1e-50; show 0 instead of noise. */
    private fun snap(value: BigDecimal, input: BigDecimal): BigDecimal {
        if (value.abs() < epsilon(precision - 2) && input.abs() > BigDecimal("0.001")) return BigDecimal.ZERO
        return value.round(outMc)
    }

    companion object {
        private val PI_CACHE = ConcurrentHashMap<Int, BigDecimal>()
        private val LN10_CACHE = ConcurrentHashMap<Int, BigDecimal>()
        /** e^230000 has about 99,900 digits. */
        val MAX_EXP_ARG = BigDecimal(230_000)
    }
}
