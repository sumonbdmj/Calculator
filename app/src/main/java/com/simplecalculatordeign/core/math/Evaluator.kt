package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.log10

enum class AngleMode { DEGREES, RADIANS }

data class EvalOptions(
    /** Significant digits used only when a result cannot be represented exactly (e.g. 1 ÷ 3, √2, sin). */
    val precision: Int = 50,
    val angleMode: AngleMode = AngleMode.DEGREES,
    val variables: Map<String, BigDecimal> = emptyMap(),
    val recordSteps: Boolean = true,
    val checkCancelled: () -> Unit = {},
)

/** One part of a human-readable calculation step: literal text or a number to be formatted. */
sealed interface StepPart {
    data class Text(val text: String) : StepPart
    data class Value(val value: BigDecimal) : StepPart
}

/** A single calculation step, e.g. [250] [" + "] [150] = 400. */
data class CalcStep(val parts: List<StepPart>, val result: BigDecimal)

/**
 * Evaluates an AST with exact BigDecimal / BigInteger arithmetic wherever possible.
 * Addition, subtraction, multiplication, integer powers, factorials and terminating
 * divisions are exact. Only non-terminating or irrational results are rounded to
 * [EvalOptions.precision] significant digits, and [approximate] is then set so the
 * UI can say so (results are never silently truncated).
 */
class Evaluator(private val options: EvalOptions) {

    val steps = mutableListOf<CalcStep>()
    var approximate = false
        private set

    /** Working precision: guard digits are removed by [MathEngine] when rounding the final result. */
    private val workPrecision = options.precision + GUARD_DIGITS
    private val math = BigMath(workPrecision, options.checkCancelled)
    private val hundred = BigDecimal(100)

    fun evaluate(node: Node): BigDecimal = eval(node)

    private fun step(result: BigDecimal, vararg parts: Any) {
        if (!options.recordSteps) return
        steps += CalcStep(
            parts.map { if (it is BigDecimal) StepPart.Value(it) else StepPart.Text(it.toString()) },
            result,
        )
    }

    private fun eval(node: Node): BigDecimal {
        options.checkCancelled()
        return when (node) {
            is Node.Number -> node.value
            is Node.Constant -> constant(node.name)
            is Node.Variable -> options.variables[node.name]
                ?: throw CalcException("Unknown variable '${node.name}'")
            is Node.Negate -> eval(node.operand).negate()
            is Node.Percent -> {
                val v = eval(node.operand)
                val r = v.divide(hundred)
                step(r, v, "%")
                r
            }
            is Node.Factorial -> factorial(eval(node.operand))
            is Node.Function -> function(node.name, eval(node.arg))
            is Node.Binary -> binary(node)
        }
    }

    private fun constant(name: String): BigDecimal {
        approximate = true
        return when (name) {
            "π" -> math.pi()
            "e" -> math.e()
            else -> throw CalcException("Unknown constant '$name'")
        }
    }

    private fun binary(node: Node.Binary): BigDecimal {
        // Calculator-style percentages: 200 + 10% = 220, 200 − 10% = 180
        if ((node.op == '+' || node.op == '-') && node.right is Node.Percent) {
            val base = eval(node.left)
            val pct = eval(node.right.operand)
            val amount = checked(base.multiply(pct).divide(hundred))
            step(amount, pct, "% of ", base)
            val result = checked(if (node.op == '+') base.add(amount) else base.subtract(amount))
            step(result, base, if (node.op == '+') " + " else " − ", amount)
            return result
        }
        val l = eval(node.left)
        val r = eval(node.right)
        val result = when (node.op) {
            '+' -> l.add(r)
            '-' -> l.subtract(r)
            '×' -> l.multiply(r)
            '÷' -> divide(l, r)
            '^' -> power(l, r)
            else -> throw CalcException("Unknown operator '${node.op}'")
        }
        val checkedResult = checked(result)
        val symbol = when (node.op) {
            '-' -> " − "
            '^' -> " ^ "
            else -> " ${node.op} "
        }
        step(checkedResult, l, symbol, r)
        return checkedResult
    }

    fun divide(l: BigDecimal, r: BigDecimal): BigDecimal {
        if (r.signum() == 0) throw CalcException("Cannot divide by zero")
        return try {
            l.divide(r) // exact when the decimal expansion terminates
        } catch (e: ArithmeticException) {
            approximate = true
            // Keep every integer digit of the quotient plus the requested precision.
            val intDigits = (l.precision() - l.scale()) - (r.precision() - r.scale()) + 1
            l.divide(r, MathContext(workPrecision + maxOf(0, intDigits), RoundingMode.HALF_EVEN))
        }
    }

    private fun power(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val e = exponent.stripTrailingZeros()
        val isInteger = e.signum() == 0 || e.scale() <= 0
        if (isInteger) {
            val n = e.toBigInteger()
            if (base.signum() == 0) {
                if (n.signum() < 0) throw CalcException("Cannot divide by zero")
                return if (n.signum() == 0) BigDecimal.ONE else BigDecimal.ZERO
            }
            if (base.abs().compareTo(BigDecimal.ONE) == 0) {
                val negativeResult = base.signum() < 0 && n.testBit(0)
                return if (negativeResult) BigDecimal.ONE.negate() else BigDecimal.ONE
            }
            if (n.bitLength() > 31) throw CalcException("Exponent is too large")
            val count = n.toInt()
            val absCount = abs(count)
            val log10Base = approxLog10(base.abs())
            if (count > 0 && log10Base * absCount > MathEngine.MAX_DIGITS) {
                throw CalcException("Result is too large (more than ${MathEngine.MAX_DIGITS} digits)")
            }
            // Digits of the exact result: integer digits + decimal places (scale × n).
            val strippedBase = base.stripTrailingZeros()
            val exactDigits = (maxOf(0.0, log10Base) * absCount).toLong() +
                maxOf(0, strippedBase.scale()).toLong() * absCount + 1
            val raised = if (exactDigits <= MathEngine.MAX_DIGITS) {
                base.pow(absCount)
            } else {
                approximate = true
                base.pow(absCount, MathContext(workPrecision, RoundingMode.HALF_EVEN))
            }
            return if (count >= 0) raised else divide(BigDecimal.ONE, raised)
        }
        if (base.signum() < 0) throw CalcException("A negative number to a fractional power is not a real number")
        if (base.signum() == 0) {
            if (e.signum() > 0) return BigDecimal.ZERO
            throw CalcException("Cannot divide by zero")
        }
        approximate = true
        return math.pow(base, e)
    }

