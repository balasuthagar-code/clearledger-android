
package com.example.clearledger.ui.income

import com.example.clearledger.domain.model.IncomeSource

data class IncomeSourcesUiState(
    val sources: List<IncomeSource> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
