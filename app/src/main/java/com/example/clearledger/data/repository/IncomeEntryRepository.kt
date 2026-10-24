
package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource

interface IncomeEntryRepository {

    fun loadSources(
        callback: (Result<List<IncomeSource>>) -> Unit
    )

    fun loadEntries(
        callback: (Result<List<IncomeEntry>>) -> Unit
    )

    fun saveEntry(
        entry: IncomeEntry,
        callback: (Result<Unit>) -> Unit
    )

    fun deleteEntry(
        entryId: String,
        callback: (Result<Unit>) -> Unit
    )
}
// thisanth