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

        val amount = agreedAmount.toBigDecimalOrNull()
            ?: return "Please enter a valid project amount."

        if (amount <= BigDecimal.ZERO) {
            return "The project amount must be greater than zero."
        }

        if (currency !in listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")) {
            return "Please select a valid currency."
        }

        val exchangeRate = rate.toBigDecimalOrNull()
            ?: return "Please enter a valid exchange rate."

        if (exchangeRate <= BigDecimal.ZERO) {
            return "The exchange rate must be greater than zero."
        }

        if (currency == "LKR" && exchangeRate.compareTo(BigDecimal.ONE) != 0) {
            return "For LKR projects, the exchange rate must be 1."
        }

        return null
    }

    fun convert(
        agreedAmount: String,
        currency: String,
        rate: String
    ): ProjectValues? {
        val amount = agreedAmount.toBigDecimalOrNull() ?: return null
        val exchangeRate = rate.toBigDecimalOrNull() ?: return null

        if (amount <= BigDecimal.ZERO || exchangeRate <= BigDecimal.ZERO) {
            return null
        }

        return try {
            val originalMinor = amount
                .multiply(BigDecimal(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()

            val baseAmount = if (currency == "LKR") {
                amount
            } else {
                amount.multiply(exchangeRate)
            }

            val baseMinor = baseAmount
                .multiply(BigDecimal(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()

            ProjectValues(
                agreedAmountMinor = originalMinor,
                exchangeRate = exchangeRate.stripTrailingZeros().toPlainString(),
                baseAmountMinor = baseMinor
            )
        } catch (_: ArithmeticException) {
            null
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