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

    /** FR-EXP-08: the user's most recent expenses (newest first, at most [limit]) - the source of merchant memory. */
    fun observeRecentExpenses(limit: Int): Flow<List<Expense>>

    /**
     * Creates or overwrites the expense with the id GIVEN in [expense]. Used by recurring confirmation
     * (deterministic id) so confirming twice can never create a duplicate.
     */
    suspend fun upsertExpense(expense: Expense): Result<Unit>
}
