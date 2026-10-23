
package com.example.clearledger.ui.income

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.clearledger.core.common.IncomeEntryValidator
import com.example.clearledger.data.repository.FirestoreIncomeEntryRepository
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import java.math.BigDecimal
import java.time.LocalDate


class IncomeEntriesViewModel(
    private val repository: FirestoreIncomeEntryRepository =
        FirestoreIncomeEntryRepository()
) : ViewModel() {

    var state by mutableStateOf(IncomeEntriesUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = state.copy(isLoading = true, error = null)

        repository.loadSources { result ->
            result.fold(
                onSuccess = { sources ->
                    state = state.copy(sources = sources)
                },
                onFailure = {
                    state = state.copy(
                        error = it.message ?: "Could not load income sources."
                    )
                }
            )

            repository.loadEntries { entryResult ->
                entryResult.fold(
                    onSuccess = { entries ->
                        state = state.copy(
                            entries = entries,
                            isLoading = false
                        )
                    },
                    onFailure = {
                        state = state.copy(
                            isLoading = false,
                            error = it.message ?: "Could not load income entries."
                        )
                    }
                )
            }
        }
    }

    fun saveEntry(
        existing: IncomeEntry?,
        sourceId: String,
        amountText: String,
        currency: String,
        rateText: String,
        date: String,
        note: String,
        status: String
    ) {
        val amountMinor = try {
            BigDecimal(amountText.trim())
                .movePointRight(2)
                .longValueExact()
        } catch (_: Exception) {
            null
        }

        if (amountMinor == null || amountMinor <= 0L) {
            state = state.copy(
                error = "Enter a valid amount greater than zero.",
                message = null
            )
            return
        }

        val rate = if (currency == "LKR") "1" else rateText.trim()

        val entry = IncomeEntry(
            id = existing?.id ?: "",
            sourceId = sourceId,
            originalAmountMinor = amountMinor,
            originalCurrency = currency,
            exchangeRate = rate,
            baseAmountMinor = try {
                IncomeEntryValidator.calculateBaseAmountMinor(amountMinor, rate)
            } catch (_: Exception) {
                state = state.copy(
                    error = "Enter a valid positive exchange rate.",
                    message = null
                )
                return
            },
            date = date.trim(),
            monthKey = try {
                IncomeEntryValidator.monthKey(date.trim())
            } catch (_: Exception) {
                ""
            },
            status = status,
            note = note.trim(),
            createdAt = existing?.createdAt ?: 0L,
            updatedAt = System.currentTimeMillis()
        )

        val validationError = IncomeEntryValidator.validate(entry)
        if (validationError != null) {
            state = state.copy(error = validationError, message = null)
            return
        }

        state = state.copy(isSaving = true, error = null, message = null)

        repository.saveEntry(entry) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        isSaving = false,
                        message = if (existing == null) {
                            "Income entry saved."
                        } else {
                            "Income entry updated."
                        },
                        error = null
                    )
                    refresh()
                },
                onFailure = {
                    state = state.copy(
                        isSaving = false,
                        error = it.message ?: "Could not save the income entry."
                    )
                }
            )
        }
    }

    fun deleteEntry(entryId: String) {
        repository.deleteEntry(entryId) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        message = "Income entry deleted.",
                        error = null
                    )
                    refresh()
                },
                onFailure = {
                    state = state.copy(
                        error = it.message ?: "Could not delete the entry.",
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

// thisanth
