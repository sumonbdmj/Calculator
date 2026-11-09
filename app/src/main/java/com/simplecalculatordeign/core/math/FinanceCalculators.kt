package com.simplecalculatordeign.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Precise BigDecimal implementations behind the Percentage, Discount, VAT and EMI screens. */
object FinanceCalculators {

    private val MC = MathContext(40, RoundingMode.HALF_EVEN)
    private val HUNDRED = BigDecimal(100)

    private fun div(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.signum() == 0) throw CalcException("Cannot divide by zero")
        return try { a.divide(b) } catch (e: ArithmeticException) { a.divide(b, MC) }
    }

    private fun clean(v: BigDecimal): BigDecimal = if (v.signum() == 0) BigDecimal.ZERO else v.stripTrailingZeros()

    // ------------------------------------------------------------ percentage

    /** X% of Y */
    fun percentOf(percent: BigDecimal, value: BigDecimal): BigDecimal = clean(percent.multiply(value).divide(HUNDRED))

    /** X is what % of Y */
    fun whatPercent(part: BigDecimal, whole: BigDecimal): BigDecimal = clean(div(part.multiply(HUNDRED), whole))

    /** % change from old to new */
    fun percentChange(from: BigDecimal, to: BigDecimal): BigDecimal = clean(div(to.subtract(from).multiply(HUNDRED), from))

    /** value increased (or decreased when negative) by percent */
    fun applyPercent(value: BigDecimal, percent: BigDecimal): BigDecimal =
        clean(value.add(value.multiply(percent).divide(HUNDRED)))

    // ------------------------------------------------------------ discount

    data class DiscountResult(
        val originalPrice: BigDecimal,
        val discountAmount: BigDecimal,
        val priceAfterDiscount: BigDecimal,
        val taxAmount: BigDecimal,
        val finalPrice: BigDecimal,
        val totalSaved: BigDecimal,
        val effectiveDiscountPercent: BigDecimal,
    )

    fun discount(
        price: BigDecimal,
        discountPercent: BigDecimal,
        extraDiscountPercent: BigDecimal = BigDecimal.ZERO,
        taxPercent: BigDecimal = BigDecimal.ZERO,
    ): DiscountResult {
        if (discountPercent > HUNDRED || extraDiscountPercent > HUNDRED) throw CalcException("Discount cannot exceed 100%")
        val first = price.multiply(discountPercent).divide(HUNDRED)
        val afterFirst = price.subtract(first)
        val second = afterFirst.multiply(extraDiscountPercent).divide(HUNDRED)
        val afterDiscount = afterFirst.subtract(second)
        val tax = afterDiscount.multiply(taxPercent).divide(HUNDRED)
        val saved = first.add(second)
        val effective = if (price.signum() == 0) BigDecimal.ZERO else div(saved.multiply(HUNDRED), price)
        return DiscountResult(
            clean(price), clean(saved), clean(afterDiscount), clean(tax),
            clean(afterDiscount.add(tax)), clean(saved), clean(effective),
        )
    }

    // ------------------------------------------------------------ VAT / GST

    data class VatResult(val net: BigDecimal, val tax: BigDecimal, val gross: BigDecimal)

    /** [inclusive] = the amount already contains the tax (remove VAT); otherwise add VAT. */
    fun vat(amount: BigDecimal, ratePercent: BigDecimal, inclusive: Boolean): VatResult {
        return if (inclusive) {
            val net = div(amount.multiply(HUNDRED), HUNDRED.add(ratePercent))
            VatResult(clean(net), clean(amount.subtract(net)), clean(amount))
        } else {
            val tax = amount.multiply(ratePercent).divide(HUNDRED)
            VatResult(clean(amount), clean(tax), clean(amount.add(tax)))
        }
    }

    // ------------------------------------------------------------ loan / EMI

    data class EmiRow(
        val month: Int,
        val payment: BigDecimal,
        val principal: BigDecimal,
        val interest: BigDecimal,
        val balance: BigDecimal,
    )

    data class EmiResult(
        val emi: BigDecimal,
        val totalInterest: BigDecimal,
        val totalPayment: BigDecimal,
        val schedule: List<EmiRow>,
    )

    /**
     * EMI = P·r·(1+r)^n / ((1+r)^n − 1), r = monthly rate. Values are kept at full
     * precision internally; the schedule rounds each row to 2 decimals (currency) and
     * the last payment absorbs the rounding difference.
     */
    fun emi(principal: BigDecimal, annualRatePercent: BigDecimal, months: Int): EmiResult {
        if (months <= 0) throw CalcException("Tenure must be at least 1 month")
        if (months > 1200) throw CalcException("Tenure is too long (max 100 years)")
        if (principal.signum() <= 0) throw CalcException("Loan amount must be greater than 0")
        if (annualRatePercent.signum() < 0) throw CalcException("Interest rate cannot be negative")
        val r = annualRatePercent.divide(BigDecimal(1200), MC)
        val emi = if (r.signum() == 0) {
            principal.divide(BigDecimal(months), MC)
        } else {
            val factor = BigDecimal.ONE.add(r).pow(months, MC)
            principal.multiply(r).multiply(factor).divide(factor.subtract(BigDecimal.ONE), MC)
        }
        val emiRounded = emi.setScale(2, RoundingMode.HALF_UP)
        val rows = ArrayList<EmiRow>(months)
        var balance = principal
        var totalPaid = BigDecimal.ZERO
        for (m in 1..months) {
            val interest = balance.multiply(r).setScale(2, RoundingMode.HALF_UP)
            var payment = emiRounded
            var principalPart = payment.subtract(interest)
            if (m == months || principalPart > balance) {
                principalPart = balance
                payment = balance.add(interest)
            }
            balance = balance.subtract(principalPart)
            totalPaid = totalPaid.add(payment)
            rows += EmiRow(m, payment, principalPart, interest, balance.max(BigDecimal.ZERO))
        }
        val totalInterest = totalPaid.subtract(principal)
        return EmiResult(clean(emi.round(MathContext(30))), clean(totalInterest), clean(totalPaid), rows)
    }

    // ------------------------------------------------------------ simple / compound interest

    fun simpleInterest(principal: BigDecimal, ratePercent: BigDecimal, years: BigDecimal): BigDecimal =
        clean(principal.multiply(ratePercent).multiply(years).divide(HUNDRED))

    fun compoundAmount(principal: BigDecimal, ratePercent: BigDecimal, years: Int, timesPerYear: Int = 1): BigDecimal {
        val rate = BigDecimal.ONE.add(ratePercent.divide(HUNDRED.multiply(BigDecimal(timesPerYear)), MC))
        return clean(principal.multiply(rate.pow(years * timesPerYear, MC)).round(MC))
    }
}
