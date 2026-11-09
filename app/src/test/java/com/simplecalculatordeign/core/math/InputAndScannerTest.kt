package com.simplecalculatordeign.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class InputEditorTest {

    @Test fun operatorReplacement() {
        assertEquals("5+", InputEditor.appendOperator("5×", '+'))
        assertEquals("5×-", InputEditor.appendOperator("5×", '-'))
        assertEquals("5+", InputEditor.appendOperator("5×-", '+'))
        assertEquals("-", InputEditor.appendOperator("", '-'))
        assertEquals("", InputEditor.appendOperator("", '×'))
    }

    @Test fun decimalPoint() {
        assertEquals("0.", InputEditor.appendDecimal(""))
        assertEquals("5.", InputEditor.appendDecimal("5"))
        assertEquals("5.2", InputEditor.appendDecimal("5.2"))
        assertEquals("5+0.", InputEditor.appendDecimal("5+"))
        assertEquals("(2)×0.", InputEditor.appendDecimal("(2)"))
    }

    @Test fun digitsAndImplicitMultiply() {
        assertEquals("7", InputEditor.appendDigit("0", '7'))
        assertEquals("(2)×3", InputEditor.appendDigit("(2)", '3'))
        assertEquals("π×2", InputEditor.appendDigit("π", '2'))
    }

    @Test fun toggleSign() {
        assertEquals("12+(-5", InputEditor.toggleSign("12+5"))
        assertEquals("12+5", InputEditor.toggleSign("12+(-5"))
        assertEquals("(-", InputEditor.toggleSign(""))
        assertEquals("(-(2+3))", InputEditor.toggleSign("(2+3)"))
        assertEquals("(2+3)", InputEditor.toggleSign("(-(2+3))"))
    }

    @Test fun reciprocalAndBackspace() {
        assertEquals("2+(1÷8)", InputEditor.reciprocal("2+8"))
        assertEquals("(1÷(2+3))", InputEditor.reciprocal("(2+3)"))
        assertEquals("(1÷sin(30))", InputEditor.reciprocal("sin(30)"))
        assertEquals("5+", InputEditor.backspace("5+sin("))
        assertEquals("5", InputEditor.backspace("5+"))
    }

    @Test fun parentheses() {
        assertEquals("(5+3)", InputEditor.closeParenthesis("(5+3"))
        assertEquals("(5+", InputEditor.closeParenthesis("(5+"))
        assertEquals("5", InputEditor.closeParenthesis("5"))
    }
}

class ExpressionExtractorTest {

    @Test fun sampleDocument() {
        val text = """
            500 × 20 =
            1200 + 350 =
            5000 ÷ 10 =
        """.trimIndent()
        val result = ExpressionExtractor.extractFromText(text)
        assertEquals(listOf("500×20", "1200+350", "5000÷10"), result.expressions.map { it.expression })
        val values = result.expressions.map { NumberFormatter.format(MathEngine.evaluate(it.expression).value) }
        assertEquals(listOf("10,000", "1,550", "500"), values)
    }

    @Test fun ocrQuirks() {
        val r = ExpressionExtractor.extractFromText("1,250 x 4 = 5000\nl2O / 4 =")
        assertEquals("1250×4", r.expressions[0].expression)
        assertEquals("5000", r.expressions[0].writtenAnswer)
        assertFalse(r.expressions[0].uncertain)
        assertEquals("120÷4", r.expressions[1].expression)
        assertTrue(r.expressions[1].uncertain)
    }

    @Test fun numbersAreNotAssumedToBeCalculations() {
        val r = ExpressionExtractor.extractFromText("Invoice 12/05/2024\nCoffee 120.50\nTea 80\nTotal: 200.50")
        assertTrue(r.expressions.isEmpty())
        assertEquals(listOf("120.50", "80", "200.50"), r.numbers.map { it.value.toPlainString() })
        assertEquals("Coffee", r.numbers[0].label)
    }

    @Test fun labelledExpressionAndLowConfidence() {
        val r = ExpressionExtractor.extract(listOf(OcrLine("Qty 2 x 350", 0.5f), OcrLine("(250 + 150) × 5 =", 0.95f)))
        assertEquals("2×350", r.expressions[0].expression)
        assertTrue(r.expressions[0].uncertain)
        assertEquals("(250+150)×5", r.expressions[1].expression)
        assertFalse(r.expressions[1].uncertain)
    }

