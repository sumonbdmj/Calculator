package com.simplecalculatordeign.core.math

import java.math.BigDecimal

/** One line of OCR output. [confidence] is 0..1 when the recogniser provides it. */
data class OcrLine(val text: String, val confidence: Float? = null)

data class RecognizedExpression(
    /** The line exactly as OCR read it. */
    val sourceText: String,
    /** Normalised expression in calculator syntax, editable by the user. */
    val expression: String,
    /** A number written after '=' on the document, if any (e.g. a student's answer). */
    val writtenAnswer: String?,
    val uncertain: Boolean,
    val notes: List<String>,
)

data class DetectedNumber(
    val label: String,
    val value: BigDecimal,
    val sourceText: String,
    val uncertain: Boolean,
)

data class ExtractionResult(
    val expressions: List<RecognizedExpression>,
    /** Stand-alone numbers (prices, totals...). Never calculated automatically. */
    val numbers: List<DetectedNumber>,
)

/**
 * Turns raw OCR text into reviewable calculations.
 *
 * Rules:
 *  - Only text that contains an explicit operation (+ − × ÷ ^ % √) becomes an expression.
 *  - Dates and clock times are ignored, so 12/05/2024 is not treated as division.
 *  - Lone numbers are reported separately for the user to select; they are never summed silently.
 *  - Any character correction, ambiguous symbol or low OCR confidence marks the item as uncertain.
 */
object ExpressionExtractor {

    private const val LOW_CONFIDENCE = 0.75f

    private val DATE = Regex("""\b\d{1,4}[/.\-]\d{1,2}[/.\-]\d{2,4}\b""")
    private val TIME = Regex("""\b\d{1,2}:\d{2}(:\d{2})?\s*(?:[aApP]\.?[mM]\.?)?(?![\d])""")
    private val PHONE_LIKE = Regex("""\b\d{3,}-\d{3,}-\d{3,}\b""")
    private val NUMERIC_RUN = Regex("""(?<![A-Za-z])[0-9OoIlSB|][0-9OoIlSB|.,]*(?![A-Za-z])""")
    private val TIMES_LETTER = Regex("""(?<=[\d)\]])\s*[xX]\s*(?=[\d(\[])""")
    private val COLON_DIVIDE = Regex("""(?<=\d)\s*:\s*(?=\d)""")
    private val THOUSANDS = Regex("""(?<=\d),(?=\d{3}(?!\d))""")
    private val DECIMAL_COMMA = Regex("""(?<=\d),(?=\d)""")
    private val EXPRESSION_RUN = Regex("""[0-9.+\-×÷^()%√π\s]+""")
    private val BINARY_OP = Regex("""[\d)%π]\s*[+\-×÷^]\s*[\d(√π\-]""")
    private val NUMBER = Regex("""(?<![\d.])-?\d+(?:\.\d+)?""")

    fun extract(lines: List<OcrLine>): ExtractionResult {
        val expressions = mutableListOf<RecognizedExpression>()
        val numbers = mutableListOf<DetectedNumber>()
        for (line in lines) {
            val raw = line.text.trim()
            if (raw.isEmpty()) continue
            val notes = mutableListOf<String>()
            if (line.confidence != null && line.confidence < LOW_CONFIDENCE) {
                notes += "Low recognition confidence (${(line.confidence * 100).toInt()}%) – please check, especially for handwriting"
            }
            val normalized = normalize(raw, notes)

            val parts = normalized.split('=')
            val left = parts[0]
            val written = parts.getOrNull(1)?.let { NUMBER.find(it.replace(" ", ""))?.value }

            val expression = findExpression(left)
            if (expression != null) {
                if (PHONE_LIKE.containsMatchIn(raw)) notes += "Looks like it could be a phone number or ID"
                expressions += RecognizedExpression(
                    sourceText = raw,
                    expression = expression,
                    writtenAnswer = written,
                    uncertain = notes.isNotEmpty(),
                    notes = notes.toList(),
                )
            } else {
                val label = labelFor(left)
                NUMBER.findAll(normalized).forEach { m ->
                    val value = m.value.toBigDecimalOrNull() ?: return@forEach
                    numbers += DetectedNumber(label, value, raw, notes.isNotEmpty())
                }
            }
        }
        return ExtractionResult(expressions, numbers)
    }

    fun extractFromText(text: String): ExtractionResult =
        extract(text.lines().map { OcrLine(it) })

    /** Converts OCR quirks into calculator syntax. Adds notes for anything guessed. */
    fun normalize(raw: String, notes: MutableList<String>): String {
        var s = raw
        s = DATE.replace(s, " ")
        s = TIME.replace(s, " ")
        s = s.replace('−', '-').replace('–', '-').replace('—', '-').replace('‒', '-')
            .replace('✕', '×').replace('✖', '×').replace('·', '×').replace('∙', '×').replace('*', '×')
            .replace('＝', '=').replace('÷', '÷')
        // Fix letters that OCR confuses with digits, only inside numeric runs.
        var corrected = false
        s = NUMERIC_RUN.replace(s) { m ->
            val v = m.value
            if (v.none { it.isDigit() }) return@replace v
            val fixed = v.map {
                when (it) {
                    'O', 'o' -> '0'
                    'I', 'l', '|' -> '1'
                    'S' -> '5'
                    'B' -> '8'
                    else -> it
                }
            }.joinToString("")
            if (fixed != v) corrected = true
            fixed
        }
        if (corrected) notes += "Some unclear characters were read as digits"
        s = TIMES_LETTER.replace(s, "×")
        if (COLON_DIVIDE.containsMatchIn(s)) {
            s = COLON_DIVIDE.replace(s, "÷")
            notes += "':' was read as division"
        }
        s = s.replace(Regex("""(?<=[\d)])\s*/\s*(?=[\d(])"""), "÷")
        s = THOUSANDS.replace(s, "")
        if (DECIMAL_COMMA.containsMatchIn(s)) {
            s = DECIMAL_COMMA.replace(s, ".")
            notes += "A comma was read as a decimal point"
        }
        return s
    }

    private fun findExpression(text: String): String? {
        val candidates = EXPRESSION_RUN.findAll(text)
            .map { it.value.trim() }
            .map { trimDangling(it) }
            .filter { c -> c.any { it.isDigit() } }
            .filter { c -> BINARY_OP.containsMatchIn(c) || c.contains('√') || c.contains('%') && c.length > 1 }
            .sortedByDescending { it.length }
            .toList()
        for (candidate in candidates) {
            val compact = candidate.replace(Regex("""\s+"""), "")
            if (MathEngine.tryEvaluate(compact, EvalOptions(recordSteps = false)) != null) return compact
            val closed = MathEngine.closeParentheses(compact)
            if (MathEngine.tryEvaluate(closed, EvalOptions(recordSteps = false)) != null) return closed
        }
        return null
    }

    private fun trimDangling(s: String): String {
        var r = s.trim()
        while (r.isNotEmpty() && r.last() in "+-×÷^(.") r = r.dropLast(1).trimEnd()
        while (r.isNotEmpty() && r.first() in "+×÷^).") r = r.drop(1).trimStart()
        return r
    }

    private fun labelFor(text: String): String =
        text.replace(NUMBER, " ")
            .replace(Regex("""[:\-=*#$৳€£₹]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifEmpty { "Number" }
}
