package com.example.clearledger.core.common

import java.math.BigDecimal

enum class CurrencyFailure {
    UNSUPPORTED_CURRENCY, INVALID_AMOUNT, EXCESS_AMOUNT_PRECISION, AMOUNT_OVERFLOW,
    INVALID_RATE, NON_POSITIVE_RATE, CONVERSION_OVERFLOW, POSITIVE_AMOUNT_ROUNDS_TO_ZERO
}

/** Inspect [failure] rather than matching human-readable exception messages. */
class CurrencyException(val failure: CurrencyFailure) : IllegalArgumentException(failure.name)

/** Rate direction: base major units per one original major unit. No persistence I/O. */
data class CurrencyConversion(
    val originalCurrency: String,
    val baseCurrency: String,
    val originalAmountMinor: Long,
    val appliedRate: BigDecimal,
    val baseAmountMinor: Long
) {
    val appliedRatePlain: String get() = appliedRate.stripTrailingZeros().toPlainString()
}

/**
 * Phase 1 API. All catalogue currencies currently have scale 2.
 * New text APIs accept trimmed, signed plain decimal amounts and signed plain rates (which must be positive),
 * at most 64 characters after trimming. No grouping or scientific notation.
 * Amounts may be zero/negative; feature validators decide their business meaning.
 * Same-currency conversion always applies 1 and ignores the supplied quote (including null).
 * Foreign pairs require a positive quote. A positive conversion rounding to zero is a
 * distinct failure, so features can ask for a larger amount without claiming overflow.
 */
object CurrencyCore {
    private const val MAX_TEXT_LENGTH = 64
    private val amountSyntax = Regex("^[+-]?[0-9]+(\\.[0-9]+)?$")
    private val rateSyntax = Regex("^[+-]?[0-9]+(\\.[0-9]+)?$")

    fun parseAmount(text: String, currency: String): Result<Long> {
        if (currency !in Currencies.supported) return error(CurrencyFailure.UNSUPPORTED_CURRENCY)
        val value = text.trim()
        if (value.length > MAX_TEXT_LENGTH || !amountSyntax.matches(value)) {
            return error(CurrencyFailure.INVALID_AMOUNT)
        }
        val amount = BigDecimal(value)
        if (amount.scale() > 2) return error(CurrencyFailure.EXCESS_AMOUNT_PRECISION)
        return try {
            Result.Success(MoneyArithmetic.toMinor(amount, 2))
        } catch (_: ArithmeticException) {
            error(CurrencyFailure.AMOUNT_OVERFLOW)
        }
    }

    fun parseRate(text: String): Result<BigDecimal> {
        val value = text.trim()
        if (value.length > MAX_TEXT_LENGTH || !rateSyntax.matches(value)) {
            return error(CurrencyFailure.INVALID_RATE)
        }
        val rate = BigDecimal(value)
        return if (rate.signum() <= 0) error(CurrencyFailure.NON_POSITIVE_RATE)
        else Result.Success(rate)
    }

    fun convert(
        originalAmountMinor: Long,
        originalCurrency: String,
        baseCurrency: String,
        exchangeRate: BigDecimal? = null
    ): Result<CurrencyConversion> {
        if (originalCurrency !in Currencies.supported || baseCurrency !in Currencies.supported) {
            return error(CurrencyFailure.UNSUPPORTED_CURRENCY)
        }
        val rate = if (originalCurrency == baseCurrency) BigDecimal.ONE
        else exchangeRate ?: return error(CurrencyFailure.INVALID_RATE)
        if (rate.signum() <= 0) return error(CurrencyFailure.NON_POSITIVE_RATE)
        val converted = try {
            MoneyArithmetic.convertMinor(originalAmountMinor, rate)
        } catch (_: ArithmeticException) {
            return error(CurrencyFailure.CONVERSION_OVERFLOW)
        }
        if (originalAmountMinor > 0 && converted == 0L) {
            return error(CurrencyFailure.POSITIVE_AMOUNT_ROUNDS_TO_ZERO)
        }
        return Result.Success(CurrencyConversion(
            originalCurrency, baseCurrency, originalAmountMinor, rate, converted
        ))
    }

    private fun error(failure: CurrencyFailure): Result.Error = Result.Error(CurrencyException(failure))
}
