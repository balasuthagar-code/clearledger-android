package com.example.clearledger.ui.crypto

import com.example.clearledger.currency.ConfirmedRateRecorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.CryptoEntryValidator
import com.example.clearledger.data.repository.CryptoEntryRepository
import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryType
import com.example.clearledger.domain.model.CryptoEntryOptions
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CryptoUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val entries: List<CryptoEntry> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val repository: CryptoEntryRepository,
    private val rateRecorder: ConfirmedRateRecorder
) : ViewModel() {
    private val currencyOwner = rateRecorder.ownerUid()

    private val _uiState = MutableStateFlow(CryptoUiState())
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    init {
        loadEntries()
    }

    fun loadEntries() {
        listener?.remove()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        listener = repository.listenToEntries(
            onSuccess = { entries ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    entries = entries,
                    error = null
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = message
                )
            }
        )
    }

    fun addEntry(
        assetName: String,
        date: String,
        type: String,
        originalValue: String,
        currency: String,
        rate: String,
        costBasisLkr: String,
        note: String
    ) {
        if (_uiState.value.isSaving) return
        if (!rateRecorder.isOwner(currencyOwner)) {
            _uiState.value = _uiState.value.copy(error = "Account changed. Reopen this form.")
            return
        }
        val validationError = CryptoEntryValidator.validate(
            assetName = assetName,
            date = date,
            type = type,
            originalValue = originalValue,
            currency = currency,
            rate = rate,
            costBasisLkr = costBasisLkr
        )

        if (validationError != null) {
            _uiState.value = _uiState.value.copy(
                error = validationError,
                message = null
            )
            return
        }

        val values = CryptoEntryValidator.convert(
            date = date,
            type = type,
            originalValue = originalValue,
            currency = currency,
            rate = rate,
            costBasisLkr = costBasisLkr
        )

        if (values == null) {
            _uiState.value = _uiState.value.copy(
                error = "Unable to calculate the values. Please check your inputs."
            )
            return
        }

        val entry = CryptoEntry(
            assetName = assetName.trim(),
            date = date,
            monthKey = values.monthKey,
            type = type,
            originalCurrency = currency,
            originalValueMinor = values.originalValueMinor,
            exchangeRate = values.exchangeRate,
            baseValueMinor = values.baseValueMinor,
            costBasisBaseMinor = values.costBasisBaseMinor,
            note = note.trim()
        )

        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )

        val remembered = rateRecorder.capture(currency, "LKR", entry.exchangeRate, currencyOwner)
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.addEntry(
            entry = entry,
            onSuccess = {
                viewModelScope.launch { rateRecorder.afterSave(remembered) }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = null,
                    message = "Crypto entry saved."
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = message,
                    message = null
                )
            }
        )
    }

    fun deleteEntry(entryId: String) {
        repository.deleteEntry(
            entryId = entryId,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    message = "Crypto entry deleted.",
                    error = null
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    error = message,
                    message = null
                )
            }
        )
    }

    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
// thisanth