package com.example.clearledger.core.common

import java.math.BigDecimal

object FreelanceProjectValidator {

    data class ProjectValues(
        val agreedAmountMinor: Long,
        val exchangeRate: String,
        val baseAmountMinor: Long
    )

    fun validate(
        name: String,
        client: String,
        agreedAmount: String,
        currency: String,
        rate: String
    ): String? {
        if (name.isBlank()) {
            return "Please enter the project name."
        }

        if (client.isBlank()) {
            return "Please enter the client name."
        }
        val amountMinor = when (
            val result = CurrencyCore.parseAmount(agreedAmount, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error -> {
                return when (
                    (result.exception as? CurrencyException)?.failure
                ) {
                    CurrencyFailure.UNSUPPORTED_CURRENCY ->
                        "Please select a valid currency."

                    CurrencyFailure.EXCESS_AMOUNT_PRECISION ->
                        "Enter a project amount with no more than two decimal places."

                    CurrencyFailure.AMOUNT_OVERFLOW ->
                        "The project amount is too large."

                    else ->
                        "Please enter a valid project amount."
                }
            }
        }

        if (amountMinor <= 0L) {
            return "The project amount must be greater than zero."
        }

        val exchangeRate = when (val result = CurrencyCore.parseRate(rate)) {
            is Result.Success -> result.data
            is Result.Error ->
                return "Please enter an exchange rate greater than zero."
        }

        if (currency == "LKR" && exchangeRate.compareTo(BigDecimal.ONE) != 0) {
            return "For LKR projects, the exchange rate must be 1."
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
                    "The converted project amount is too large."

                CurrencyFailure.POSITIVE_AMOUNT_ROUNDS_TO_ZERO ->
                    "The converted project amount is too small."

                else ->
                    "Please check the project currency and exchange rate."
            }
        }
    }

    fun convert(
        agreedAmount: String,
        currency: String,
        rate: String
    ): ProjectValues? {
        val amountMinor = when (
            val result = CurrencyCore.parseAmount(agreedAmount, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }

        if (amountMinor <= 0L) return null

        val exchangeRate = when (val result = CurrencyCore.parseRate(rate)) {
            is Result.Success -> result.data
            is Result.Error -> return null
        }

        if (currency == "LKR" && exchangeRate.compareTo(BigDecimal.ONE) != 0) {
            return null
        }

        return when (
            val result = CurrencyCore.convert(
                originalAmountMinor = amountMinor,
                originalCurrency = currency,
                baseCurrency = "LKR",
                exchangeRate = exchangeRate
            )
        ) {
            is Result.Success -> ProjectValues(
                agreedAmountMinor = result.data.originalAmountMinor,
                exchangeRate = result.data.appliedRatePlain,
                baseAmountMinor = result.data.baseAmountMinor
            )

            is Result.Error -> null
        }
    }

    fun validateMilestone(
        title: String,
        amount: String,
        dueDate: String
    ): String? {
        if (title.isBlank()) {
            return "Please enter the milestone title."
        }

        val milestoneAmount = amount.toBigDecimalOrNull()
            ?: return "Please enter a valid milestone amount."

        if (milestoneAmount <= BigDecimal.ZERO) {
            return "The milestone amount must be greater than zero."
        }

        if (dueDate.isBlank()) {
            return "Please select a due date."
        }

        return null
    }
}
// thisanth