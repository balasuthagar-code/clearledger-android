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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel
class IncomeSourcesViewModel @Inject constructor(
    private val repository: IncomeSourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeSourcesUiState())
    val uiState: StateFlow<IncomeSourcesUiState> = _uiState.asStateFlow()

    init {
        loadIncomeSources()
    }

    fun loadIncomeSources() {
        viewModelScope.launch {
            repository.getIncomeSources()
                .onStart {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                }
                .catch { exception ->
                    if (exception is CancellationException) throw exception

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                            ?: "Failed to load income sources."
                    )
                }
                .collect { sources ->
                    _uiState.value = _uiState.value.copy(
                        incomeSources = sources,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun addIncomeSource(source: IncomeSource) {
        if (source.name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter an income source name.",
                message = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                message = null
            )

            try {
                repository.addIncomeSource(source)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Income source added successfully.",
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = exception.message
                        ?: "Failed to add income source."
                )
            }
        }
    }

    fun updateIncomeSource(source: IncomeSource) {
        if (source.id.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Income source ID is missing.",
                message = null
            )
            return
        }

        if (source.name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter an income source name.",
                message = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                message = null
            )

            try {
                repository.updateIncomeSource(source)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Income source updated successfully.",
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = exception.message
                        ?: "Failed to update income source."
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}