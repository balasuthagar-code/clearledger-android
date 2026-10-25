package com.example.clearledger.core.common

import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
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

        if (currency !in Currencies.supported) {
            return "Please select a valid currency."
        }

        val amount = originalValue.toBigDecimalOrNull()
            ?: return "Please enter a valid amount."

        val amountMinor = when (
            val result = CurrencyCore.parseAmount(originalValue, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error ->
                return "Enter a valid amount with no more than two decimal places and within the supported range."
        }

        if (amountMinor <= 0L) {
            return "The amount must be greater than zero."
        }

        val exchangeRate = when (val result = CurrencyCore.parseRate(rate)) {
            is Result.Success -> result.data
            is Result.Error ->
                return "Enter a valid exchange rate greater than zero."
        }

        if (currency == "LKR" &&
            exchangeRate.compareTo(BigDecimal.ONE) != 0
        ) {
            return "For LKR, the exchange rate must be 1."
        }

        if (type == CryptoEntryType.SELL ||
            type == CryptoEntryType.RESULT
        ) {
            val costBasisMinor = when (
                val result = CurrencyCore.parseAmount(costBasisLkr, "LKR")
            ) {
                is Result.Success -> result.data
                is Result.Error ->
                    return "Enter a valid cost basis in LKR with no more than two decimal places and within the supported range."
            }

            if (costBasisMinor < 0L) {
                return "The cost basis cannot be negative."
            }
        }

        return when (
            val result = CurrencyCore.convert(
                originalAmountMinor = amountMinor,
                originalCurrency = currency,
                baseCurrency = "LKR",
                exchangeRate = exchangeRate
            )
        ) {
            is Result.Success -> null
            is Result.Error -> when (
                (result.exception as? CurrencyException)?.failure
            ) {
                CurrencyFailure.CONVERSION_OVERFLOW ->
                    "The converted amount is too large."

                CurrencyFailure.POSITIVE_AMOUNT_ROUNDS_TO_ZERO ->
                    "The converted amount is too small."

                else ->
                    "Please check the currency and exchange rate."
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
        val amountMinor = when (
            val result = CurrencyCore.parseAmount(originalValue, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }

        if (amountMinor <= 0L) return null

        val exchangeRate = when (val result = CurrencyCore.parseRate(rate)) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }

        if (currency == "LKR" &&
            exchangeRate.compareTo(BigDecimal.ONE) != 0
        ) {
            return null
        }

        val conversion = when (
            val result = CurrencyCore.convert(
                originalAmountMinor = amountMinor,
                originalCurrency = currency,
                baseCurrency = "LKR",
                exchangeRate = exchangeRate
            )
        ) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }

        val costBasisMinor = if (
            type == CryptoEntryType.SELL ||
            type == CryptoEntryType.RESULT
        ) {
            when (
                val result = CurrencyCore.parseAmount(costBasisLkr, "LKR")
            ) {
                is Result.Success -> {
                    if (result.data < 0L) return null
                    result.data
                }

                is Result.Error -> return null
            }
        } else {
            0L
        }

        val monthKey = try {
            YearMonth.from(LocalDate.parse(date)).toString()
        } catch (_: Exception) {
            return null
        }

        return ValidatedCryptoEntry(
            originalValueMinor = conversion.originalAmountMinor,
            exchangeRate = conversion.appliedRatePlain,
            baseValueMinor = conversion.baseAmountMinor,
            costBasisBaseMinor = costBasisMinor,
            monthKey = monthKey
        )
    }
}
// thisanth