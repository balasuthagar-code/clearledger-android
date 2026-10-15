package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** Real-time list of the signed-in user's categories, ordered by sortOrder. */
    fun observeCategories(): Flow<List<ExpenseCategory>>

    /** Creates the 7 default categories if the user has none yet. Safe to call repeatedly. */
    suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit>
}
