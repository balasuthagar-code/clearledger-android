package com.example.clearledger.domain.model

/**
 * FR-DASH-08 / BR-10 (all amounts are base-currency minor units):
 *   safeToSpend = income - expenses - pendingCommitments - goalSaving
 * It is an ESTIMATE built from recorded data only. A negative amount means the month is over budget.
 */
data class SafeToSpend(
    val incomeMinor: Long,
    val expensesMinor: Long,
    val pendingCommitmentsMinor: Long,
    val goalSavingMinor: Long
) {
    val amountMinor: Long
        get() = incomeMinor - expensesMinor - pendingCommitmentsMinor - goalSavingMinor

    val isOverBudget: Boolean get() = amountMinor < 0L
}
