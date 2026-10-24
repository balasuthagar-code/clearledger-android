package com.example.clearledger.ui.expense.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.usecase.ExpenseDayGroup
import com.example.clearledger.domain.usecase.GroupExpensesByDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the list screen shows once data has loaded.
 * [monthHasExpenses] tells "this month is empty" apart from "the category filter hides everything".
 */
data class ExpenseListContent(
    val groups: List<ExpenseDayGroup>,
    val shownTotalMinor: Long,
    val shownCount: Int,
    val monthHasExpenses: Boolean
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    private val groupExpenses: GroupExpensesByDateUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(MonthKey.current())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    /** null = "All categories". */
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val retryCount = MutableStateFlow(0)

    /** One-shot text for a Snackbar ("Expense deleted"); the screen calls messageShown() afterwards. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<ExpenseListContent>> =
        combine(_selectedMonth, retryCount) { month, _ -> month }
            .flatMapLatest { month ->
                // Changing the category filter must NOT restart the Firestore listener,
                // so it is combined inside this block instead of being a flatMapLatest trigger.
                combine(
                    expenseRepository.observeExpensesForMonth(month),
                    _selectedCategoryId
                ) { expenses, categoryId ->
                    val groups = groupExpenses(expenses, categoryId)
                    UiState.Success(
                        ExpenseListContent(
                            groups = groups,
                            shownTotalMinor = groups.sumOf { it.totalBaseMinor },
                            shownCount = groups.sumOf { it.expenses.size },
                            monthHasExpenses = expenses.isNotEmpty()
                        )
                    ) as UiState<ExpenseListContent>
                }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error("Could not load your expenses. Check your connection and try again.")) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** For the filter chips and for showing "Food" instead of the id "food". */
    val categories: StateFlow<List<ExpenseCategory>> =
        categoryRepository.observeCategories()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun showPreviousMonth() { _selectedMonth.value = MonthKey.previous(_selectedMonth.value) }
    fun showNextMonth() { _selectedMonth.value = MonthKey.next(_selectedMonth.value) }
    fun selectCategory(categoryId: String?) { _selectedCategoryId.value = categoryId }
    fun retry() { retryCount.value += 1 }

    /** FR-EXP-07: deleting removes the document; the real-time listener updates the list and the dashboard. */
    fun deleteExpense(id: String) {
        viewModelScope.launch {
            _message.value = when (expenseRepository.deleteExpense(id)) {
                is Result.Success -> "Expense deleted"
                is Result.Error -> "Could not delete the expense. Please try again."
            }
        }
    }

    fun messageShown() { _message.value = null }
}
