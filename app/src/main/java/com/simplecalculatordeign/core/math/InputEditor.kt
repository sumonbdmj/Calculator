package com.simplecalculatordeign.core.math

/**
 * Pure functions that edit the calculator's expression string the way a premium
 * calculator keypad behaves (operator replacement, smart decimal point, sign
 * toggling, reciprocal, function-aware backspace...).
 *
 * Internal symbols: digits . + - × ÷ ^ ( ) % ! ² √ π e and "sin(" "cos(" "tan(" "log(" "ln(".
 */
object InputEditor {

    private val BINARY_OPERATORS = setOf('+', '-', '×', '÷', '^')
    private val POSTFIX = setOf('%', '!', '²')
    private val FUNCTION_PREFIXES = listOf("sin(", "cos(", "tan(", "log(", "ln(")
    const val MAX_LENGTH = 5_000

    /** True if the expression currently ends with something that can be operated on. */
    fun endsWithOperand(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val last = expr.last()
        return last.isDigit() || last == ')' || last in POSTFIX || last == 'π' || last == 'e' || last == '.'
    }

    private fun needsImplicitMultiplyBeforeNumber(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val last = expr.last()
        return last == ')' || last in POSTFIX || last == 'π' || last == 'e'
    }

    private fun limit(expr: String, addition: String): String =
        if (expr.length + addition.length > MAX_LENGTH) expr else expr + addition

    fun appendDigit(expr: String, digit: Char): String {
        require(digit.isDigit())
        // Avoid leading zeros like "007": replace a lone "0" in the current number.
        val number = currentNumber(expr)
        if (number == "0") return expr.dropLast(1) + digit
        val prefix = if (needsImplicitMultiplyBeforeNumber(expr)) "×" else ""
        return limit(expr, prefix + digit)
    }

    /** Inserts a full number (memory recall, history value...). */
    fun appendNumber(expr: String, number: String): String {
        if (number.isEmpty()) return expr
        val negative = number.startsWith("-")
        val body = if (negative) "(-${number.substring(1)})" else number
        val prefix = when {
            expr.isEmpty() -> ""
            expr.last().isDigit() || expr.last() == '.' || needsImplicitMultiplyBeforeNumber(expr) -> "×"
            else -> ""
        }
        return limit(expr, prefix + body)
    }

    fun appendDecimal(expr: String): String {
        val number = currentNumber(expr)
        if (number.contains('.')) return expr
        if (number.isEmpty()) {
            val prefix = if (needsImplicitMultiplyBeforeNumber(expr)) "×" else ""
            return limit(expr, prefix + "0.")
        }
        return limit(expr, ".")
    }

    fun appendOperator(expr: String, op: Char): String {
        require(op in BINARY_OPERATORS)
        if (expr.isEmpty()) return if (op == '-') "-" else expr
        val last = expr.last()
        if (last in BINARY_OPERATORS) {
            if (op == '-' && last in setOf('×', '÷', '^')) return expr + op // negative operand
            if (last == '-' && expr.length >= 2) {
                val beforeMinus = expr[expr.length - 2]
                if (beforeMinus in setOf('×', '÷', '^')) {
                    return expr.dropLast(2) + op
                }
                if (beforeMinus == '(') return expr // "(-" stays until a number is typed
            }
            if (expr.length == 1) return expr // just "-"
            return expr.dropLast(1) + op
        }
        if (last == '(' || last == '√' || expr.endsWithFunction()) {
            return if (op == '-') expr + op else expr
        }
        return limit(expr, op.toString())
    }

    fun appendPostfix(expr: String, symbol: Char): String {
        require(symbol in POSTFIX)
        return if (endsWithOperand(expr)) limit(expr, symbol.toString()) else expr
    }

    fun appendPowerOperator(expr: String): String = appendOperator(expr, '^')

    fun appendConstant(expr: String, constant: Char): String {
        require(constant == 'π' || constant == 'e')
        val prefix = if (expr.isNotEmpty() && (expr.last() == '.')) "0×" else ""
        return limit(expr, prefix + constant)
    }

