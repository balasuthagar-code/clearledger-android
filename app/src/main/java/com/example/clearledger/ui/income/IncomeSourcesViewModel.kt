
package com.example.clearledger.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.data.repository.IncomeSourceRepository
import com.example.clearledger.domain.model.IncomeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IncomeSourcesViewModel @Inject constructor(
    private val repository: IncomeSourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IncomeSourcesUiState(isLoading = true)
    )

    val uiState: StateFlow<IncomeSourcesUiState> =
        _uiState.asStateFlow()

    init {
        observeSources()
    }

    private fun observeSources() {
        viewModelScope.launch {
            repository.getIncomeSources()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                                ?: "Unable to load income sources."
                        )
                    }
                }
                .collect { sources ->
                    _uiState.update {
                        it.copy(
                            sources = sources,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun saveSource(
        id: String = "",
        name: String,
        type: String,
        currency: String
    ) {
        if (name.isBlank()) {
            showError("Enter an income source name.")
            return
        }

        if (type.isBlank()) {
            showError("Enter an income source type.")
            return
        }

        if (!currency.trim().matches(Regex("[A-Za-z]{3}"))) {
            showError("Enter a valid 3-letter currency code, such as LKR.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                if (id.isBlank()) {
                    repository.addIncomeSource(
                        IncomeSource(
                            name = name.trim(),
                            type = type.trim(),
                            defaultCurrency = currency.trim().uppercase(),
                            active = true
                        )
                    )
                } else {
                    val oldSource = _uiState.value.sources
                        .firstOrNull { it.id == id }

                    repository.updateIncomeSource(
                        IncomeSource(
                            id = id,
                            name = name.trim(),
                            type = type.trim(),
                            defaultCurrency = currency.trim().uppercase(),
                            active = oldSource?.active ?: true
                        )
                    )
                }

                _uiState.update {
                    it.copy(isLoading = false, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unable to save income source."
                    )
                }
            }
        }
    }

    fun toggleActive(source: IncomeSource) {
        viewModelScope.launch {
            try {
                repository.updateIncomeSource(
                    source.copy(active = !source.active)
                )
                _uiState.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e.message ?: "Unable to update income source.")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
