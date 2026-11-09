package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class TutorAnswer(
    val title: String,
    val steps: List<String>,
    val finalAnswer: String?,
    /** false when the tutor could not understand the question. */
    val solved: Boolean,
)

/**
 * Offline math explainer used by the AI Math Assistant when no AI backend is
 * configured (or for questions it can answer exactly). Everything is computed with
 * the same precise engine; nothing is guessed.
 */
class LocalMathTutor(private val format: FormatOptions = FormatOptions(), private val precision: Int = 34) {

    private val mc = MathContext(precision, RoundingMode.HALF_EVEN)
    private fun f(v: BigDecimal) = NumberFormatter.format(v.round(mc).let { if (it.signum() == 0) BigDecimal.ZERO else it.stripTrailingZeros() }, format)

    private val num = """(-?\d[\d,]*(?:\.\d+)?)"""

    fun answer(question: String): TutorAnswer {
        val q = normalizeWords(question)
        return tryPercentOf(q)
            ?: tryWhatPercent(q)
            ?: tryIncreaseDecrease(q)
            ?: tryPercentChange(q)
            ?: tryDiscount(q)
            ?: trySimpleInterest(q)
            ?: tryCompoundInterest(q)
            ?: tryEmi(q)
            ?: tryEquation(q)
            ?: tryExpression(q)
            ?: TutorAnswer(
                title = "I couldn't understand that yet",
                steps = listOf(
                    "Offline mode understands expressions and common phrases, for example:",
                    "• 15% of 2400",
                    "• 300 is what percent of 1200",
                    "• increase 1200 by 15%",
                    "• percent change from 80 to 100",
                    "• 20% off 3500",
                    "• simple interest on 10000 at 5% for 3 years",
                    "• emi for 500000 at 9% for 5 years",
                    "• solve 2x + 5 = 17  or  x^2 - 5x + 6 = 0",
                ),
                finalAnswer = null,
                solved = false,
            )
    }

