
package com.example.clearledger.ui.goals

import com.example.clearledger.domain.model.SavingsContribution
import com.example.clearledger.domain.model.SavingsGoal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class SavingsContributionsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val contributions: List<SavingsContribution> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val totalSaved: Double
        get() = selectedGoal?.let { goal ->
            goal.startingAmount + contributions.sumOf { it.amount }
        } ?: 0.0

    val remainingAmount: Double
        get() = max(
            (selectedGoal?.targetAmount ?: 0.0) - totalSaved,
            0.0
        )

    val progress: Float
        get() {
            val target = selectedGoal?.targetAmount ?: 0.0
            return if (target > 0.0) {
                min(totalSaved / target, 1.0).toFloat()
            } else {
                0f
            }
        }

    val monthsLeft: Long
        get() {
            val deadline = selectedGoal?.deadline
                ?.let { value ->
                    runCatching {
                        LocalDate.parse(value)
                    }.getOrNull()
                } ?: return 1L

            val currentMonth = YearMonth.now()
            val deadlineMonth = YearMonth.from(deadline)

            return max(
                ChronoUnit.MONTHS.between(
                    currentMonth,
                    deadlineMonth
                ),
                1L
            )
        }

    val requiredMonthlySaving: Double
        get() {
            if (remainingAmount <= 0.0) return 0.0
            return ceil(remainingAmount / monthsLeft)
        }
}
