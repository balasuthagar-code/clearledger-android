package com.example.clearledger.ui.recurring.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.RecurringRepository
import com.example.clearledger.domain.usecase.RecurringDraft
import com.example.clearledger.domain.usecase.RecurringFormErrors
import com.example.clearledger.domain.usecase.SaveRecurringResult
import com.example.clearledger.domain.usecase.SaveRecurringUseCase
import com.example.clearledger.navigation.RecurringRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringFormState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val name: String = "",
    val amountText: String = "",
    val currency: String = Currencies.BASE,
    val rateText: String = "",
    val categoryId: String? = null,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dueDayText: String = "1",
    val dueMonthText: String = "1",
    val isActive: Boolean = true,
    val errors: RecurringFormErrors = RecurringFormErrors(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

sealed interface RecurringFormEvent {
    data object Saved : RecurringFormEvent
}

/** One ViewModel for "Add recurring item" and "Edit recurring item" (selected by the route argument). */
@HiltViewModel
class RecurringFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecurringRepository,
    categoryRepository: CategoryRepository,
    private val saveRecurring: SaveRecurringUseCase
) : ViewModel() {

    private val recurringId: String? = savedStateHandle.get<String>(RecurringRoutes.ARG_RECURRING_ID)

    private val _state = MutableStateFlow(
        RecurringFormState(isEditing = recurringId != null, isLoading = recurringId != null)
    )
    val state: StateFlow<RecurringFormState> = _state.asStateFlow()

    private val _events = Channel<RecurringFormEvent>(Channel.BUFFERED)
    val events: Flow<RecurringFormEvent> = _events.receiveAsFlow()

    val categories: StateFlow<List<ExpenseCategory>> =
        categoryRepository.observeCategories()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (recurringId != null) load(recurringId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            when (val result = repository.getRecurring(id)) {
                is Result.Success -> {
                    val item = result.data
                    if (item == null) {
                        _state.update { it.copy(isLoading = false, loadError = "This item no longer exists.") }
                    } else {
                        populate(item)
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, loadError = "Could not load this item. Check your connection and try again.")
                }
            }
        }
    }

    private fun populate(item: RecurringItem) {
        _state.update {
            it.copy(
                isLoading = false,
                name = item.name,
                amountText = MoneyInput.minorToInputText(item.amountMinor),
                currency = item.currency,
                rateText = if (item.currency == Currencies.BASE) "" else item.exchangeRate,
                categoryId = item.categoryId,
                frequency = item.frequency,
                dueDayText = item.dueDay.toString(),
                dueMonthText = item.dueMonth.toString(),
                isActive = item.isActive
            )
        }
    }

    // ---- field changes (each also clears that field's old error) ----
    fun onNameChange(text: String) = _state.update { it.copy(name = text, errors = it.errors.copy(name = null)) }
    fun onAmountChange(text: String) = _state.update { it.copy(amountText = text, errors = it.errors.copy(amount = null)) }
    fun onCurrencyChange(code: String) = _state.update { it.copy(currency = code, errors = it.errors.copy(rate = null)) }
    fun onRateChange(text: String) = _state.update { it.copy(rateText = text, errors = it.errors.copy(rate = null)) }
    fun onCategorySelected(id: String) = _state.update { it.copy(categoryId = id, errors = it.errors.copy(category = null)) }

    /** Changing the frequency resets the due day to a sensible default for that frequency. */
    fun onFrequencyChange(frequency: RecurringFrequency) = _state.update {
        it.copy(
            frequency = frequency,
            dueDayText = if (frequency == it.frequency) it.dueDayText else "1",
            errors = it.errors.copy(dueDay = null, dueMonth = null)
        )
    }

    fun onDueDayChange(text: String) = _state.update { it.copy(dueDayText = text, errors = it.errors.copy(dueDay = null)) }
    fun onDueMonthChange(text: String) = _state.update { it.copy(dueMonthText = text, errors = it.errors.copy(dueMonth = null)) }
    fun onActiveChange(active: Boolean) = _state.update { it.copy(isActive = active) }

    fun save() {
        val s = _state.value
        if (s.isSaving || s.isLoading || s.loadError != null) return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val result = saveRecurring(
                RecurringDraft(
                    id = recurringId,
                    name = s.name,
                    amountText = s.amountText,
                    currency = s.currency,
                    rateText = s.rateText,
                    categoryId = s.categoryId,
                    frequency = s.frequency,
                    dueDayText = s.dueDayText,
                    dueMonthText = s.dueMonthText,
                    isActive = s.isActive
                )
            )
            when (result) {
                is SaveRecurringResult.Saved -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.send(RecurringFormEvent.Saved)
                }
                is SaveRecurringResult.Invalid -> _state.update { it.copy(isSaving = false, errors = result.errors) }
                is SaveRecurringResult.Failed -> _state.update { it.copy(isSaving = false, saveError = result.message) }
            }
        }
    }
}
