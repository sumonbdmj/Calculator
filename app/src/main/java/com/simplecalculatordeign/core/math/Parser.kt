package com.simplecalculatordeign.core.math

import java.math.BigDecimal

/** Abstract syntax tree of a parsed expression. */
sealed interface Node {
    data class Number(val value: BigDecimal) : Node
    data class Constant(val name: String) : Node
    data class Variable(val name: String) : Node
    data class Negate(val operand: Node) : Node
    data class Binary(val op: Char, val left: Node, val right: Node) : Node
    data class Percent(val operand: Node) : Node
    data class Factorial(val operand: Node) : Node
    data class Function(val name: String, val arg: Node) : Node
}

/**
 * Recursive-descent parser.
 *
 * Grammar (lowest → highest precedence):
 *   expression := term (('+' | '-') term)*
 *   term       := unary (('×' | '÷') unary | <implicit ×> unary)*
 *   unary      := ('-' | '+') unary | power
 *   power      := postfix ('^' unary)?          (right associative)
 *   postfix    := primary ('%' | '!' | '²')*
 *   primary    := number | constant | variable | '(' expression ')' | function | '√' arg
 */
class Parser(private val tokens: List<Token>) {

    private var pos = 0
    private var depth = 0

    fun parse(): Node {
        if (tokens.isEmpty()) throw CalcException("Enter an expression")
        val node = parseExpression()
        if (pos < tokens.size) {
            throw if (tokens[pos] == Token.RParen) CalcException("Unmatched closing parenthesis")
            else CalcException("Unexpected ${describe(tokens[pos])}")
        }
        return node
    }

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun parseExpression(): Node {
        if (++depth > MAX_DEPTH) throw CalcException("Expression is nested too deeply")
        var left = parseTerm()
        while (true) {
            val t = peek()
            if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                pos++
                left = Node.Binary(t.symbol, left, parseTerm())
            } else break
        }
        depth--
        return left
    }

    private fun parseTerm(): Node {
        var left = parseUnary()
        while (true) {
            val t = peek()
            left = when {
                t is Token.Op && (t.symbol == '×' || t.symbol == '÷') -> {
                    pos++
                    Node.Binary(t.symbol, left, parseUnary())
                }
                startsImplicitOperand(t) -> Node.Binary('×', left, parseUnary())
                else -> return left
            }
        }
    }

    /** Implicit multiplication: 2π, 3(4+1), (2)(3), 2√9, 2sin(30). Not between two plain numbers. */
    private fun startsImplicitOperand(t: Token?): Boolean =
        t == Token.LParen || t is Token.Const || t is Token.Variable || t is Token.Func || t == Token.Sqrt

    private fun parseUnary(): Node {
        val t = peek()
        if (t is Token.Op && t.symbol == '-') { pos++; return Node.Negate(parseUnary()) }
        if (t is Token.Op && t.symbol == '+') { pos++; return parseUnary() }
        return parsePower()
    }

    private fun parsePower(): Node {
        val base = parsePostfix()
        val t = peek()
        if (t is Token.Op && t.symbol == '^') {
            pos++
            if (peek() == null) throw CalcException("Missing exponent")
            return Node.Binary('^', base, parseUnary())
        }
        return base
    }

    private fun parsePostfix(): Node {
        var node = parsePrimary()
        while (true) {
            node = when (peek()) {
                Token.Percent -> { pos++; Node.Percent(node) }
                Token.Factorial -> { pos++; Node.Factorial(node) }
                Token.Square -> { pos++; Node.Binary('^', node, Node.Number(BigDecimal(2))) }
                else -> return node
            }
        }
    }

    private fun parsePrimary(): Node {
        val t = peek() ?: throw CalcException("Incomplete expression")
        return when (t) {
            is Token.Num -> {
                pos++
                val value = try { BigDecimal(t.text) } catch (e: NumberFormatException) {
                    throw CalcException("Invalid number '${t.text}'")
                }
                Node.Number(value)
            }
            is Token.Const -> { pos++; Node.Constant(t.name) }
            is Token.Variable -> { pos++; Node.Variable(t.name) }
            Token.LParen -> {
                pos++
                if (peek() == Token.RParen) throw CalcException("Empty parentheses")
                val inner = parseExpression()
                if (peek() != Token.RParen) throw CalcException("Missing closing parenthesis")
                pos++
                inner
            }
            is Token.Func -> { pos++; Node.Function(t.name, parseFunctionArgument(t.name)) }
            Token.Sqrt -> { pos++; Node.Function("sqrt", parseFunctionArgument("√")) }
            else -> throw CalcException("Unexpected ${describe(t)}")
        }
    }

    /** sin(30)² means (sin 30)², while sin30 / √9 take the following operand. */
    private fun parseFunctionArgument(name: String): Node {
        val next = peek() ?: throw CalcException("Missing value after $name")
        if (next == Token.LParen) return parsePrimary()
        if (next is Token.Op && next.symbol == '-') { pos++; return Node.Negate(parseFunctionArgument(name)) }
        return parsePostfix()
    }

    private fun describe(t: Token): String = when (t) {
        is Token.Num -> "number ${t.text}"
        is Token.Op -> "operator '${t.symbol}'"
        is Token.Func -> "function ${t.name}"
        is Token.Const -> "'${t.name}'"
        is Token.Variable -> "'${t.name}'"
        Token.LParen -> "'('"
        Token.RParen -> "')'"
        Token.Percent -> "'%'"
        Token.Factorial -> "'!'"
        Token.Square -> "'²'"
        Token.Sqrt -> "'√'"
    }

    companion object {
        private const val MAX_DEPTH = 250
    }
}
