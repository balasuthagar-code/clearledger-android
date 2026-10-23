
package com.example.clearledger.domain.model

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class SavingsGoalProgress(
    val totalSaved: Double,
    val remainingAmount: Double,
    val progressPercentage: Double,
    val requiredMonthlySaving: Double,
    val monthsLeft: Long
)

object SavingsGoalProgressCalculator {

    fun calculate(
        targetAmount: Double,
        startingAmount: Double,
        contributions: List<Double>,
        deadline: LocalDate,
        today: LocalDate = LocalDate.now()
    ): SavingsGoalProgress {

        val totalSaved = startingAmount + contributions.sum()

        val remainingAmount = max(
            targetAmount - totalSaved,
            0.0
        )

        val progressPercentage = if (targetAmount > 0.0) {
            min((totalSaved / targetAmount) * 100.0, 100.0)
        } else {
            0.0
        }

        val currentMonth = YearMonth.from(today)
        val deadlineMonth = YearMonth.from(deadline)

        val monthsLeft = max(
            1L,
            java.time.temporal.ChronoUnit.MONTHS.between(
                currentMonth,
                deadlineMonth
            )
        )

        val requiredMonthlySaving = if (remainingAmount > 0.0) {
            ceil(remainingAmount / monthsLeft)
        } else {
            0.0
        }

        return SavingsGoalProgress(
            totalSaved = totalSaved,
            remainingAmount = remainingAmount,
            progressPercentage = progressPercentage,
            requiredMonthlySaving = requiredMonthlySaving,
            monthsLeft = monthsLeft
        )
    }
}
