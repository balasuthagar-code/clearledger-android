
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeEntry
import kotlinx.coroutines.flow.Flow

interface IncomeEntryRepository {

    fun getIncomeEntries(): Flow<List<IncomeEntry>>

    suspend fun addIncomeEntry(entry: IncomeEntry)

    suspend fun updateIncomeEntry(entry: IncomeEntry)

    suspend fun deleteIncomeEntry(entryId: String)
}
