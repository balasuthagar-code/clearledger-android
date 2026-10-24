package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Glue: the due items of [today]'s month, refreshed whenever a recurring item OR an expense of that
 * month changes (so deleting a confirmed expense makes the item PENDING again).
 */
class ObserveDueRecurringUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val expenseRepository: ExpenseRepository,
    private val getDue: GetDueRecurringUseCase
) {
    operator fun invoke(today: LocalDate): Flow<List<DueRecurring>> {
        val month = YearMonth.from(today)
        return combine(
            recurringRepository.observeRecurring(),
            expenseRepository.observeExpensesForMonth(month.toString())
        ) { items, expenses ->
            val linked: Map<String, String> = expenses
                .mapNotNull { expense -> expense.recurringId?.let { it to expense.id } }
                .toMap()
            getDue(items, month, linked)
        }
    }
}
