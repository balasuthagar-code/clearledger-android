
package com.example.clearledger.core.common

import java.time.LocalDate
import java.time.YearMonth

object IncomeGoalValidator {

    fun isValidSourceName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }

    fun isValidIncome(
        amount: Double,
        currency: String,
        exchangeRate: Double?
    ): Boolean {
        if (!amount.isFinite() || amount <= 0.0) {
            return false
        }

        if (currency.isBlank()) {
            return false
        }

        // Base currency is LKR.
        if (currency.equals("LKR", ignoreCase = true)) {
            return true
        }

        return exchangeRate != null &&
                exchangeRate.isFinite() &&
                exchangeRate > 0.0
    }

    fun monthKey(date: LocalDate): String {
        return YearMonth.from(date).toString()
    }

    fun isValidGoal(
        name: String,
        targetAmount: Double,
        deadline: LocalDate,
        startingAmount: Double,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        return name.trim().isNotEmpty() &&
                targetAmount.isFinite() &&
                targetAmount > 0.0 &&
                deadline.isAfter(today) &&
                startingAmount.isFinite() &&
                startingAmount >= 0.0 &&
                startingAmount <= targetAmount
    }
}