    fun appendFunction(expr: String, name: String): String {
        val token = if (name == "√") "√" else "$name("
        return limit(expr, token)
    }

    fun openParenthesis(expr: String): String = limit(expr, "(")

    fun closeParenthesis(expr: String): String {
        val open = expr.count { it == '(' } - expr.count { it == ')' }
        return if (open > 0 && endsWithOperand(expr)) expr + ")" else expr
    }

    fun backspace(expr: String): String {
        if (expr.isEmpty()) return expr
        FUNCTION_PREFIXES.firstOrNull { expr.endsWith(it) }?.let { return expr.dropLast(it.length) }
        return expr.dropLast(1)
    }

    /** ± : negates the last operand. "12+5" → "12+(-5", "12+(-5" → "12+5". */
    fun toggleSign(expr: String): String {
        if (expr.isEmpty()) return "(-"
        val last = expr.last()
        if (last.isDigit() || last == '.') {
            val start = numberStart(expr)
            val number = expr.substring(start)
            val before = expr.substring(0, start)
            return when {
                before.endsWith("(-") -> before.dropLast(2) + number
                before == "-" -> number
                else -> before + "(-" + number
            }
        }
        if (last in BINARY_OPERATORS || last == '(' || last == '√' || expr.endsWithFunction()) {
            if (expr.endsWith("(-")) return expr.dropLast(2)
            return limit(expr, "(-")
        }
        // Operand that is a group/constant/postfix: wrap it.
        val start = lastOperandStart(expr) ?: return expr
        val operand = expr.substring(start)
        val before = expr.substring(0, start)
        if (operand.startsWith("(-") && operand.endsWith(")") && lastOperandStart(operand) == 0) {
            return before + operand.substring(2, operand.length - 1) // unwrap (-X) → X
        }
        return before + "(-" + operand + ")"
    }

    /** 1/x : replaces the last operand with (1÷operand). */
    fun reciprocal(expr: String): String {
        if (!endsWithOperand(expr)) return limit(expr, "1÷(")
        val trimmed = if (expr.last() == '.') expr.dropLast(1) else expr
        val start = lastOperandStart(trimmed) ?: return expr
        val operand = trimmed.substring(start)
        return trimmed.substring(0, start) + "(1÷" + operand + ")"
    }

    fun currentNumber(expr: String): String {
        if (expr.isEmpty()) return ""
        val last = expr.last()
        if (!last.isDigit() && last != '.') return ""
        return expr.substring(numberStart(expr))
    }

    private fun numberStart(expr: String): Int {
        var i = expr.length
        while (i > 0 && (expr[i - 1].isDigit() || expr[i - 1] == '.')) i--
        return i
    }

    /** Start index of the last complete operand (number, constant or parenthesised group with prefix function). */
    fun lastOperandStart(expr: String): Int? {
        if (expr.isEmpty()) return null
        var end = expr.length
        while (end > 0 && expr[end - 1] in POSTFIX) end--
        if (end == 0) return null
        val c = expr[end - 1]
        var start: Int = when {
            c.isDigit() || c == '.' -> numberStart(expr.substring(0, end))
            c == 'π' || c == 'e' -> end - 1
            c == ')' -> {
                var depth = 0
                var i = end - 1
                var found = -1
                while (i >= 0) {
                    when (expr[i]) {
                        ')' -> depth++
                        '(' -> { depth--; if (depth == 0) { found = i; break } }
                    }
                    i--
                }
                if (found < 0) return null
                var s = found
                for (f in listOf("sin", "cos", "tan", "log", "ln")) {
                    if (expr.substring(0, s).endsWith(f)) { s -= f.length; break }
                }
                s
            }
            else -> return null
        }
        while (start > 0 && expr[start - 1] == '√') start--
        return start
    }

    private fun String.endsWithFunction(): Boolean = FUNCTION_PREFIXES.any { endsWith(it) }
}
