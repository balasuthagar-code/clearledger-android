package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.IncomeSummarySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Glue: re-calculates the summary whenever expenses OR income change (real-time dashboard). */
class ObserveMonthlySummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeSource: IncomeSummarySource,
    private val calculate: CalculateMonthlySummaryUseCase
) {
    operator fun invoke(monthKey: String): Flow<MonthlySummary> =
        combine(
            expenseRepository.observeExpensesForMonth(monthKey),
            incomeSource.observeReceivedBaseAmounts(monthKey)
        ) { expenses, income -> calculate(monthKey, income, expenses) }
}
