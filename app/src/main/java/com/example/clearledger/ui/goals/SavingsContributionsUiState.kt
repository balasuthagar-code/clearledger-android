
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
    // Total saved = starting amount + all contributions.
    // Amounts are stored in minor units (e.g. cents).
    val totalSavedMinor: Long
        get() = selectedGoal?.let { goal ->
            goal.startingSavedMinor +
                    contributions.sumOf { it.amountMinor }
        } ?: 0L

    // Remaining amount needed to reach the goal.
    val remainingAmountMinor: Long
        get() = max(
            (selectedGoal?.targetAmountMinor ?: 0L) - totalSavedMinor,
            0L
        )

    // Progress from 0.0 to 1.0.
    val progress: Float
        get() {
            val target = selectedGoal?.targetAmountMinor ?: 0L

            return if (target > 0L) {
                min(
                    totalSavedMinor.toDouble() / target.toDouble(),
                    1.0
                ).toFloat()
            } else {
                0f
            }
        }

    // Number of months remaining until the deadline.
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

    // Required monthly saving, in minor units.
    val requiredMonthlySavingMinor: Long
        get() {
            if (remainingAmountMinor <= 0L) return 0L

            return ceil(
                remainingAmountMinor.toDouble() / monthsLeft.toDouble()
            ).toLong()
        }
}
//thisanth