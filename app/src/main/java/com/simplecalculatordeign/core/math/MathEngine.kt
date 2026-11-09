package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class CalcResult(
    val expression: String,
    val value: BigDecimal,
    /** true only when a non-terminating / irrational value had to be rounded. */
    val approximate: Boolean,
    val steps: List<CalcStep>,
)

/** Facade over tokenizer → parser → evaluator. Pure Kotlin, no Android dependencies. */
object MathEngine {

    /** Hard safety limit on the number of integer digits a result may have. */
    const val MAX_DIGITS = 100_000

    private val OPERATOR_CHARS = setOf('+', '-', '×', '÷', '^', '*', '/', '−')

    fun evaluate(expression: String, options: EvalOptions = EvalOptions()): CalcResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) throw CalcException("Enter an expression")
        try {
            val tokens = Tokenizer.tokenize(trimmed, allowVariables = options.variables.isNotEmpty())
            val ast = Parser(tokens).parse()
            val evaluator = Evaluator(options)
            var value = evaluator.evaluate(ast)
            if (evaluator.approximate) {
                // Requested precision applies after the decimal point for numbers ≥ 1,
                // so integer digits are never lost when a fraction had to be rounded.
                val intDigits = maxOf(0, value.precision() - value.scale())
                value = value.round(MathContext(options.precision + intDigits, RoundingMode.HALF_EVEN))
            }
            return CalcResult(trimmed, normalize(value), evaluator.approximate, evaluator.steps.toList())
        } catch (e: CalcException) {
            throw e
        } catch (e: ArithmeticException) {
            throw CalcException(e.message?.let { "Math error: $it" } ?: "Math error")
        } catch (e: NumberFormatException) {
            throw CalcException("Invalid number")
        } catch (e: StackOverflowError) {
            throw CalcException("Expression is too complex")
        } catch (e: OutOfMemoryError) {
            throw CalcException("Result is too large for this device")
        }
    }

    fun tryEvaluate(expression: String, options: EvalOptions = EvalOptions()): CalcResult? =
        try { evaluate(expression, options) } catch (e: CalcException) { null }

    /**
     * Makes a partially typed expression evaluable for the live preview: drops
     * trailing operators / open functions and closes open parentheses.
     */
    fun prepareForPreview(expression: String): String {
        var s = expression.trimEnd()
        while (s.isNotEmpty()) {
            val last = s.last()
            s = when {
                last in OPERATOR_CHARS || last == '(' || last == '√' || last == '.' -> s.dropLast(1).trimEnd()
                else -> break
            }
            // Remove a dangling function name left after stripping its "("
            for (f in listOf("sin", "cos", "tan", "log", "ln")) {
                if (s.endsWith(f)) { s = s.dropLast(f.length); break }
            }
        }
        return closeParentheses(s)
    }

    fun closeParentheses(expression: String): String {
        val open = expression.count { it == '(' } - expression.count { it == ')' }
        return if (open > 0) expression + ")".repeat(open) else expression
    }

    /** True when an expression contains an actual operation (not just a number). */
    fun hasOperation(expression: String): Boolean {
        val body = expression.trim().removePrefix("-").removePrefix("−")
        return body.any { it in OPERATOR_CHARS || it in "%!²√πe(" } ||
            Tokenizer.FUNCTIONS.any { body.contains(it) }
    }

    private fun normalize(v: BigDecimal): BigDecimal =
        if (v.signum() == 0) BigDecimal.ZERO else v.stripTrailingZeros()
}
