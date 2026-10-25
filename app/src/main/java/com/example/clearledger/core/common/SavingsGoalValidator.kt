package com.example.clearledger.core.common

import java.time.LocalDate



object SavingsGoalValidator {

    val supportedCurrencies =
        listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")

    fun validate(
        name: String,
        targetAmount: String,
        currency: String,
        deadline: String,
        startingAmount: String
    ): String? {
        if (name.isBlank()) {
            return "Please enter a goal name."
        }

        val target = targetAmount.toDoubleOrNull()
        if (currency !in supportedCurrencies) {
            return "Please select a valid currency."
        }

        val targetMinor = when (
            val result = CurrencyCore.parseAmount(targetAmount, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error ->
                return "Enter a valid target amount with no more than two decimal places and within the supported range."
        }

        if (targetMinor <= 0L) {
            return "Target amount must be greater than zero."
        }

        val startingMinor = when (
            val result = CurrencyCore.parseAmount(startingAmount, currency)
        ) {
            is Result.Success -> result.data
            is Result.Error ->
                return "Enter a valid starting amount with no more than two decimal places and within the supported range."
        }

        if (startingMinor < 0L || startingMinor > targetMinor) {
            return "Starting amount must be between zero and the target."
        }

        val validDeadline = try {
            LocalDate.parse(deadline).isAfter(LocalDate.now())
        } catch (_: Exception) {
            false
        }

        if (!validDeadline) {
            return "Enter a future deadline as YYYY-MM-DD."
        }

        return null
    }
}