package com.example.clearledger.ui.income

import com.example.clearledger.domain.model.IncomeSource

data class IncomeSourcesUiState(
    val incomeSources: List<IncomeSource> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)