    @Test fun timesAreIgnored() {
        val r = ExpressionExtractor.extractFromText("Meeting 10:30 am")
        assertTrue(r.expressions.isEmpty())
    }
}

class FinanceAndUnitsTest {

    @Test fun percentageTools() {
        assertEquals("360", FinanceCalculators.percentOf(BigDecimal(15), BigDecimal(2400)).toPlainString())
        assertEquals("25", FinanceCalculators.whatPercent(BigDecimal(300), BigDecimal(1200)).toPlainString())
        assertEquals("25", FinanceCalculators.percentChange(BigDecimal(80), BigDecimal(100)).toPlainString())
        assertEquals("1380", FinanceCalculators.applyPercent(BigDecimal(1200), BigDecimal(15)).toPlainString())
    }

    @Test fun discountAndVat() {
        val d = FinanceCalculators.discount(BigDecimal(3500), BigDecimal(20))
        assertEquals("2800", d.finalPrice.toPlainString())
        assertEquals("700", d.totalSaved.toPlainString())
        val add = FinanceCalculators.vat(BigDecimal(1000), BigDecimal(15), inclusive = false)
        assertEquals("1150", add.gross.toPlainString())
        val remove = FinanceCalculators.vat(BigDecimal(1150), BigDecimal(15), inclusive = true)
        assertEquals("1000", remove.net.toPlainString())
        assertEquals("150", remove.tax.toPlainString())
    }

    @Test fun emi() {
        val r = FinanceCalculators.emi(BigDecimal(500000), BigDecimal(9), 60)
        assertEquals("10379.18", r.emi.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        assertEquals(60, r.schedule.size)
        assertEquals(0, r.schedule.last().balance.signum())
        val zero = FinanceCalculators.emi(BigDecimal(1200), BigDecimal.ZERO, 12)
        assertEquals("100", zero.emi.toPlainString())
    }

    @Test fun units() {
        val length = UnitConverter.units.getValue(UnitCategory.LENGTH)
        val km = length.first { it.id == "km" }
        val mi = length.first { it.id == "mi" }
        assertEquals("1.609344", UnitConverter.convert(BigDecimal.ONE, mi, km, UnitCategory.LENGTH).toPlainString())
        val temp = UnitConverter.units.getValue(UnitCategory.TEMPERATURE)
        val c = temp.first { it.id == "c" }
        val f = temp.first { it.id == "f" }
        assertEquals("212", UnitConverter.convert(BigDecimal(100), c, f, UnitCategory.TEMPERATURE).toPlainString())
        assertEquals("37", UnitConverter.convert(BigDecimal("98.6"), f, c, UnitCategory.TEMPERATURE).toPlainString())
    }
}

class LocalMathTutorTest {

    private val tutor = LocalMathTutor()

    @Test fun phrases() {
        assertEquals("360", tutor.answer("What is 15% of 2400?").finalAnswer)
        assertEquals("25%", tutor.answer("300 is what percent of 1200").finalAnswer)
        assertEquals("1,380", tutor.answer("increase 1200 by 15%").finalAnswer)
        assertEquals("2,800", tutor.answer("20% off 3500").finalAnswer)
        assertEquals("1,500", tutor.answer("simple interest on 10000 at 5% for 3 years").finalAnswer)
        assertEquals("10,379.18", tutor.answer("emi for 500000 at 9% for 5 years").finalAnswer)
    }

    @Test fun equations() {
        assertEquals("x = 6", tutor.answer("solve 2x + 5 = 17").finalAnswer)
        assertEquals("x = 3 or x = 2", tutor.answer("x^2 - 5x + 6 = 0").finalAnswer)
        assertEquals("No real solution", tutor.answer("x^2 + 1 = 0").finalAnswer)
    }

    @Test fun wordExpression() {
        val a = tutor.answer("5 plus 3 times 2")
        assertEquals("11", a.finalAnswer)
        assertTrue(a.solved)
        assertNull(tutor.answer("tell me a joke").finalAnswer)
    }
}
