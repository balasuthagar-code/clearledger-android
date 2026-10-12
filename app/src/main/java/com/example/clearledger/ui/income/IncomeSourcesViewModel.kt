
package com.example.clearledger.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.data.repository.IncomeSourceRepository
import com.example.clearledger.domain.model.IncomeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncomeSourcesViewModel @Inject constructor(
    private val repository: IncomeSourceRepository
) : ViewModel() {
    // Keep the existing uiState, observeSources,
    // saveSource, toggleActive and clearError functions here.
}