    /** Converts word problems into symbols: "5 plus 3 times 2" → "5 + 3 × 2". */
    fun normalizeWords(text: String): String {
        var s = " " + text.lowercase().trim().removeSuffix("?").removeSuffix(".") + " "
        val replacements = listOf(
            "multiplied by" to "×", "divided by" to "÷", "to the power of" to "^", "raised to" to "^",
            "square root of" to "√", "percent" to "%", "per cent" to "%",
            " plus " to " + ", " minus " to " − ", " times " to " × ", " over " to " ÷ ",
            " into " to " × ", " squared " to "² ", " cubed " to "³ ",
        )
        for ((from, to) in replacements) s = s.replace(from, to)
        s = s.replace("% age", "%").replace("what is ", "").replace("calculate ", "").replace("find ", "")
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    private fun n(s: String) = BigDecimal(s.replace(",", ""))

    private fun tryPercentOf(q: String): TutorAnswer? {
        val m = Regex("""^$num ?% of $num$""").find(q) ?: return null
        val p = n(m.groupValues[1]); val v = n(m.groupValues[2])
        val r = FinanceCalculators.percentOf(p, v)
        return TutorAnswer(
            "${f(p)}% of ${f(v)}",
            listOf(
                "Percent means \"per hundred\", so ${f(p)}% = ${f(p)} ÷ 100.",
                "Multiply by the value: ${f(v)} × ${f(p)} ÷ 100",
                "${f(v)} × ${f(p)} = ${f(v.multiply(p))}",
                "${f(v.multiply(p))} ÷ 100 = ${f(r)}",
            ),
            f(r), true,
        )
    }

    private fun tryWhatPercent(q: String): TutorAnswer? {
        val m = Regex("""^$num is what % of $num$""").find(q)
            ?: Regex("""^what % of $num is $num$""").find(q)?.let { mm ->
                return buildWhatPercent(n(mm.groupValues[2]), n(mm.groupValues[1]))
            }
            ?: return null
        return buildWhatPercent(n(m.groupValues[1]), n(m.groupValues[2]))
    }

    private fun buildWhatPercent(part: BigDecimal, whole: BigDecimal): TutorAnswer {
        val r = FinanceCalculators.whatPercent(part, whole)
        return TutorAnswer(
            "${f(part)} as a percentage of ${f(whole)}",
            listOf(
                "Formula: (part ÷ whole) × 100",
                "${f(part)} ÷ ${f(whole)} = ${f(part.divide(whole, mc))}",
                "${f(part.divide(whole, mc))} × 100 = ${f(r)}%",
            ),
            "${f(r)}%", true,
        )
    }

    private fun tryIncreaseDecrease(q: String): TutorAnswer? {
        val m = Regex("""^(increase|decrease|reduce) $num by $num ?%$""").find(q)
            ?: Regex("""^$num (increased|decreased|reduced) by $num ?%$""").find(q)?.let { mm ->
                return buildChange(mm.groupValues[2].startsWith("increase"), n(mm.groupValues[1]), n(mm.groupValues[3]))
            }
            ?: return null
        return buildChange(m.groupValues[1] == "increase", n(m.groupValues[2]), n(m.groupValues[3]))
    }

    private fun buildChange(increase: Boolean, value: BigDecimal, pct: BigDecimal): TutorAnswer {
        val amount = FinanceCalculators.percentOf(pct, value)
        val result = if (increase) value.add(amount) else value.subtract(amount)
        val word = if (increase) "Increase" else "Decrease"
        return TutorAnswer(
            "$word ${f(value)} by ${f(pct)}%",
            listOf(
                "Find ${f(pct)}% of ${f(value)}: ${f(value)} × ${f(pct)} ÷ 100 = ${f(amount)}",
                if (increase) "Add it: ${f(value)} + ${f(amount)} = ${f(result)}"
                else "Subtract it: ${f(value)} − ${f(amount)} = ${f(result)}",
            ),
            f(result), true,
        )
    }

    private fun tryPercentChange(q: String): TutorAnswer? {
        val m = Regex("""^(?:% change|% increase|% decrease|change) from $num to $num$""").find(q) ?: return null
        val a = n(m.groupValues[1]); val b = n(m.groupValues[2])
        val diff = b.subtract(a)
        val r = FinanceCalculators.percentChange(a, b)
        return TutorAnswer(
            "Percentage change from ${f(a)} to ${f(b)}",
            listOf(
                "Formula: (new − old) ÷ old × 100",
                "${f(b)} − ${f(a)} = ${f(diff)}",
                "${f(diff)} ÷ ${f(a)} = ${f(diff.divide(a, mc))}",
                "${f(diff.divide(a, mc))} × 100 = ${f(r)}%",
                if (r.signum() >= 0) "It is an increase of ${f(r)}%." else "It is a decrease of ${f(r.negate())}%.",
            ),
            "${f(r)}%", true,
        )
    }

    private fun tryDiscount(q: String): TutorAnswer? {
        val m = Regex("""^$num ?% (?:off|discount on) $num$""").find(q)
            ?: Regex("""^discount of $num ?% on $num$""").find(q)
            ?: return null
        val pct = n(m.groupValues[1]); val price = n(m.groupValues[2])
        val d = FinanceCalculators.discount(price, pct)
        return TutorAnswer(
            "${f(pct)}% discount on ${f(price)}",
            listOf(
                "Discount = ${f(price)} × ${f(pct)} ÷ 100 = ${f(d.discountAmount)}",
                "Price after discount = ${f(price)} − ${f(d.discountAmount)} = ${f(d.finalPrice)}",
                "You save ${f(d.totalSaved)}.",
            ),
            f(d.finalPrice), true,
        )
    }

    private fun trySimpleInterest(q: String): TutorAnswer? {
        if (!q.contains("simple interest")) return null
        val m = Regex("""$num.*?(?:at|@) $num ?%.*?(?:for )?$num (year|yr|month)""").find(q) ?: return null
        val p = n(m.groupValues[1]); val r = n(m.groupValues[2])
        var t = n(m.groupValues[3])
        val monthly = m.groupValues[4].startsWith("month")
        if (monthly) t = t.divide(BigDecimal(12), mc)
        val si = FinanceCalculators.simpleInterest(p, r, t)
        return TutorAnswer(
            "Simple interest",
            listOf(
                "Formula: SI = P × R × T ÷ 100",
                "P = ${f(p)}, R = ${f(r)}%, T = ${f(t)} years" + if (monthly) " (${m.groupValues[3]} months ÷ 12)" else "",
                "SI = ${f(p)} × ${f(r)} × ${f(t)} ÷ 100 = ${f(si)}",
                "Total amount = ${f(p)} + ${f(si)} = ${f(p.add(si))}",
            ),
            f(si), true,
        )
    }

    private fun tryCompoundInterest(q: String): TutorAnswer? {
        if (!q.contains("compound")) return null
        val m = Regex("""$num.*?(?:at|@) $num ?%.*?(?:for )?$num (year|yr)""").find(q) ?: return null
        val p = n(m.groupValues[1]); val r = n(m.groupValues[2]); val years = n(m.groupValues[3]).toInt()
        val amount = FinanceCalculators.compoundAmount(p, r, years)
        return TutorAnswer(
            "Compound interest (yearly)",
            listOf(
                "Formula: A = P × (1 + R/100)^T",
                "A = ${f(p)} × (1 + ${f(r)}/100)^$years",
                "A = ${f(amount)}",
                "Interest = A − P = ${f(amount.subtract(p))}",
            ),
            f(amount.subtract(p)), true,
        )
    }

    private fun tryEmi(q: String): TutorAnswer? {
        if (!q.contains("emi") && !q.contains("installment") && !q.contains("instalment")) return null
        val m = Regex("""$num.*?(?:at|@) $num ?%.*?(?:for )?$num (year|yr|month)""").find(q) ?: return null
        val p = n(m.groupValues[1]); val r = n(m.groupValues[2])
        val count = n(m.groupValues[3]).toInt()
        val months = if (m.groupValues[4].startsWith("month")) count else count * 12
        val res = FinanceCalculators.emi(p, r, months)
        val two = FormatOptions(format.grouping, 2)
        return TutorAnswer(
            "Loan EMI",
            listOf(
                "Formula: EMI = P × r × (1+r)^n ÷ ((1+r)^n − 1)",
                "P = ${f(p)}, annual rate = ${f(r)}% → monthly r = ${f(r)} ÷ 12 ÷ 100 = ${f(r.divide(BigDecimal(1200), mc))}",
                "n = $months months",
                "EMI = ${NumberFormatter.format(res.emi, two)} per month",
                "Total payment = ${NumberFormatter.format(res.totalPayment, two)}",
                "Total interest = ${NumberFormatter.format(res.totalInterest, two)}",
            ),
            NumberFormatter.format(res.emi, two), true,
        )
    }

    /** Linear and quadratic equations in x, found by sampling the expression exactly. */
    private fun tryEquation(q: String): TutorAnswer? {
        val text = q.removePrefix("solve").removePrefix(":").trim()
        if (!text.contains('x') || text.count { it == '=' } != 1) return null
        val (lhs, rhs) = text.split('=').map { it.trim() }
        if (lhs.isEmpty() || rhs.isEmpty()) return null
        fun g(x: Long): BigDecimal? {
            val vars = mapOf("x" to BigDecimal(x))
            val l = MathEngine.tryEvaluate(lhs, EvalOptions(precision = 50, variables = vars, recordSteps = false)) ?: return null
            val r = MathEngine.tryEvaluate(rhs, EvalOptions(precision = 50, variables = vars, recordSteps = false)) ?: return null
            return l.value.subtract(r.value)
        }
        val fm1 = g(-1) ?: return null
        val f0 = g(0) ?: return null
        val f1 = g(1) ?: return null
        val f2 = g(2) ?: return null
        val f3 = g(3) ?: return null
        val two = BigDecimal(2)
        val a = f1.add(fm1).divide(two).subtract(f0)
        val b = f1.subtract(fm1).divide(two)
        val c = f0
        fun model(x: Long) = a.multiply(BigDecimal(x * x)).add(b.multiply(BigDecimal(x))).add(c)
        val tolerance = BigDecimal("1e-30")
        if (model(2).subtract(f2).abs() > tolerance || model(3).subtract(f3).abs() > tolerance) {
            return TutorAnswer(
                "Equation not supported offline",
                listOf("Offline mode solves linear (ax + b = c) and quadratic (ax² + bx + c = 0) equations in x."),
                null, false,
            )
        }
        val steps = mutableListOf("Move everything to one side: $lhs − ($rhs) = 0")
        if (a.signum() == 0) {
            if (b.signum() == 0) {
                return TutorAnswer(
                    "Equation", steps + if (c.signum() == 0) "Every x satisfies it (identity)." else "No x satisfies it (contradiction).",
                    if (c.signum() == 0) "All real numbers" else "No solution", true,
                )
            }
            val x = c.negate().divide(b, mc)
            steps += "This is linear: ${f(b)}x + ${f(c)} = 0"
            steps += "${f(b)}x = ${f(c.negate())}"
            steps += "x = ${f(c.negate())} ÷ ${f(b)} = ${f(x)}"
            return TutorAnswer("Solve for x", steps, "x = ${f(x)}", true)
        }
        steps += "This is quadratic: a = ${f(a)}, b = ${f(b)}, c = ${f(c)}"
        val disc = b.multiply(b).subtract(BigDecimal(4).multiply(a).multiply(c))
        steps += "Discriminant D = b² − 4ac = ${f(b.multiply(b))} − ${f(BigDecimal(4).multiply(a).multiply(c))} = ${f(disc)}"
        if (disc.signum() < 0) {
            steps += "D < 0, so there are no real solutions."
            return TutorAnswer("Solve for x", steps, "No real solution", true)
        }
        val sqrtD = BigMath(precision).sqrt(disc)
        val twoA = two.multiply(a)
        val x1 = b.negate().add(sqrtD).divide(twoA, mc)
        val x2 = b.negate().subtract(sqrtD).divide(twoA, mc)
        steps += "√D = ${f(sqrtD)}"
        steps += "x = (−b ± √D) ÷ 2a = (${f(b.negate())} ± ${f(sqrtD)}) ÷ ${f(twoA)}"
        return if (disc.signum() == 0) {
            TutorAnswer("Solve for x", steps, "x = ${f(x1)}", true)
        } else {
            steps += "x₁ = ${f(x1)},  x₂ = ${f(x2)}"
            TutorAnswer("Solve for x", steps, "x = ${f(x1)} or x = ${f(x2)}", true)
        }
    }

    private fun tryExpression(q: String): TutorAnswer? {
        val cleaned = q.replace(Regex("""[a-z ]+$"""), "").trim()
        if (cleaned.isEmpty() || !cleaned.any { it.isDigit() }) return null
        val result = MathEngine.tryEvaluate(cleaned, EvalOptions(precision = precision)) ?: return null
        val steps = if (result.steps.isEmpty()) listOf("This is already a single number.")
        else result.steps.mapIndexed { i, s -> "Step ${i + 1}: ${NumberFormatter.formatStep(s, format)}" }
        val prefix = if (result.approximate) "≈ " else ""
        return TutorAnswer(
            NumberFormatter.formatExpression(cleaned, format.grouping),
            steps + listOf("Rule used: brackets → powers/roots → × and ÷ (left to right) → + and − (left to right)."),
            prefix + NumberFormatter.format(result.value, format), true,
        )
    }
}
