package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.Expense
import java.time.LocalDate
import javax.inject.Inject

/** One day of the expense list: its expenses (newest first) and the day's total in base currency. */
data class ExpenseDayGroup(
    val date: LocalDate,
    val expenses: List<Expense>,
    val totalBaseMinor: Long
)

/**
 * FR-EXP-06: the expense list is grouped by date (newest day first) and can be filtered by category.
 * The month filter is applied earlier, by the repository query (monthKey).
 * Day totals use baseAmountMinor, like every other total in the app.
 */
class GroupExpensesByDateUseCase @Inject constructor() {

    operator fun invoke(expenses: List<Expense>, categoryId: String? = null): List<ExpenseDayGroup> =
        expenses
            .filter { categoryId == null || it.categoryId == categoryId }
            .groupBy { it.date }
            .map { (date, items) ->
                ExpenseDayGroup(
                    date = date,
                    expenses = items.sortedByDescending { it.createdAtMillis },
                    totalBaseMinor = items.sumOf { it.baseAmountMinor }
                )
            }
            .sortedByDescending { it.date }
}
