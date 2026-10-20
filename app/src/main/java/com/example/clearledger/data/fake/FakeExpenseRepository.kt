package com.example.clearledger.data.fake

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory expenses. NO LONGER BOUND in ExpenseModule (Sprint 2 binds FirestoreExpenseRepository).
 * Kept only as a handy fake for @Preview or a quick offline experiment: to use it again, change one
 * line in ExpenseModule. Never ship it as the real implementation.
 */
@Singleton
class FakeExpenseRepository @Inject constructor() : ExpenseRepository {

    private val today = LocalDate.now()
    private val thisMonth = MonthKey.of(today)
    private var nextId = 100

    private val data = MutableStateFlow(
        listOf(
            sample("e1", 3_400_000, "rent", ExpenseClassification.COMMITTED, "Landlord"),
            sample("e2", 480_000, "subscriptions", ExpenseClassification.COMMITTED, "Gym"),
            sample("e3", 1_200_000, "food", ExpenseClassification.DISCRETIONARY, "Coffee & dining"),
            sample("e4", 350_000, "transport", ExpenseClassification.DISCRETIONARY, "PickMe")
        )
    )

    override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> =
        data.map { list -> list.filter { it.monthKey == monthKey } }

    override suspend fun getExpense(id: String): Result<Expense?> =
        Result.Success(data.value.firstOrNull { it.id == id })

    override suspend fun addExpense(expense: Expense): Result<String> {
        val id = "e${nextId++}"
        data.update { it + expense.copy(id = id) }
        return Result.Success(id)
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        data.update { list -> list.map { if (it.id == expense.id) expense else it } }
        return Result.Success(Unit)
    }

    override suspend fun deleteExpense(id: String): Result<Unit> {
        data.update { list -> list.filterNot { it.id == id } }
        return Result.Success(Unit)
    }

    private fun sample(
        id: String, minor: Long, category: String, c: ExpenseClassification, merchant: String
    ) = Expense(
        id = id, amountMinor = minor, currency = "LKR", exchangeRate = "1", baseAmountMinor = minor,
        categoryId = category, classification = c, paymentMethod = PaymentMethod.CARD,
        merchant = merchant, date = today, monthKey = thisMonth
    )
}
