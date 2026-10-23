
package com.example.clearledger.core.common

import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeEntryOptions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object IncomeEntryValidator {

    fun validate(entry: IncomeEntry): String? {
        if (entry.sourceId.isBlank()) {
            return "Please select an income source."
        }

        if (entry.originalAmountMinor <= 0L) {
            return "Amount must be greater than zero."
        }

        if (entry.originalCurrency !in IncomeEntryOptions.currencies) {
            return "Please select a valid currency."
        }

        if (entry.status !in IncomeEntryOptions.statuses) {
            return "Please select a valid income status."
        }

        val parsedDate = try {
            LocalDate.parse(entry.date)
        } catch (_: Exception) {
            null
        }

        if (parsedDate == null) {
            return "Enter a valid date using YYYY-MM-DD."
        }

        val rate = entry.exchangeRate.toBigDecimalOrNull()
        if (rate == null || rate <= BigDecimal.ZERO) {
            return "Exchange rate must be greater than zero."
        }

        if (entry.originalCurrency == "LKR" && rate.compareTo(BigDecimal.ONE) != 0) {
            return "The exchange rate for LKR must be 1."
        }

        return null
    }

    fun calculateBaseAmountMinor(
        originalAmountMinor: Long,
        exchangeRate: String
    ): Long {
        val rate = exchangeRate.toBigDecimal()
        return BigDecimal.valueOf(originalAmountMinor)
            .multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun monthKey(date: String): String {
        return LocalDate.parse(date).toString().substring(0, 7)
    }
}
