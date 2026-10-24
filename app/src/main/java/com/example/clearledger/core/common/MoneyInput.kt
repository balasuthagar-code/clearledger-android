package com.example.clearledger.core.common

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Text <-> minor units, and the BR-2 currency conversion. Pure Kotlin, so it is unit tested
 * without Android. Never uses Double (0.1 + 0.2 != 0.3).
 *
 * If Member 2's multi-currency module already offers the same functions, delete this file and use
 * theirs - the SRS wants ONE conversion implementation for the whole app.
 */
object MoneyInput {

    // Up to 10 digits before the point and 2 after: max 9,999,999,999.99 (so minor units fit a Long).
    private val AMOUNT = Regex("^\\d{1,10}(\\.\\d{1,2})?$")
    // Exchange rate: up to 4 digits before the point, 6 after (e.g. 300.5 or 0.003333).
    private val RATE = Regex("^\\d{1,4}(\\.\\d{1,6})?$")

    /**
     * "1250.50" -> 125050.  "1,250.5" -> 125050.  Returns null when the text is not a valid amount
     * (empty, letters, negative, more than 2 decimals). "0" returns 0; the validator rejects it.
     */
    fun parseToMinor(text: String): Long? {
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (!AMOUNT.matches(cleaned)) return null
        return BigDecimal(cleaned).movePointRight(2).longValueExact()
    }

    /** 125050 -> "1250.50" (plain text for an input box, no thousands separator). */
    fun minorToInputText(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()

    /** Positive exchange rate or null. */
    fun parseRate(text: String): BigDecimal? {
        val cleaned = text.trim()
        if (!RATE.matches(cleaned)) return null
        val rate = BigDecimal(cleaned)
        return if (rate.signum() > 0) rate else null
    }

    /**
     * BR-2: baseAmountMinor = round_half_up(originalAmountMinor x exchangeRate).
     * Example: USD 100.00 (10000) at 300.50 -> 3,005,000 = LKR 30,050.00.
     */
    fun toBaseMinor(amountMinor: Long, rate: BigDecimal): Long? = try {
        BigDecimal.valueOf(amountMinor).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (e: ArithmeticException) {
        null
    }
}
