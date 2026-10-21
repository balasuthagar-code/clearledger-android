package com.example.clearledger.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.model.SafeToSpend
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.PrimaryGoalSource
import com.example.clearledger.domain.usecase.CalculateSafeToSpendUseCase
import com.example.clearledger.domain.usecase.ObserveDueRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveMonthlySummaryUseCase
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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val categoryRepository: CategoryRepository,
    primaryGoalSource: PrimaryGoalSource,
    observeDueRecurring: ObserveDueRecurringUseCase,
    private val calculateSafeToSpend: CalculateSafeToSpendUseCase
) : ViewModel() {

    // Mutable state stays private; the UI only sees read-only StateFlows (assignment rule).
    private val _selectedMonth = MutableStateFlow(MonthKey.current())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val retryCount = MutableStateFlow(0)

    /**
     * Uses the team's shared UiState. "Empty" is not a separate state: the screen shows its
     * empty message when Success.data.isEmpty is true.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<MonthlySummary>> =
        combine(_selectedMonth, retryCount) { month, _ -> month }
            .flatMapLatest { month ->
                observeMonthlySummary(month)
                    .map<MonthlySummary, UiState<MonthlySummary>> { UiState.Success(it) }
                    .onStart { emit(UiState.Loading) }
                    .catch {
                        emit(UiState.Error("Could not load this month. Check your connection and try again."))
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** categoryId -> display name, so "food" is shown as "Food". Empty map if it cannot load. */
    val categoryNames: StateFlow<Map<String, String>> =
        categoryRepository.observeCategories()
            .map { list -> list.associate { it.id to it.name } }
            .catch { emit(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** FR-DASH-05: the primary goal's progress, or null (no goal yet / goals module not connected). */
    val primaryGoal: StateFlow<GoalSummary?> =
        primaryGoalSource.observePrimaryGoal()
            .catch { emit(null) }                       // a goal problem must never break the dashboard
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Recurring items due this month that are still PENDING (not confirmed, not skipped), as a base-currency
     * total. null = not known yet or could not be loaded - the Safe-to-spend card is then hidden rather than
     * showing an optimistic number.
     */
    private val pendingCommitmentsMinor: StateFlow<Long?> =
        observeDueRecurring(LocalDate.now())
            .map<List<DueRecurring>, Long?> { due ->
                due.filter { it.status == DueStatus.PENDING }.sumOf { it.item.baseAmountMinor }
            }
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val currentMonth = MonthKey.current()

    /**
     * FR-DASH-08 / BR-10. Only meaningful for the CURRENT month ("rest of the month"), so it is null for
     * past or future months, while loading, and when the month has no records.
     */
    val safeToSpend: StateFlow<SafeToSpend?> =
        combine(uiState, _selectedMonth, pendingCommitmentsMinor, primaryGoal) { state, month, pending, goal ->
            if (state !is UiState.Success || month != currentMonth || pending == null || state.data.isEmpty) {
                null
            } else {
                calculateSafeToSpend(state.data, pending, goal?.requiredMonthlyMinor ?: 0L)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Makes sure the default categories exist. Idempotent; Member 1's onboarding can take this over.
        viewModelScope.launch { categoryRepository.seedDefaultCategoriesIfEmpty() }
    }

    fun showPreviousMonth() { _selectedMonth.value = MonthKey.previous(_selectedMonth.value) }
    fun showNextMonth() { _selectedMonth.value = MonthKey.next(_selectedMonth.value) }
    fun retry() { retryCount.value += 1 }
}
