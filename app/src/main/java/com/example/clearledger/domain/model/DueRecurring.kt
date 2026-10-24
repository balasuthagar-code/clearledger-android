package com.example.clearledger.domain.model

import java.time.LocalDate

/** FR-REC-03: where a recurring item stands in the month being tracked. */
enum class DueStatus { PENDING, CONFIRMED, SKIPPED }

/**
 * One recurring item that falls due in a given month.
 *
 * @property dueDate   the due day of that month (a day 31 in a 30-day month becomes the 30th).
 * @property status    CONFIRMED when a linked expense exists, SKIPPED when the user skipped this month,
 *                     otherwise PENDING.
 * @property expenseId the linked expense when CONFIRMED.
 */
data class DueRecurring(
    val item: RecurringItem,
    val dueDate: LocalDate,
    val status: DueStatus,
    val expenseId: String? = null
) {
    /** "Overdue" is derived, never stored: still pending and the due date has passed. */
    fun isOverdue(today: LocalDate): Boolean = status == DueStatus.PENDING && dueDate.isBefore(today)
}
