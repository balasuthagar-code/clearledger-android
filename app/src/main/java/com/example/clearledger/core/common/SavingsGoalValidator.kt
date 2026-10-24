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
        if (target == null || !target.isFinite() || target <= 0) {
            return "Target amount must be greater than zero."
        }

        if (currency !in supportedCurrencies) {
            return "Please select a valid currency."
        }

        val starting = startingAmount.toDoubleOrNull()
        if (starting == null || !starting.isFinite() ||
            starting < 0 || starting > target
        ) {
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