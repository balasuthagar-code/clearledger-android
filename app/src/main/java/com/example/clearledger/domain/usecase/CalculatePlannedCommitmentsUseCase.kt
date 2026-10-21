package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import javax.inject.Inject

/**
 * @property monthlyTotalBaseMinor FR-REC-05: total of the ACTIVE MONTHLY items, in the base currency.
 * @property activeMonthlyCount    how many items that total is made of.
 * @property otherActiveCount      active weekly / yearly items; listed but NOT in the monthly total,
 *                                 because the SRS defines the planned total over monthly items.
 */
data class PlannedCommitments(
    val monthlyTotalBaseMinor: Long,
    val activeMonthlyCount: Int,
    val otherActiveCount: Int
)

/**
 * FR-REC-05. A recurring definition is a PLAN: this total is NOT added to the expense totals.
 * An expense counts only when it is recorded as a real expense.
 */
class CalculatePlannedCommitmentsUseCase @Inject constructor() {

    operator fun invoke(items: List<RecurringItem>): PlannedCommitments {
        val active = items.filter { it.isActive }
        val monthly = active.filter { it.frequency == RecurringFrequency.MONTHLY }
        return PlannedCommitments(
            monthlyTotalBaseMinor = monthly.sumOf { it.baseAmountMinor },
            activeMonthlyCount = monthly.size,
            otherActiveCount = active.size - monthly.size
        )
    }
}
