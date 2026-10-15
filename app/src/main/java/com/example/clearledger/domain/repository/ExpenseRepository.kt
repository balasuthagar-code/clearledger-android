package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    /** Real-time expenses for one month key ("YYYY-MM"). Sprint 2 adds add/edit/delete. */
    fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>>
}
