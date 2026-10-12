
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeSource
import kotlinx.coroutines.flow.Flow

interface IncomeSourceRepository {

    fun getIncomeSources(): Flow<List<IncomeSource>>

    suspend fun addIncomeSource(source: IncomeSource)

    suspend fun updateIncomeSource(source: IncomeSource)
}
