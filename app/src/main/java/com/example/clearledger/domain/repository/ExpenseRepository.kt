package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    /** Real-time expenses for one month key ("YYYY-MM"). Used by the dashboard and the list. */
    fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>>

    /** One expense for the edit screen. Success(null) means "it does not exist (any more)". */
    suspend fun getExpense(id: String): Result<Expense?>

    /** Saves a new expense (the id inside [expense] is ignored). Returns the new document id. */
    suspend fun addExpense(expense: Expense): Result<String>

    /** Overwrites the editable fields of an existing expense (keeps its createdAt). */
    suspend fun updateExpense(expense: Expense): Result<Unit>

    suspend fun deleteExpense(id: String): Result<Unit>
}
