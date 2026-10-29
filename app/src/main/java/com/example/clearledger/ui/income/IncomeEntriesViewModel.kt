
package com.example.clearledger.ui.income

import com.example.clearledger.currency.ConfirmedRateRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.clearledger.core.common.IncomeEntryValidator
import com.example.clearledger.data.repository.FirestoreIncomeEntryRepository
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeEntryOptions
import com.example.clearledger.domain.model.IncomeSource
import java.time.LocalDate
import com.example.clearledger.core.common.CurrencyCore
import com.example.clearledger.core.common.CurrencyException
import com.example.clearledger.core.common.CurrencyFailure
import com.example.clearledger.core.common.Result
import com.example.clearledger.data.repository.IncomeEntryRepository


@HiltViewModel
class IncomeEntriesViewModel @Inject constructor(
    private val repository: IncomeEntryRepository,
    private val rateRecorder: ConfirmedRateRecorder
) : ViewModel() {
    private val currencyOwner = rateRecorder.ownerUid()

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
        if (state.isSaving) return
        if (!rateRecorder.isOwner(currencyOwner)) {
            state = state.copy(error = "Account changed. Reopen this form.")
            return
        }
        val amountMinor = when (
            val result = CurrencyCore.parseAmount(amountText, currency)
        ) {
            is Result.Success -> result.data

            is Result.Error -> {
                val failure =
                    (result.exception as? CurrencyException)?.failure

                val message = when (failure) {
                    CurrencyFailure.UNSUPPORTED_CURRENCY ->
                        "Please select a valid currency."

                    CurrencyFailure.EXCESS_AMOUNT_PRECISION ->
                        "Enter an amount with no more than two decimal places."

                    CurrencyFailure.AMOUNT_OVERFLOW ->
                        "The amount is too large."

                    else ->
                        "Enter a valid amount."
                }

                state = state.copy(error = message, message = null)
                return
            }
        }

        if (amountMinor <= 0L)  {
            state = state.copy(
                error = "Enter a valid amount greater than zero.",
                message = null
            )
            return
        }

        // Existing income screens currently use LKR as their base currency.
        val baseCurrency = "LKR"

        val parsedRate = if (currency == baseCurrency) {
            null
        } else {
            when (val result = CurrencyCore.parseRate(rateText)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    state = state.copy(
                        error = "Enter a valid exchange rate greater than zero.",
                        message = null
                    )
                    return
                }
            }
        }

        val conversion = when (
            val result = CurrencyCore.convert(
                originalAmountMinor = amountMinor,
                originalCurrency = currency,
                baseCurrency = baseCurrency,
                exchangeRate = parsedRate
            )
        ) {
            is Result.Success -> result.data
            is Result.Error -> {
                val failure =
                    (result.exception as? CurrencyException)?.failure

                val errorMessage = when (failure) {
                    CurrencyFailure.CONVERSION_OVERFLOW ->
                        "The converted amount is too large."

                    CurrencyFailure.POSITIVE_AMOUNT_ROUNDS_TO_ZERO ->
                        "The converted amount is too small."

                    else ->
                        "Could not convert this amount. Check the currency and rate."
                }

                state = state.copy(error = errorMessage, message = null)
                return
            }
        }

        val unchangedMoney = existing != null && existing.originalCurrency == currency &&
            existing.originalAmountMinor == amountMinor && existing.exchangeRate.toBigDecimalOrNull()?.compareTo(conversion.appliedRate) == 0
        val rate = if (unchangedMoney) existing!!.exchangeRate else conversion.appliedRatePlain

        val entry = IncomeEntry(
            id = existing?.id ?: "",
            sourceId = sourceId,
            originalAmountMinor = amountMinor,
            originalCurrency = currency,
            exchangeRate = rate,
            baseAmountMinor = if (unchangedMoney) existing!!.baseAmountMinor else conversion.baseAmountMinor,
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

        val remembered = if (existing == null || !unchangedMoney) rateRecorder.capture(currency, baseCurrency, rate, currencyOwner) else null
        repository.saveEntry(entry) { result ->
            result.fold(
                onSuccess = {
                    viewModelScope.launch { rateRecorder.afterSave(remembered) }
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

    fun updateStatus(entryId: String, status: String) {
        if (status !in IncomeEntryOptions.statuses) {
            state = state.copy(
                error = "Please select a valid income status.",
                message = null
            )
            return
        }

        repository.updateStatus(entryId, status) { result ->
            result.fold(
                onSuccess = {
                    state = state.copy(
                        message = "Income status updated.",
                        error = null
                    )
                    refresh()
                },
                onFailure = {
                    state = state.copy(
                        error = it.message ?: "Could not update income status.",
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
