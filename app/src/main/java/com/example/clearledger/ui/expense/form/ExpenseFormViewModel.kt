package com.example.clearledger.ui.expense.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.DefaultCategories
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.MerchantSuggestion
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.usecase.ExpenseDraft
import com.example.clearledger.domain.usecase.ExpenseFormErrors
import com.example.clearledger.domain.usecase.SaveExpenseResult
import com.example.clearledger.domain.usecase.SaveExpenseUseCase
import com.example.clearledger.domain.usecase.SuggestMerchantsUseCase
import com.example.clearledger.navigation.ExpenseRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Everything the form screen shows. Immutable; the ViewModel replaces it on every change. */
data class ExpenseFormState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,                 // true while an existing expense is being loaded
    val loadError: String? = null,
    val amountText: String = "",
    val currency: String = Currencies.BASE,
    val rateText: String = "",
    val categoryId: String? = null,
    val classification: ExpenseClassification = ExpenseClassification.DISCRETIONARY,
    /** Once the user taps a classification chip, picking a category no longer overrides it. */
    val classificationChosenByUser: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val merchant: String = "",
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val errors: ExpenseFormErrors = ExpenseFormErrors(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

sealed interface ExpenseFormEvent {
    data object Saved : ExpenseFormEvent
}

/**
 * One ViewModel for BOTH "Add expense" (quick-add, FR-EXP-01) and "Edit expense" (FR-EXP-07).
 * Edit mode is selected by the navigation argument "expenseId"; no id = add mode.
 */
@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val saveExpense: SaveExpenseUseCase,
    private val suggestMerchants: SuggestMerchantsUseCase
) : ViewModel() {

    private val expenseId: String? = savedStateHandle.get<String>(ExpenseRoutes.ARG_EXPENSE_ID)
    private var createdAtMillis: Long = 0L

    private val _state = MutableStateFlow(
        ExpenseFormState(
            isEditing = expenseId != null,
            isLoading = expenseId != null,
            classificationChosenByUser = expenseId != null    // keep the saved classification when editing
        )
    )
    val state: StateFlow<ExpenseFormState> = _state.asStateFlow()

    private val _events = Channel<ExpenseFormEvent>(Channel.BUFFERED)
    val events: Flow<ExpenseFormEvent> = _events.receiveAsFlow()

    /** Favourite categories first (FR-EXP-09), then the normal order. */
    val categories: StateFlow<List<ExpenseCategory>> =
        categoryRepository.observeCategories()
            .map { list -> list.sortedWith(compareByDescending<ExpenseCategory> { it.isFavourite }.thenBy { it.sortOrder }) }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** FR-EXP-08: merchants seen in the user's recent expenses, with their usual category. */
    private val merchantMemory: StateFlow<List<MerchantSuggestion>> =
        expenseRepository.observeRecentExpenses(MERCHANT_MEMORY_SIZE)
            .map { suggestMerchants.buildMemory(it) }
            .catch { emit(emptyList()) }                 // suggestions are a nicety: never an error screen
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The chips under the amount field: most used merchants, narrowed down as the user types. */
    val merchantSuggestions: StateFlow<List<MerchantSuggestion>> =
        combine(merchantMemory, _state.map { it.merchant }.distinctUntilChanged()) { memory, typed ->
            suggestMerchants(memory, typed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Safe to call repeatedly; makes sure the category chips are never empty on first use.
        viewModelScope.launch { categoryRepository.seedDefaultCategoriesIfEmpty() }
        if (expenseId != null) loadExpense(expenseId)
    }

    private fun loadExpense(id: String) {
        viewModelScope.launch {
            when (val result = expenseRepository.getExpense(id)) {
                is Result.Success -> {
                    val expense = result.data
                    if (expense == null) {
                        _state.update { it.copy(isLoading = false, loadError = "This expense no longer exists.") }
                    } else {
                        populate(expense)
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, loadError = "Could not load this expense. Check your connection and try again.")
                }
            }
        }
    }

    private fun populate(e: Expense) {
        createdAtMillis = e.createdAtMillis
        _state.update {
            it.copy(
                isLoading = false,
                amountText = MoneyInput.minorToInputText(e.amountMinor),
                currency = e.currency,
                rateText = if (e.currency == Currencies.BASE) "" else e.exchangeRate,
                categoryId = e.categoryId,
                classification = e.classification,
                paymentMethod = e.paymentMethod,
                merchant = e.merchant,
                note = e.note,
                date = e.date
            )
        }
    }

    // ---- field changes (each one also clears that field's old error message) ----

    fun onAmountChange(text: String) =
        _state.update { it.copy(amountText = text, errors = it.errors.copy(amount = null)) }

    fun onCurrencyChange(currency: String) =
        _state.update { it.copy(currency = currency, errors = it.errors.copy(rate = null)) }

    fun onRateChange(text: String) =
        _state.update { it.copy(rateText = text, errors = it.errors.copy(rate = null)) }

    /** FR-EXP-03: the category's default classification is applied unless the user already chose one. */
    fun onCategorySelected(categoryId: String) {
        val default = (categories.value.firstOrNull { it.id == categoryId }
            ?: DefaultCategories.all.firstOrNull { it.id == categoryId })?.defaultClassification
        _state.update {
            it.copy(
                categoryId = categoryId,
                classification = if (!it.classificationChosenByUser && default != null) default else it.classification,
                errors = it.errors.copy(category = null)
            )
        }
    }

    fun onClassificationSelected(classification: ExpenseClassification) =
        _state.update { it.copy(classification = classification, classificationChosenByUser = true) }

    fun onPaymentMethodSelected(method: PaymentMethod) = _state.update { it.copy(paymentMethod = method) }
    fun onMerchantChange(text: String) = _state.update { it.copy(merchant = text) }

    /**
     * FR-EXP-08: tapping a remembered merchant fills the merchant and pre-fills its usual category -
     * but never replaces a category the user already picked (and never touches one when editing).
     */
    fun onMerchantSuggestionSelected(suggestion: MerchantSuggestion) {
        _state.update { it.copy(merchant = suggestion.name) }
        if (_state.value.categoryId == null && suggestion.categoryId.isNotBlank()) {
            onCategorySelected(suggestion.categoryId)
        }
    }

    /** FR-EXP-09: star / unstar the selected category; favourites are listed first next time. */
    fun toggleFavourite() {
        val id = _state.value.categoryId ?: return
        val category = categories.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            if (categoryRepository.setFavourite(id, !category.isFavourite) is Result.Error) {
                _state.update { it.copy(saveError = "Could not update your favourite categories. Please try again.") }
            }
        }
    }
    fun onNoteChange(text: String) = _state.update { it.copy(note = text) }
    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun save() {
        val s = _state.value
        if (s.isSaving || s.isLoading || s.loadError != null) return    // ignore double taps
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val result = saveExpense(
                ExpenseDraft(
                    id = expenseId,
                    amountText = s.amountText,
                    currency = s.currency,
                    rateText = s.rateText,
                    categoryId = s.categoryId,
                    classification = s.classification,
                    paymentMethod = s.paymentMethod,
                    merchant = s.merchant,
                    note = s.note,
                    date = s.date,
                    createdAtMillis = createdAtMillis
                )
            )
            when (result) {
                is SaveExpenseResult.Saved -> {
                    _state.update { it.copy(isSaving = false) }
                    _events.send(ExpenseFormEvent.Saved)
                }
                is SaveExpenseResult.Invalid -> _state.update { it.copy(isSaving = false, errors = result.errors) }
                is SaveExpenseResult.Failed -> _state.update { it.copy(isSaving = false, saveError = result.message) }
            }
        }
    }

    private companion object {
        const val MERCHANT_MEMORY_SIZE = 200
    }
}
