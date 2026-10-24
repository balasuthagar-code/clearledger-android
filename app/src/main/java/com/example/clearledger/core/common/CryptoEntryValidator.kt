package com.example.clearledger.core.common

import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

object CryptoEntryValidator {

    data class ValidatedCryptoEntry(
        val originalValueMinor: Long,
        val exchangeRate: String,
        val baseValueMinor: Long,
        val costBasisBaseMinor: Long,
        val monthKey: String
    )

    fun validate(
        assetName: String,
        date: String,
        type: String,
        originalValue: String,
        currency: String,
        rate: String,
        costBasisLkr: String
    ): String? {
        if (assetName.isBlank()) {
            return "Please enter the cryptocurrency name."
        }

        val parsedDate = try {
            LocalDate.parse(date)
        } catch (_: Exception) {
            return "Please enter a valid date."
        }

        if (parsedDate.isAfter(LocalDate.now())) {
            return "The date cannot be in the future."
        }

        if (type !in CryptoEntryType.values) {
            return "Please select a valid entry type."
        }

        if (currency !in listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")) {
            return "Please select a valid currency."
        }

        val amount = originalValue.toBigDecimalOrNull()
            ?: return "Please enter a valid amount."

        if (amount <= BigDecimal.ZERO) {
            return "The amount must be greater than zero."
        }

        val exchangeRate = rate.toBigDecimalOrNull()
            ?: return "Please enter a valid exchange rate."

        if (exchangeRate <= BigDecimal.ZERO) {
            return "The exchange rate must be greater than zero."
        }

        if (currency == "LKR" &&
            exchangeRate.compareTo(BigDecimal.ONE) != 0
        ) {
            return "For LKR, the exchange rate must be 1."
        }

        if (type == CryptoEntryType.SELL ||
            type == CryptoEntryType.RESULT
        ) {
            val costBasis = costBasisLkr.toBigDecimalOrNull()
                ?: return "Please enter the cost basis in LKR."

            if (costBasis < BigDecimal.ZERO) {
                return "The cost basis cannot be negative."
            }
        }

        return null
    }

    fun convert(
        date: String,
        type: String,
        originalValue: String,
        currency: String,
        rate: String,
        costBasisLkr: String
    ): ValidatedCryptoEntry? {
        return try {
            val amount = originalValue.toBigDecimalOrNull()
                ?: return null

            val exchangeRate = rate.toBigDecimalOrNull()
                ?: return null

            if (amount <= BigDecimal.ZERO ||
                exchangeRate <= BigDecimal.ZERO
            ) {
                return null
            }

            val originalMinor = amount
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            val baseAmount = if (currency == "LKR") {
                amount
            } else {
                amount.multiply(exchangeRate)
            }

            val baseMinor = baseAmount
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            val costBasisMinor = if (
                type == CryptoEntryType.SELL ||
                type == CryptoEntryType.RESULT
            ) {
                val costBasis = costBasisLkr.toBigDecimalOrNull()
                    ?: return null

                if (costBasis < BigDecimal.ZERO) return null

                costBasis
                    .multiply(BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            } else {
                0L
            }

            ValidatedCryptoEntry(
                originalValueMinor = originalMinor,
                exchangeRate = exchangeRate
                    .stripTrailingZeros()
                    .toPlainString(),
                baseValueMinor = baseMinor,
                costBasisBaseMinor = costBasisMinor,
                monthKey = YearMonth.parse(date.substring(0, 7)).toString()
            )
        } catch (_: Exception) {
            null
        }
    }
}
// thisanth