    private fun factorial(v: BigDecimal): BigDecimal {
        val stripped = v.stripTrailingZeros()
        if (v.signum() < 0 || (stripped.signum() != 0 && stripped.scale() > 0)) {
            throw CalcException("Factorial needs a whole number ≥ 0")
        }
        val n = v.toBigInteger()
        if (n > BigInteger.valueOf(MAX_FACTORIAL.toLong())) {
            throw CalcException("Factorial input is too large (max $MAX_FACTORIAL)")
        }
        var result = BigInteger.ONE
        for (i in 2..n.toInt()) {
            if (i % 256 == 0) options.checkCancelled()
            result = result.multiply(BigInteger.valueOf(i.toLong()))
        }
        val r = BigDecimal(result)
        step(r, v, "!")
        return r
    }

    private fun function(name: String, arg: BigDecimal): BigDecimal {
        val result: BigDecimal
        when (name) {
            "sqrt" -> {
                result = exactSqrtOrApprox(arg)
                step(result, "√(", arg, ")")
            }
            "sin", "cos", "tan" -> {
                result = trig(name, arg)
                val unit = if (options.angleMode == AngleMode.DEGREES) "°" else " rad"
                step(result, "$name(", arg, "$unit)")
            }
            "log" -> {
                result = math.log10(arg)
                if (arg.stripTrailingZeros().unscaledValue() != BigInteger.ONE) approximate = true
                step(result, "log(", arg, ")")
            }
            "ln" -> {
                result = math.ln(arg)
                if (arg.compareTo(BigDecimal.ONE) != 0) approximate = true
                step(result, "ln(", arg, ")")
            }
            else -> throw CalcException("Unknown function '$name'")
        }
        return result
    }

    private fun exactSqrtOrApprox(x: BigDecimal): BigDecimal {
        val r = math.sqrt(x)
        val stripped = r.stripTrailingZeros()
        // Exact if squaring the rounded root gives back x exactly.
        if (stripped.precision() < workPrecision - 2 && stripped.multiply(stripped).compareTo(x) == 0) return stripped
        approximate = true
        return r
    }

    private fun trig(name: String, arg: BigDecimal): BigDecimal {
        if (options.angleMode == AngleMode.DEGREES) {
            val deg = arg.remainder(BigDecimal(360)).let { if (it.signum() < 0) it.add(BigDecimal(360)) else it }
            exactDegree(name, deg)?.let { return it }
            approximate = true
            val wide = MathContext(workPrecision + 8, RoundingMode.HALF_EVEN)
            val precisePi = BigMath(workPrecision + 8, options.checkCancelled).pi()
            val radians = arg.remainder(BigDecimal(360)).multiply(precisePi, wide).divide(BigDecimal(180), wide)
            return applyTrig(name, radians)
        }
        approximate = true
        return applyTrig(name, arg)
    }

    private fun applyTrig(name: String, radians: BigDecimal): BigDecimal = when (name) {
        "sin" -> math.sin(radians)
        "cos" -> math.cos(radians)
        else -> math.tan(radians)
    }

    /** Rational values of sin/cos/tan at special angles, kept exact. */
    private fun exactDegree(name: String, deg: BigDecimal): BigDecimal? {
        val d = deg.stripTrailingZeros()
        if (d.signum() != 0 && d.scale() > 0) return null
        val angle = d.toInt()
        val half = BigDecimal("0.5")
        val one = BigDecimal.ONE
        val zero = BigDecimal.ZERO
        return when (name) {
            "sin" -> when (angle) {
                0, 180 -> zero; 30, 150 -> half; 90 -> one
                210, 330 -> half.negate(); 270 -> one.negate(); else -> null
            }
            "cos" -> when (angle) {
                0 -> one; 60, 300 -> half; 90, 270 -> zero
                120, 240 -> half.negate(); 180 -> one.negate(); else -> null
            }
            else -> when (angle) {
                0, 180 -> zero; 45, 225 -> one; 135, 315 -> one.negate()
                90, 270 -> throw CalcException("tan is undefined at $angle°")
                else -> null
            }
        }
    }

    /** Guards against results that would freeze the UI or exhaust memory. */
    private fun checked(v: BigDecimal): BigDecimal {
        val integerDigits = v.precision() - v.scale()
        if (integerDigits > MathEngine.MAX_DIGITS) {
            throw CalcException("Result is too large (more than ${MathEngine.MAX_DIGITS} digits)")
        }
        if (v.scale() > MathEngine.MAX_DIGITS) {
            approximate = true
            return v.round(MathContext(workPrecision, RoundingMode.HALF_EVEN))
        }
        return v
    }

    private fun approxLog10(v: BigDecimal): Double {
        val digits = v.precision() - v.scale()
        val leading = v.movePointLeft(digits).toDouble()
        return digits + log10(leading)
    }

    companion object {
        const val MAX_FACTORIAL = 25_000
        const val GUARD_DIGITS = 8
    }
}
