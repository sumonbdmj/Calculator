package com.simplecalculatordeign.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplecalculatordeign.core.math.FinanceCalculators
import com.simplecalculatordeign.core.math.FormatOptions
import com.simplecalculatordeign.core.math.NumberFormatter
import com.simplecalculatordeign.ui.LocalAppSettings
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.GradientCard
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.components.InfoBanner
import com.simplecalculatordeign.ui.components.NumberField
import com.simplecalculatordeign.ui.components.PillChip
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.ResultRow
import com.simplecalculatordeign.ui.components.SectionTitle
import com.simplecalculatordeign.ui.theme.Brand
import com.simplecalculatordeign.ui.theme.CalcTheme
import java.math.BigDecimal
import java.math.RoundingMode

// ===================================================================== shared helpers

internal fun String.toAmount(): BigDecimal? = trim().takeIf { it.isNotEmpty() && it != "." && it != "-" }?.toBigDecimalOrNull()

/** Formats money-like values: full precision, but at most [maxDecimals] places (marked ≈ when rounded). */
@Composable
internal fun fmt(value: BigDecimal, maxDecimals: Int = 2): String {
    val base = LocalAppSettings.current.formatOptions
    val rounded = value.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros().let { if (it.signum() == 0) BigDecimal.ZERO else it }
    val prefix = if (rounded.compareTo(value) != 0) "≈ " else ""
    val opts = if (base.decimalPlaces >= 0) base else FormatOptions(base.grouping, -1)
    return prefix + NumberFormatter.format(rounded, opts)
}

