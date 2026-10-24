package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.CategoryTotal
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.MonthlySummary
import javax.inject.Inject

/**
 * Pure Kotlin business rule (SRS BR-6 / BR-7). No Android, no Firebase, no coroutines - so it is
 * trivially unit-testable and keeps business logic out of Composables.
 *
 *   totalIncome   = sum(received income, base currency)
 *   totalExpenses = sum(expense.baseAmountMinor) for the month
 *   committed + discretionary = totalExpenses
 *   remaining     = totalIncome - totalExpenses
 */
class CalculateMonthlySummaryUseCase @Inject constructor() {

    operator fun invoke(
        monthKey: String,
        receivedIncomeBaseMinor: List<Long>,
        expenses: List<Expense>
    ): MonthlySummary {
        val monthExpenses = expenses.filter { it.monthKey == monthKey }

        val totalIncome = receivedIncomeBaseMinor.sum()
        val totalExpenses = monthExpenses.sumOf { it.baseAmountMinor }
        val committed = monthExpenses
            .filter { it.classification == ExpenseClassification.COMMITTED }
            .sumOf { it.baseAmountMinor }
        val discretionary = monthExpenses
            .filter { it.classification == ExpenseClassification.DISCRETIONARY }
            .sumOf { it.baseAmountMinor }

        val categoryTotals = monthExpenses
            .groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val total = items.sumOf { it.baseAmountMinor }
                CategoryTotal(
                    categoryId = categoryId,
                    totalMinor = total,
                    percentage = if (totalExpenses == 0L) 0.0 else total * 100.0 / totalExpenses
                )
            }
            .sortedByDescending { it.totalMinor }

        return MonthlySummary(
            monthKey = monthKey,
            totalIncomeMinor = totalIncome,
            totalExpensesMinor = totalExpenses,
            committedMinor = committed,
            discretionaryMinor = discretionary,
            remainingMinor = totalIncome - totalExpenses,
            categoryTotals = categoryTotals
        )
    }
}
