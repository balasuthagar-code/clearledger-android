package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.model.SafeToSpend
import javax.inject.Inject

/**
 * BR-10 (FR-DASH-08). Pure Kotlin, no Android.
 *
 *   safeToSpend = totalIncome - totalExpenses - unpaidActiveRecurring - requiredMonthlyGoalSaving
 *
 * [pendingCommitmentsMinor] = recurring items due this month that are neither confirmed (already an
 * expense, so already inside totalExpenses) nor skipped.
 * [goalSavingMinor] = the primary goal's required monthly saving (0 when there is no goal).
 */
class CalculateSafeToSpendUseCase @Inject constructor() {

    operator fun invoke(
        summary: MonthlySummary,
        pendingCommitmentsMinor: Long,
        goalSavingMinor: Long
    ): SafeToSpend = SafeToSpend(
        incomeMinor = summary.totalIncomeMinor,
        expensesMinor = summary.totalExpensesMinor,
        pendingCommitmentsMinor = pendingCommitmentsMinor.coerceAtLeast(0L),
        goalSavingMinor = goalSavingMinor.coerceAtLeast(0L)
    )
}
