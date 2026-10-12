package com.example.clearledger.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

object IncomeEntryValidator {

    val supportedCurrencies = listOf(
        "LKR", "USD", "EUR", "GBP", "AUD", "INR"
    )

    val statuses = listOf("EXPECTED", "RECEIVED")

    fun parseAmountToMinorUnits(value: String): Long? {
        return try {
            val amount = BigDecimal(value.trim())

            if (amount <= BigDecimal.ZERO) {
                null
            } else {
                amount.multiply(BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun calculateBaseAmount(
        originalAmountMinor: Long,
        exchangeRate: String
    ): Long? {
        return try {
            val rate = BigDecimal(exchangeRate.trim())

            if (originalAmountMinor <= 0L || rate <= BigDecimal.ZERO) {
                null
            } else {
                BigDecimal.valueOf(originalAmountMinor)
                    .multiply(rate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isValidDate(value: String): Boolean {
        return try {
            LocalDate.parse(value.trim())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun monthKey(date: String): String {
        return YearMonth.from(LocalDate.parse(date)).toString()
    }

    fun formatAmount(minorUnits: Long): String {
        return BigDecimal.valueOf(minorUnits, 2).toPlainString()
    }
}