package com.example.clearledger.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.PrimaryGoalSource
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
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val categoryRepository: CategoryRepository,
    primaryGoalSource: PrimaryGoalSource
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

    init {
        // Makes sure the default categories exist. Idempotent; Member 1's onboarding can take this over.
        viewModelScope.launch { categoryRepository.seedDefaultCategoriesIfEmpty() }
    }

    fun showPreviousMonth() { _selectedMonth.value = MonthKey.previous(_selectedMonth.value) }
    fun showNextMonth() { _selectedMonth.value = MonthKey.next(_selectedMonth.value) }
    fun retry() { retryCount.value += 1 }
}
