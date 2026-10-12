package com.example.clearledger.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.IncomeEntryValidator
import com.example.clearledger.data.repository.IncomeEntryRepository
import com.example.clearledger.data.repository.IncomeSourceRepository
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class IncomeEntriesViewModel @Inject constructor(
    private val entryRepository: IncomeEntryRepository,
    private val sourceRepository: IncomeSourceRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow("")
    private val selectedSourceId = MutableStateFlow("")

    private val _uiState = MutableStateFlow(IncomeEntriesUiState())
    val uiState: StateFlow<IncomeEntriesUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                entryRepository.getIncomeEntries(),
                sourceRepository.getIncomeSources(),
                selectedMonth,
                selectedSourceId
            ) { entries, sources, month, sourceId ->

                val filteredEntries = entries.filter { entry ->
                    (month.isBlank() || entry.monthKey == month) &&
                            (sourceId.isBlank() || entry.sourceId == sourceId)
                }

                IncomeEntriesUiState(
                    entries = filteredEntries,
                    sources = sources,
                    isLoading = false,
                    selectedMonth = month,
                    selectedSourceId = sourceId,
                    error = _uiState.value.error
                )
            }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load income entries"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun filterByMonth(month: String) {
        selectedMonth.value = month.trim()
    }

    fun filterBySource(sourceId: String) {
        selectedSourceId.value = sourceId
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun saveIncomeEntry(
        entryId: String? = null,
        sourceId: String,
        amount: String,
        currency: String,
        exchangeRate: String,
        date: String,
        note: String,
        status: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null
            )

            try {
                val source = _uiState.value.sources.firstOrNull {
                    it.id == sourceId && it.active
                } ?: throw IllegalArgumentException(
                    "Please select an active income source"
                )

                if (currency !in IncomeEntryValidator.supportedCurrencies) {
                    throw IllegalArgumentException("Select a supported currency")
                }

                if (status !in IncomeEntryValidator.statuses) {
                    throw IllegalArgumentException("Select a valid income status")
                }

                if (!IncomeEntryValidator.isValidDate(date)) {
                    throw IllegalArgumentException(
                        "Enter a valid date in YYYY-MM-DD format"
                    )
                }

                val amountMinor =
                    IncomeEntryValidator.parseAmountToMinorUnits(amount)
                        ?: throw IllegalArgumentException(
                            "Enter an amount greater than zero"
                        )

                val rate = if (currency == "LKR") "1" else exchangeRate

                val baseAmountMinor =
                    IncomeEntryValidator.calculateBaseAmount(
                        amountMinor,
                        rate
                    ) ?: throw IllegalArgumentException(
                        "Enter a valid exchange rate greater than zero"
                    )

                val entry = IncomeEntry(
                    id = entryId.orEmpty(),
                    sourceId = source.id,
                    sourceName = source.name,
                    originalAmountMinor = amountMinor,
                    currency = currency,
                    exchangeRate = rate,
                    baseAmountMinor = baseAmountMinor,
                    date = date.trim(),
                    monthKey = IncomeEntryValidator.monthKey(date.trim()),
                    note = note.trim(),
                    status = status
                )

                if (entryId.isNullOrBlank()) {
                    entryRepository.addIncomeEntry(entry)
                } else {
                    entryRepository.updateIncomeEntry(entry)
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Unable to save income"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun deleteIncomeEntry(entryId: String) {
        viewModelScope.launch {
            try {
                entryRepository.deleteIncomeEntry(entryId)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Unable to delete income"
                )
            }
        }
    }
}
