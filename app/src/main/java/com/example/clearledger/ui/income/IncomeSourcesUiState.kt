package com.example.clearledger.ui.income

import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource

data class IncomeEntriesUiState(
    val entries: List<IncomeEntry> = emptyList(),
    val sources: List<IncomeSource> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val selectedMonth: String = "",
    val selectedSourceId: String = ""
)
