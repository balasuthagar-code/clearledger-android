package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RecurringItem
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    /** Real-time list of ALL the user's recurring items (active and inactive), ordered by name. */
    fun observeRecurring(): Flow<List<RecurringItem>>

    /** Success(null) means "it does not exist (any more)". */
    suspend fun getRecurring(id: String): Result<RecurringItem?>

    /** Saves a new item (the id inside [item] is ignored). Returns the new document id. */
    suspend fun addRecurring(item: RecurringItem): Result<String>

    suspend fun updateRecurring(item: RecurringItem): Result<Unit>

    /** FR-REC-02: activate / deactivate without opening the edit form. */
    suspend fun setActive(id: String, active: Boolean): Result<Unit>

    suspend fun deleteRecurring(id: String): Result<Unit>
}
