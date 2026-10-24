package com.example.clearledger.ui.income

import com.example.clearledger.data.repository.IncomeSourceSummary
import com.example.clearledger.domain.model.IncomeSource

data class IncomeSourcesUiState(
    val summaries: List<IncomeSourceSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)