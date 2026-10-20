package com.example.clearledger.testutil

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** A real, in-memory ExpenseRepository for unit tests, with switches to simulate failures. */
open class InMemoryExpenseRepository(initial: List<Expense> = emptyList()) : ExpenseRepository {

    val data = MutableStateFlow(initial)
    val added = mutableListOf<Expense>()
    val updated = mutableListOf<Expense>()
    val deletedIds = mutableListOf<String>()
    var failWrites = false
    private var nextId = 1

    override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> =
        data.map { list -> list.filter { it.monthKey == monthKey } }

    override suspend fun getExpense(id: String): Result<Expense?> =
        Result.Success(data.value.firstOrNull { it.id == id })

    override suspend fun addExpense(expense: Expense): Result<String> {
        if (failWrites) return Result.Error(Exception("Could not save the expense. Please try again."))
        val saved = expense.copy(id = "new${nextId++}")
        added += saved
        data.update { it + saved }
        return Result.Success(saved.id)
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        if (failWrites) return Result.Error(Exception("Could not update the expense. Please try again."))
        updated += expense
        data.update { list -> list.map { if (it.id == expense.id) expense else it } }
        return Result.Success(Unit)
    }

    override suspend fun deleteExpense(id: String): Result<Unit> {
        if (failWrites) return Result.Error(Exception("Could not delete the expense. Please try again."))
        deletedIds += id
        data.update { list -> list.filterNot { it.id == id } }
        return Result.Success(Unit)
    }
}
