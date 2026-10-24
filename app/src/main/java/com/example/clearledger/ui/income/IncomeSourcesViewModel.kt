
package com.example.clearledger.ui.income

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.clearledger.core.common.IncomeSourceValidator
import com.example.clearledger.data.repository.FirestoreIncomeSourceRepository
import com.example.clearledger.data.repository.IncomeSourceSummary
import com.example.clearledger.domain.model.IncomeSource



class IncomeSourcesViewModel(
    private val repository: FirestoreIncomeSourceRepository =
        FirestoreIncomeSourceRepository()
) : ViewModel() {

    var state by mutableStateOf(IncomeSourcesUiState())
        private set

    init {
        loadSources()
    }

    fun loadSources() {
        state = state.copy(isLoading = true, error = null)

        repository.loadSources { result ->
            result.fold(
                onSuccess = { summaries ->
                    state = state.copy(
                        summaries = summaries,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        error = error.message ?: "Could not load income sources."
                    )
                }
            )
        }
    }

    fun saveSource(source: IncomeSource) {
        val validationError = IncomeSourceValidator.validate(source)

        if (validationError != null) {
            state = state.copy(error = validationError, message = null)
            return
        }

        repository.saveSource(source) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        message = "Income source saved.",
                        error = null
                    )
                    loadSources()
                },
                onFailure = { error ->
                    state = state.copy(
                        error = error.message ?: "Could not save the source.",
                        message = null
                    )
                }
            )
        }
    }

    fun deleteSource(sourceId: String) {
        repository.deleteSource(sourceId) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        message = "Income source deleted.",
                        error = null
                    )
                    loadSources()
                },
                onFailure = { error ->
                    state = state.copy(
                        error = error.message ?: "Could not delete the source.",
                        message = null
                    )
                }
            )
        }
    }

    fun toggleActive(source: IncomeSource) {
        repository.setActive(source.id, !source.isActive) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        message = if (source.isActive) {
                            "Income source deactivated."
                        } else {
                            "Income source activated."
                        },
                        error = null
                    )
                    loadSources()
                },
                onFailure = { error ->
                    state = state.copy(
                        error = error.message ?: "Could not update source status.",
                        message = null
                    )
                }
            )
        }
    }

    fun clearMessages() {
        state = state.copy(error = null, message = null)
    }
}
