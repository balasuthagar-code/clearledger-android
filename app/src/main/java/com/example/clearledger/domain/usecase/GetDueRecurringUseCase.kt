package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * FR-REC-03: which recurring items fall due in [month], and what is their status?
 *
 *  - Only ACTIVE items are tracked.
 *  - MONTHLY items are due every month; YEARLY items only in their dueMonth.
 *  - WEEKLY items are NOT tracked (they would fall due several times a month); they stay in the list
 *    and are described as plans only.
 *  - Status: CONFIRMED if a linked expense exists, else SKIPPED if the user skipped this month,
 *    else PENDING.
 *
 * Pure Kotlin - unit tested without Android.
 */
class GetDueRecurringUseCase @Inject constructor() {

    /** @param linkedExpenseIds recurringId -> id of the expense recorded for it in [month]. */
    operator fun invoke(
        items: List<RecurringItem>,
        month: YearMonth,
        linkedExpenseIds: Map<String, String>
    ): List<DueRecurring> =
        items
            .filter { it.isActive && isTrackedIn(it, month) }
            .map { item ->
                val expenseId = linkedExpenseIds[item.id]
                DueRecurring(
                    item = item,
                    dueDate = dueDateOf(item, month),
                    status = when {
                        expenseId != null -> DueStatus.CONFIRMED
                        item.skippedMonthKey == month.toString() -> DueStatus.SKIPPED
                        else -> DueStatus.PENDING
                    },
                    expenseId = expenseId
                )
            }
            .sortedWith(compareBy<DueRecurring> { it.dueDate }.thenBy { it.item.name.lowercase() })

    private fun isTrackedIn(item: RecurringItem, month: YearMonth): Boolean = when (item.frequency) {
        RecurringFrequency.MONTHLY -> true
        RecurringFrequency.YEARLY -> item.dueMonth == month.monthValue
        RecurringFrequency.WEEKLY -> false
    }

    private fun dueDateOf(item: RecurringItem, month: YearMonth): LocalDate =
        month.atDay(item.dueDay.coerceIn(1, month.lengthOfMonth()))
}
