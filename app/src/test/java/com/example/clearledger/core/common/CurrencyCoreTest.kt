package com.example.clearledger.core.common

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class CurrencyCoreTest {
    private fun <T> value(result: Result<T>): T = (result as Result.Success<T>).data
    private fun failure(expected: CurrencyFailure, result: Result<*>) {
        assertEquals(expected, ((result as Result.Error).exception as CurrencyException).failure)
    }

    @Test fun usdToLkr() {
        val amount = value(CurrencyCore.parseAmount("100.00", "USD"))
        val quote = value(CurrencyCore.parseRate("300.50"))
        val conversion = value(CurrencyCore.convert(amount, "USD", "LKR", quote))
        assertEquals(3_005_000L, conversion.baseAmountMinor)
        assertEquals("USD", conversion.originalCurrency)
        assertEquals("LKR", conversion.baseCurrency)
        assertEquals(10_000L, conversion.originalAmountMinor)
        assertEquals(quote, conversion.appliedRate)
        assertEquals("300.5", conversion.appliedRatePlain)
    }

    @Test fun nonLkrBaseAndSameCurrency() {
        assertEquals(8_000L, value(CurrencyCore.convert(10_000, "EUR", "GBP", BigDecimal("0.8"))).baseAmountMinor)
        for (currency in Currencies.supported) {
            for (quote in listOf(null, BigDecimal("123"), BigDecimal.ZERO, BigDecimal("-1"))) {
                val converted = value(CurrencyCore.convert(123, currency, currency, quote))
                assertEquals(BigDecimal.ONE, converted.appliedRate)
                assertEquals("1", converted.appliedRatePlain)
                assertEquals(123L, converted.baseAmountMinor)
            }
        }
    }

    @Test fun halfUpRoundsOnce() {
        for ((amount, rate, expected) in listOf(
            Triple(1L, "0.5", 1L), Triple(3L, "0.5", 2L),
            Triple(-1L, "0.5", -1L), Triple(100L, "0.01499", 1L),
            Triple(100L, "0.015", 2L)
        )) assertEquals(expected, value(CurrencyCore.convert(amount, "USD", "INR", BigDecimal(rate))).baseAmountMinor)
    }

    @Test fun unsupportedCodesAreNotNormalized() {
        for (code in listOf("JPY", "usd", " USD", "")) {
            failure(CurrencyFailure.UNSUPPORTED_CURRENCY, CurrencyCore.parseAmount("1", code))
            failure(CurrencyFailure.UNSUPPORTED_CURRENCY, CurrencyCore.convert(1, code, "USD"))
            failure(CurrencyFailure.UNSUPPORTED_CURRENCY, CurrencyCore.convert(1, "USD", code))
        }
    }

    @Test fun invalidAndNonPositiveRates() {
        for (text in listOf("", "NaN", "1e2", "1,000", ".5", "1.", "1 2", "1".repeat(65))) {
            failure(CurrencyFailure.INVALID_RATE, CurrencyCore.parseRate(text))
        }
        for (text in listOf("0", "0.000", "-1")) {
            failure(CurrencyFailure.NON_POSITIVE_RATE, CurrencyCore.parseRate(text))
            failure(CurrencyFailure.NON_POSITIVE_RATE, CurrencyCore.convert(1, "AUD", "USD", BigDecimal(text)))
        }
        failure(CurrencyFailure.INVALID_RATE, CurrencyCore.convert(1, "AUD", "USD"))
        assertEquals(BigDecimal("1.25"), value(CurrencyCore.parseRate(" +1.25 ")))
        assertEquals(BigDecimal("0.1234567"), value(CurrencyCore.parseRate("0.1234567")))
    }

    @Test fun amountSyntaxAndPrecision() {
        for (text in listOf("", "1,000", "1 000", "1e2", ".5", "1.", "--1", "1".repeat(65))) {
            failure(CurrencyFailure.INVALID_AMOUNT, CurrencyCore.parseAmount(text, "USD"))
        }
        for (text in listOf("1.001", "1.000", "-0.001")) {
            failure(CurrencyFailure.EXCESS_AMOUNT_PRECISION, CurrencyCore.parseAmount(text, "USD"))
        }
        assertEquals(0L, value(CurrencyCore.parseAmount("0", "USD")))
        assertEquals(-125L, value(CurrencyCore.parseAmount(" -1.25 ", "USD")))
        assertEquals(125L, value(CurrencyCore.parseAmount("+1.25", "USD")))
    }

    @Test fun overflowIsDistinctFromPositiveRoundedZero() {
        for (text in listOf("92233720368547758.08", "-92233720368547758.09")) {
            failure(CurrencyFailure.AMOUNT_OVERFLOW, CurrencyCore.parseAmount(text, "USD"))
        }
        for (amount in listOf(Long.MIN_VALUE, Long.MAX_VALUE)) {
            failure(CurrencyFailure.CONVERSION_OVERFLOW, CurrencyCore.convert(amount, "USD", "EUR", BigDecimal("2")))
            assertEquals(amount, value(CurrencyCore.convert(amount, "USD", "EUR", BigDecimal.ONE)).baseAmountMinor)
        }
        failure(CurrencyFailure.POSITIVE_AMOUNT_ROUNDS_TO_ZERO, CurrencyCore.convert(1, "USD", "EUR", BigDecimal("0.4999")))
        assertEquals(0L, value(CurrencyCore.convert(0, "USD", "EUR", BigDecimal("0.1"))).baseAmountMinor)
        assertEquals(0L, value(CurrencyCore.convert(-1, "USD", "EUR", BigDecimal("0.1"))).baseAmountMinor)
    }

    @Test fun fullLongRangeFormattingRoundTrips() {
        val amounts = listOf(Long.MIN_VALUE, Long.MAX_VALUE, -123456789L, -1L, 0L, 1L, 123456789L)
        for (minor in amounts) {
            assertEquals(minor, value(CurrencyCore.parseAmount(MoneyInput.minorToInputText(minor), "LKR")))
            for (scale in 0..4) {
                assertEquals(minor, MoneyUtils.majorToMinorLong(MoneyUtils.minorToMajorString(minor, scale), scale))
            }
        }
    }
}
