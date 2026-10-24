package com.example.clearledger.ui.recurring.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.RecurringRepository
import com.example.clearledger.domain.usecase.CalculatePlannedCommitmentsUseCase
import com.example.clearledger.domain.usecase.ConfirmRecurringResult
import com.example.clearledger.domain.usecase.ConfirmRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveDueRecurringUseCase
import com.example.clearledger.domain.usecase.PlannedCommitments
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
import java.time.LocalDate
import javax.inject.Inject

data class RecurringListContent(
    val items: List<RecurringItem>,
    val planned: PlannedCommitments,
    /** FR-REC-03: items due in the current month with their status. */
    val due: List<DueRecurring> = emptyList()
)

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val repository: RecurringRepository,
    categoryRepository: CategoryRepository,
    private val calculatePlanned: CalculatePlannedCommitmentsUseCase,
    observeDue: ObserveDueRecurringUseCase,
    private val confirmRecurring: ConfirmRecurringUseCase
) : ViewModel() {

    /** The day the screen was opened; "this month" and "overdue" are measured from it. */
    val today: LocalDate = LocalDate.now()
    private val currentMonthKey = MonthKey.of(today)

    private val retryCount = MutableStateFlow(0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<RecurringListContent>> =
        retryCount
            .flatMapLatest {
                combine<List<RecurringItem>, List<DueRecurring>, UiState<RecurringListContent>>(
                    repository.observeRecurring(),
                    observeDue(today)
                ) { items, due ->
                    UiState.Success(RecurringListContent(items, calculatePlanned(items), due))
                }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error("Could not load your recurring items. Check your connection and try again.")) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** categoryId -> name, so "rent" is shown as "Rent". */
    val categoryNames: StateFlow<Map<String, String>> =
        categoryRepository.observeCategories()
            .map { list: List<ExpenseCategory> -> list.associate { it.id to it.name } }
            .catch { emit(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun retry() { retryCount.value += 1 }

    /** FR-REC-02: the real-time listener refreshes the list and the planned total. */
    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch {
            if (repository.setActive(id, active) is Result.Error) {
                _message.value = "Could not change the item. Please try again."
            }
        }
    }

    /** FR-REC-04: creates ONE linked expense (same id every time, so a double tap cannot duplicate it). */
    fun confirm(recurringId: String) {
        val due = (uiState.value as? UiState.Success)?.data?.due?.firstOrNull { it.item.id == recurringId } ?: return
        viewModelScope.launch {
            _message.value = when (val result = confirmRecurring(due, today)) {
                is ConfirmRecurringResult.Confirmed -> "${due.item.name} added to your expenses"
                is ConfirmRecurringResult.Failed -> result.message
            }
        }
    }

    /** FR-REC-03: Skipped applies to the current month only; nothing is recorded. */
    fun skip(recurringId: String) {
        viewModelScope.launch {
            if (repository.setSkippedMonth(recurringId, currentMonthKey) is Result.Error) {
                _message.value = "Could not change the item. Please try again."
            }
        }
    }

    fun undoSkip(recurringId: String) {
        viewModelScope.launch {
            if (repository.setSkippedMonth(recurringId, null) is Result.Error) {
                _message.value = "Could not change the item. Please try again."
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            _message.value = when (repository.deleteRecurring(id)) {
                is Result.Success -> "Recurring item deleted"
                is Result.Error -> "Could not delete the item. Please try again."
            }
        }
    }

    fun messageShown() { _message.value = null }
}
