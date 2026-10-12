
package com.example.clearledger.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.IncomeEntryValidator
import com.example.clearledger.data.repository.IncomeEntryRepository
import com.example.clearledger.data.repository.IncomeSourceRepository
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IncomeEntriesViewModel @Inject constructor(
    private val entryRepository: IncomeEntryRepository,
    private val sourceRepository: IncomeSourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeEntriesUiState())
    val uiState: StateFlow<IncomeEntriesUiState> = _uiState.asStateFlow()

    private val entriesFlow = MutableStateFlow<List<IncomeEntry>>(emptyList())
    private val sourcesFlow = MutableStateFlow<List<IncomeSource>>(emptyList())

    private val lastRates = mutableMapOf<String, String>()

    init {
        observeEntries()
        observeSources()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            entryRepository.getIncomeEntries()
                .catch { showError(it.message ?: "Unable to load income history.") }
                .collect { entries ->
                    entriesFlow.value = entries
                    refreshState()
                }
        }
    }

    private fun observeSources() {
        viewModelScope.launch {
            sourceRepository.getIncomeSources()
                .catch { showError(it.message ?: "Unable to load income sources.") }
                .collect { sources ->
                    sourcesFlow.value = sources
                    refreshState()
                }
        }
    }

    private fun refreshState() {
        _uiState.update {
            it.copy(
                entries = entriesFlow.value,
                sources = sourcesFlow.value,
                isLoading = false
            )
        }
    }

    fun setMonthFilter(month: String) {
        _uiState.update { it.copy(selectedMonth = month.trim()) }
    }

    fun setSourceFilter(sourceId: String) {
        _uiState.update { it.copy(selectedSourceId = sourceId) }
    }

    fun lastRateFor(currency: String): String =
        if (currency == "LKR") "1" else lastRates[currency] ?: ""

    fun saveEntry(
        id: String = "",
        sourceId: String,
        amount: String,
        currency: String,
        rate: String,
        date: String,
        note: String,
        status: String
    ) {
        val activeSource = sourcesFlow.value.firstOrNull {
            it.id == sourceId && it.active
        }

        if (activeSource == null) {
            showError("Select an active income source.")
            return
        }

        if (currency !in IncomeEntryValidator.supportedCurrencies) {
            showError("Select a supported currency.")
            return
        }

        val amountMinor =
            IncomeEntryValidator.parseAmountToMinorUnits(amount)

        if (amountMinor == null) {
            showError("Enter an amount greater than zero.")
            return
        }

        if (!IncomeEntryValidator.isValidDate(date)) {
            showError("Enter the date in YYYY-MM-DD format.")
            return
        }

        if (status !in IncomeEntryValidator.statuses) {
            showError("Select Expected or Received.")
            return
        }

        val effectiveRate = if (currency == "LKR") "1" else rate.trim()

        val baseAmountMinor = IncomeEntryValidator.calculateBaseAmount(
            originalAmountMinor = amountMinor,
            exchangeRate = effectiveRate
        )

        if (baseAmountMinor == null) {
            showError("Enter a valid exchange rate greater than zero.")
            return
        }

        val monthKey = try {
            IncomeEntryValidator.monthKey(date)
        } catch (_: Exception) {
            showError("Enter a valid date.")
            return
        }

        val entry = IncomeEntry(
            id = id,
            sourceId = activeSource.id,
            sourceName = activeSource.name,
            originalAmountMinor = amountMinor,
            currency = currency,
            exchangeRate = effectiveRate,
            baseAmountMinor = baseAmountMinor,
            date = date.trim(),
            monthKey = monthKey,
            note = note.trim(),
            status = status
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                if (currency != "LKR") {
                    lastRates[currency] = effectiveRate
                }

                if (id.isBlank()) {
                    entryRepository.addIncomeEntry(entry)
                } else {
                    entryRepository.updateIncomeEntry(entry)
                }

                _uiState.update {
                    it.copy(isSaving = false, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e.message ?: "Unable to save the income entry.")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                entryRepository.deleteIncomeEntry(entryId)
                _uiState.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(e.message ?: "Unable to delete the entry.")
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
