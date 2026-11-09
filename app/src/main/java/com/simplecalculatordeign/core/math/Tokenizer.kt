package com.simplecalculatordeign.core.math

/**
 * Thrown for any invalid expression or impossible calculation. The message is
 * user-facing and is shown directly in the UI.
 */
class CalcException(message: String) : RuntimeException(message)

sealed interface Token {
    data class Num(val text: String) : Token
    data class Op(val symbol: Char) : Token            // + - × ÷ ^
    data class Func(val name: String) : Token          // sin cos tan log ln
    data class Const(val name: String) : Token         // π e
    data class Variable(val name: String) : Token      // x (equation solving only)
    data object LParen : Token
    data object RParen : Token
    data object Percent : Token
    data object Factorial : Token
    data object Square : Token
    data object Sqrt : Token
}

/**
 * Converts an expression string into tokens. Accepts both the calculator's own
 * symbols (× ÷ −) and keyboard/OCR friendly ASCII alternatives (* / -).
 */
object Tokenizer {

    val FUNCTIONS = setOf("sin", "cos", "tan", "log", "ln", "sqrt")

    fun tokenize(input: String, allowVariables: Boolean = false): List<Token> {
        val tokens = mutableListOf<Token>()
        val s = input
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() || c == ' ' || c == '_' -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    val sb = StringBuilder()
                    var dots = 0
                    while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) {
                        when (s[i]) {
                            '.' -> { dots++; sb.append('.') }
                            ',' -> Unit // grouping separator, ignored
                            else -> sb.append(s[i])
                        }
                        i++
                    }
                    // Optional exponent part, e.g. 1.5E-2500 (only upper-case E)
                    if (i < s.length && s[i] == 'E') {
                        var j = i + 1
                        if (j < s.length && (s[j] == '+' || s[j] == '-')) j++
                        if (j < s.length && s[j].isDigit()) {
                            sb.append('E')
                            if (s[i + 1] == '-' || s[i + 1] == '+') sb.append(s[i + 1])
                            i = j
                            while (i < s.length && s[i].isDigit()) { sb.append(s[i]); i++ }
                        }
                    }
                    val text = sb.toString()
                    if (dots > 1) throw CalcException("Invalid number '${s.substring(start, i)}'")
                    if (text == "." || text.isEmpty()) throw CalcException("Invalid number")
                    tokens += Token.Num(text)
                }

                c == '+' -> { tokens += Token.Op('+'); i++ }
                c == '-' || c == '−' || c == '–' || c == '—' -> { tokens += Token.Op('-'); i++ }
                c == '×' || c == '*' || c == '·' -> { tokens += Token.Op('×'); i++ }
                c == '÷' || c == '/' -> { tokens += Token.Op('÷'); i++ }
                c == '^' -> { tokens += Token.Op('^'); i++ }
                c == '(' || c == '[' || c == '{' -> { tokens += Token.LParen; i++ }
                c == ')' || c == ']' || c == '}' -> { tokens += Token.RParen; i++ }
                c == '%' -> { tokens += Token.Percent; i++ }
                c == '!' -> { tokens += Token.Factorial; i++ }
                c == '²' -> { tokens += Token.Square; i++ }
                c == '³' -> { tokens += Token.Op('^'); tokens += Token.Num("3"); i++ }
                c == '√' -> { tokens += Token.Sqrt; i++ }
                c == 'π' -> { tokens += Token.Const("π"); i++ }

                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter() && s[i] != 'π') i++
                    val word = s.substring(start, i)
                    tokens += readWord(word, allowVariables)
                }

                c == '=' -> throw CalcException("Remove '=' from the expression")
                else -> throw CalcException("Unexpected character '$c'")
            }
        }
        return tokens
    }

    private fun readWord(word: String, allowVariables: Boolean): List<Token> {
        val lower = word.lowercase()
        return when {
            lower in FUNCTIONS -> listOf(if (lower == "sqrt") Token.Sqrt else Token.Func(lower))
            lower == "pi" -> listOf(Token.Const("π"))
            word == "e" -> listOf(Token.Const("e"))
            allowVariables && lower == "x" -> listOf(Token.Variable("x"))
            !allowVariables && lower == "x" -> listOf(Token.Op('×'))
            allowVariables && lower.all { it == 'x' } -> List(lower.length) { Token.Variable("x") }
            else -> throw CalcException("Unknown symbol '$word'")
        }
    }
}
