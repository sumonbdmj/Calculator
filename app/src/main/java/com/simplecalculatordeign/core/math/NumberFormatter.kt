package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.RoundingMode

enum class GroupingStyle { INTERNATIONAL, INDIAN, NONE }

data class FormatOptions(
    val grouping: GroupingStyle = GroupingStyle.INTERNATIONAL,
    /** -1 = automatic (show every digit). Otherwise a fixed number of decimal places. */
    val decimalPlaces: Int = -1,
)

data class FormattedNumber(
    val text: String,
    /** true if the displayed text differs from the full value (fixed decimals setting). */
    val roundedForDisplay: Boolean,
)

object NumberFormatter {

    /** Beyond this many decimal places we switch to mantissa × 10^n form (still all digits). */
    private const val MAX_PLAIN_SCALE = 2_000

    fun format(value: BigDecimal, options: FormatOptions = FormatOptions(), approximate: Boolean = false): String =
        formatDetailed(value, options, approximate).text

    /**
     * @param approximate when true and the value's known digits end before the decimal
     * point (e.g. 10^1000.5), mantissa form is used so unknown digits are not shown as zeros.
     */
    fun formatDetailed(
        value: BigDecimal,
        options: FormatOptions = FormatOptions(),
        approximate: Boolean = false,
    ): FormattedNumber {
        if (approximate && value.signum() != 0 && value.stripTrailingZeros().scale() < -3) {
            return FormattedNumber(scientific(value.stripTrailingZeros(), "×10^"), false)
        }
        if (options.decimalPlaces >= 0) {
            val scaled = value.setScale(options.decimalPlaces, RoundingMode.HALF_UP)
            val rounded = scaled.compareTo(value) != 0
            if (scaled.precision() - scaled.scale() <= MathEngine.MAX_DIGITS + 1) {
                return FormattedNumber(groupPlain(scaled.toPlainString(), options.grouping), rounded)
            }
        }
        val stripped = if (value.signum() == 0) BigDecimal.ZERO else value.stripTrailingZeros()
        if (stripped.scale() > MAX_PLAIN_SCALE) {
            return FormattedNumber(scientific(stripped, "×10^"), false)
        }
        return FormattedNumber(groupPlain(stripped.toPlainString(), options.grouping), false)
    }

    /** Full-precision, machine-readable form used for copy, history and re-entry. */
    fun plain(value: BigDecimal): String {
        val stripped = if (value.signum() == 0) BigDecimal.ZERO else value.stripTrailingZeros()
        return if (stripped.scale() > MAX_PLAIN_SCALE) scientific(stripped, "E") else stripped.toPlainString()
    }

    private fun scientific(v: BigDecimal, separator: String): String {
        val digits = v.unscaledValue().abs().toString()
        val exponent = digits.length - 1 - v.scale()
        val mantissa = if (digits.length > 1) digits[0] + "." + digits.substring(1) else digits
        val sign = if (v.signum() < 0) "-" else ""
        return "$sign$mantissa$separator$exponent"
    }

    fun groupPlain(plain: String, style: GroupingStyle): String {
        if (style == GroupingStyle.NONE) return plain
        val negative = plain.startsWith("-")
        val body = if (negative) plain.substring(1) else plain
        val dot = body.indexOf('.')
        val intPart = if (dot >= 0) body.substring(0, dot) else body
        val frac = if (dot >= 0) body.substring(dot) else ""
        val grouped = groupInteger(intPart, style)
        return (if (negative) "-" else "") + grouped + frac
    }

    fun groupInteger(digits: String, style: GroupingStyle): String {
        if (style == GroupingStyle.NONE || digits.length <= 3) return digits
        val sb = StringBuilder(digits.length + digits.length / 2)
        when (style) {
            GroupingStyle.INTERNATIONAL -> {
                val first = digits.length % 3
                if (first > 0) sb.append(digits, 0, first)
                var i = first
                while (i < digits.length) {
                    if (sb.isNotEmpty()) sb.append(',')
                    sb.append(digits, i, i + 3)
                    i += 3
                }
            }
            GroupingStyle.INDIAN -> {
                val head = digits.substring(0, digits.length - 3)
                val tail = digits.substring(digits.length - 3)
                val first = head.length % 2
                if (first > 0) sb.append(head, 0, first)
                var i = first
                while (i < head.length) {
                    if (sb.isNotEmpty()) sb.append(',')
                    sb.append(head, i, i + 2)
                    i += 2
                }
                sb.append(',').append(tail)
            }
            GroupingStyle.NONE -> sb.append(digits)
        }
        return sb.toString()
    }

    /**
     * Pretty display form of an expression the user is typing: groups digits of
     * every number and uses typographic operators.
     */
    fun formatExpression(expression: String, style: GroupingStyle): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            if (c.isDigit()) {
                val start = i
                while (i < expression.length && expression[i].isDigit()) i++
                val digits = expression.substring(start, i)
                val afterDot = start > 0 && expression[start - 1] == '.'
                val inExponent = start > 0 && (expression[start - 1] == 'E' ||
                    (start > 1 && expression[start - 2] == 'E'))
                sb.append(if (afterDot || inExponent) digits else groupInteger(digits, style))
                continue
            }
            sb.append(
                when (c) {
                    '-' -> '−'
                    '*' -> '×'
                    '/' -> '÷'
                    else -> c
                }
            )
            i++
        }
        return sb.toString()
    }

    /** Renders a calculation step such as "250 + 150 = 400". */
    fun formatStep(step: CalcStep, options: FormatOptions = FormatOptions()): String {
        val left = step.parts.joinToString("") { part ->
            when (part) {
                is StepPart.Text -> part.text
                is StepPart.Value -> {
                    val f = format(part.value, options)
                    if (part.value.signum() < 0) "($f)" else f
                }
            }
        }
        return "$left = ${format(step.result, options)}"
    }
}