@Composable
internal fun ToolFrame(
    title: String,
    icon: ImageVector,
    brush: Brush,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, brush, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun ChipsRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
internal fun ResultHero(label: String, value: String, copyValue: String? = null, footer: String? = null) {
    GradientCard {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Text(value, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
        if (footer != null) {
            Spacer(Modifier.height(4.dp))
            Text(footer, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
        if (copyValue != null) {
            val copy = com.simplecalculatordeign.ui.rememberCopier()
            Spacer(Modifier.height(8.dp))
            Text(
                "Copy",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { copy(label, copyValue) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun FormulaCard(lines: List<String>) {
    PremiumCard {
        Text("How it's calculated", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

// ===================================================================== Percentage

private enum class PercentMode(val label: String) { Of("X% of Y"), WhatPercent("X is what % of Y"), Change("% change"), Increase("Increase by %"), Decrease("Decrease by %") }

@Composable
fun PercentageScreen(onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(PercentMode.Of) }
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }

    ToolFrame("Percentage", Icons.Rounded.Percent, Brush.linearGradient(listOf(Brand.Cyan, Brand.Blue)), "Every common percentage question, exact to the last digit.", onBack) {
        ChipsRow { PercentMode.entries.forEach { m -> PillChip(m.label, mode == m, { mode = m }) } }
        val (labelA, labelB) = when (mode) {
            PercentMode.Of -> "Percentage (X)" to "Value (Y)"
            PercentMode.WhatPercent -> "Part (X)" to "Whole (Y)"
            PercentMode.Change -> "From (old value)" to "To (new value)"
            PercentMode.Increase, PercentMode.Decrease -> "Value" to "Percentage"
        }
        PremiumCard {
            NumberField(labelA, a, { a = it }, suffix = if (mode == PercentMode.Of) "%" else null, allowNegative = true)
            Spacer(Modifier.height(10.dp))
            NumberField(labelB, b, { b = it }, suffix = if (mode == PercentMode.Increase || mode == PercentMode.Decrease) "%" else null, allowNegative = true, imeDone = true)
        }
        val x = a.toAmount()
        val y = b.toAmount()
        if (x != null && y != null) {
            val outcome = runCatching {
                when (mode) {
                    PercentMode.Of -> Triple("${fmtPlain(x)}% of ${fmtPlain(y)}", FinanceCalculators.percentOf(x, y), listOf("${fmtPlain(y)} × ${fmtPlain(x)} ÷ 100"))
                    PercentMode.WhatPercent -> Triple("${fmtPlain(x)} is", FinanceCalculators.whatPercent(x, y), listOf("${fmtPlain(x)} ÷ ${fmtPlain(y)} × 100"))
                    PercentMode.Change -> Triple("Change", FinanceCalculators.percentChange(x, y), listOf("(${fmtPlain(y)} − ${fmtPlain(x)}) ÷ ${fmtPlain(x)} × 100"))
                    PercentMode.Increase -> Triple("Result", FinanceCalculators.applyPercent(x, y), listOf("${fmtPlain(x)} + ${fmtPlain(x)} × ${fmtPlain(y)} ÷ 100"))
                    PercentMode.Decrease -> Triple("Result", FinanceCalculators.applyPercent(x, y.negate()), listOf("${fmtPlain(x)} − ${fmtPlain(x)} × ${fmtPlain(y)} ÷ 100"))
                }
            }
            outcome.onSuccess { (label, value, formula) ->
                val isPercent = mode == PercentMode.WhatPercent || mode == PercentMode.Change
                ResultHero(label, fmt(value, 10) + if (isPercent) "%" else "", NumberFormatter.plain(value),
                    footer = when {
                        mode == PercentMode.Change && value.signum() > 0 -> "An increase"
                        mode == PercentMode.Change && value.signum() < 0 -> "A decrease"
                        mode == PercentMode.Increase || mode == PercentMode.Decrease ->
                            "Difference: ${fmt(FinanceCalculators.percentOf(y, x))}"
                        else -> null
                    })
                FormulaCard(formula)
            }.onFailure { InfoBanner(it.message ?: "Invalid input", Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun fmtPlain(v: BigDecimal) = NumberFormatter.plain(v)

// ===================================================================== Discount

@Composable
fun DiscountScreen(onBack: () -> Unit) {
    var price by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("") }

    ToolFrame("Discount", Icons.Rounded.LocalOffer, Brush.linearGradient(listOf(Brand.Amber, Brand.Orange)), "Find the sale price and how much you save.", onBack) {
        PremiumCard {
            NumberField("Original price", price, { price = it })
            Spacer(Modifier.height(10.dp))
            NumberField("Discount", discount, { discount = it }, suffix = "%")
            Spacer(Modifier.height(4.dp))
            ChipsRow { listOf("5", "10", "15", "20", "25", "30", "50").forEach { p -> PillChip("$p%", discount == p, { discount = p }) } }
            Spacer(Modifier.height(10.dp))
            NumberField("Extra discount (optional)", extra, { extra = it }, suffix = "%")
            Spacer(Modifier.height(10.dp))
            NumberField("Tax after discount (optional)", tax, { tax = it }, suffix = "%", imeDone = true)
        }
        val p = price.toAmount()
        val d = discount.toAmount()
        if (p != null && d != null) {
            runCatching { FinanceCalculators.discount(p, d, extra.toAmount() ?: BigDecimal.ZERO, tax.toAmount() ?: BigDecimal.ZERO) }
                .onSuccess { r ->
                    ResultHero("Final price", fmt(r.finalPrice), NumberFormatter.plain(r.finalPrice), footer = "You save ${fmt(r.totalSaved)} (${fmt(r.effectiveDiscountPercent, 4)}% off)")
                    PremiumCard {
                        ResultRow("Original price", fmt(r.originalPrice))
                        ResultRow("Total discount", "− " + fmt(r.discountAmount), valueColor = CalcTheme.colors.success)
                        ResultRow("Price after discount", fmt(r.priceAfterDiscount))
                        if (r.taxAmount.signum() != 0) ResultRow("Tax", "+ " + fmt(r.taxAmount))
                        ResultRow("Final price", fmt(r.finalPrice), emphasize = true, copyValue = NumberFormatter.plain(r.finalPrice))
                    }
                    if ((extra.toAmount() ?: BigDecimal.ZERO).signum() != 0) {
                        InfoBanner(
                            "Discounts stack: ${discount}% + ${extra}% is ${fmt(r.effectiveDiscountPercent, 4)}% in total, not ${fmt(d.add(extra.toAmount()!!))}%.",
                            Icons.Rounded.LocalOffer,
                        )
                    }
                }
                .onFailure { InfoBanner(it.message ?: "Invalid input", Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// ===================================================================== GST / VAT

@Composable
fun VatScreen(onBack: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("15") }
    var inclusive by rememberSaveable { mutableStateOf(false) }

    ToolFrame("GST / VAT", Icons.Rounded.Receipt, Brush.linearGradient(listOf(Brand.Green, Brand.Teal)), "Add tax to a net price, or extract it from a gross price.", onBack) {
        ChipsRow {
            PillChip("Add VAT", !inclusive, { inclusive = false })
            PillChip("Remove VAT", inclusive, { inclusive = true })
        }
        PremiumCard {
            NumberField(if (inclusive) "Amount including VAT" else "Amount excluding VAT", amount, { amount = it })
            Spacer(Modifier.height(10.dp))
            NumberField("Rate", rate, { rate = it }, suffix = "%", imeDone = true)
            Spacer(Modifier.height(6.dp))
            ChipsRow { listOf("5", "7.5", "10", "12", "15", "18", "28").forEach { r -> PillChip("$r%", rate == r, { rate = r }) } }
        }
        val a = amount.toAmount()
        val r = rate.toAmount()
        if (a != null && r != null) {
            runCatching { FinanceCalculators.vat(a, r, inclusive) }
                .onSuccess { res ->
                    ResultHero(if (inclusive) "Net amount (excl. VAT)" else "Gross amount (incl. VAT)", fmt(if (inclusive) res.net else res.gross),
                        NumberFormatter.plain(if (inclusive) res.net else res.gross))
                    PremiumCard {
                        ResultRow("Net amount", fmt(res.net), copyValue = NumberFormatter.plain(res.net))
                        ResultRow("VAT / GST (${NumberFormatter.plain(r)}%)", fmt(res.tax), copyValue = NumberFormatter.plain(res.tax))
                        ResultRow("Gross amount", fmt(res.gross), emphasize = true, copyValue = NumberFormatter.plain(res.gross))
                    }
                    FormulaCard(
                        if (inclusive) listOf("Net = Gross × 100 ÷ (100 + rate)", "VAT = Gross − Net")
                        else listOf("VAT = Net × rate ÷ 100", "Gross = Net + VAT")
                    )
                }
                .onFailure { InfoBanner(it.message ?: "Invalid input", Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// ===================================================================== Loan / EMI

@Composable
fun LoanScreen(onBack: () -> Unit) {
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var tenure by rememberSaveable { mutableStateOf("") }
    var inYears by rememberSaveable { mutableStateOf(true) }
    var showSchedule by rememberSaveable { mutableStateOf(false) }

    ToolFrame("Loan / EMI", Icons.Rounded.AccountBalance, Brush.linearGradient(listOf(Brand.Indigo, Brand.Purple)), "Monthly instalment, total interest and full repayment schedule.", onBack) {
        PremiumCard {
            NumberField("Loan amount", principal, { principal = it })
            Spacer(Modifier.height(10.dp))
            NumberField("Annual interest rate", rate, { rate = it }, suffix = "%")
            Spacer(Modifier.height(10.dp))
            NumberField("Tenure", tenure, { tenure = it.filter { c -> c.isDigit() } }, suffix = if (inYears) "years" else "months", imeDone = true)
            Spacer(Modifier.height(6.dp))
            ChipsRow {
                PillChip("Years", inYears, { inYears = true })
                PillChip("Months", !inYears, { inYears = false })
            }
        }
        val p = principal.toAmount()
        val r = rate.toAmount()
        val t = tenure.toIntOrNull()
        if (p != null && r != null && t != null) {
            val months = if (inYears) t * 12 else t
            val result = remember(p, r, months) { runCatching { FinanceCalculators.emi(p, r, months) } }
            result.onSuccess { res ->
                ResultHero("Monthly EMI", fmt(res.emi), NumberFormatter.plain(res.emi.setScale(2, RoundingMode.HALF_UP)), footer = "$months monthly payments")
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val principalShare = if (res.totalPayment.signum() == 0) 1f else p.divide(res.totalPayment, java.math.MathContext.DECIMAL64).toFloat()
                        DonutChart(principalShare, Modifier.size(110.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Legend(Brand.Purple, "Principal", fmt(p))
                            Legend(Brand.Orange, "Interest", fmt(res.totalInterest))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ResultRow("Total interest", fmt(res.totalInterest), copyValue = NumberFormatter.plain(res.totalInterest))
                    ResultRow("Total payment", fmt(res.totalPayment), emphasize = true, copyValue = NumberFormatter.plain(res.totalPayment))
                }
                FormulaCard(listOf("EMI = P × r × (1+r)ⁿ ÷ ((1+r)ⁿ − 1)", "r = annual rate ÷ 12 ÷ 100,  n = months"))
                PremiumCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showSchedule = !showSchedule },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Repayment schedule", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(if (showSchedule) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                    }
                    if (showSchedule) {
                        Spacer(Modifier.height(8.dp))
                        ScheduleHeader()
                        res.schedule.forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Cell("${row.month}", 0.5f)
                                Cell(fmt(row.principal), 1f)
                                Cell(fmt(row.interest), 1f)
                                Cell(fmt(row.balance), 1.2f)
                            }
                        }
                    }
                }
            }.onFailure { InfoBanner(it.message ?: "Invalid input", Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ScheduleHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        listOf("#" to 0.5f, "Principal" to 1f, "Interest" to 1f, "Balance" to 1.2f).forEach { (t, w) ->
            Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(w), textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float) {
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(weight), textAlign = TextAlign.End, maxLines = 1)
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun DonutChart(principalShare: Float, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        val principalSweep = 360f * principalShare.coerceIn(0f, 1f)
        drawArc(Brand.Orange, -90f + principalSweep, 360f - principalSweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawArc(Brand.Purple, -90f, principalSweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
    }
}

