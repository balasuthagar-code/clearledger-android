
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeSource

interface IncomeSourceRepository {
    fun loadSources(callback: (Result<List<IncomeSourceSummary>>) -> Unit)

    fun saveSource(source: IncomeSource, callback: (Result<Unit>) -> Unit)

    fun deleteSource(sourceId: String, callback: (Result<Unit>) -> Unit)

    fun setActive(sourceId: String, active: Boolean, callback: (Result<Unit>) -> Unit)
}
// thisanth