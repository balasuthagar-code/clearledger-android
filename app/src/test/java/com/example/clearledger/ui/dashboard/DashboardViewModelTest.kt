package com.example.clearledger.ui.dashboard

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.IncomeSummarySource
import com.example.clearledger.domain.usecase.CalculateMonthlySummaryUseCase
import com.example.clearledger.domain.usecase.ObserveMonthlySummaryUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ---- small fakes for the things the ViewModel depends on ----
    private class FakeExpenses(initial: List<Expense> = emptyList()) : InMemoryExpenseRepository(initial)

    private class FailingExpenses : InMemoryExpenseRepository() {
        override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> =
            flow { throw RuntimeException("boom") }
    }

    private class FakeIncome(private val amounts: List<Long> = emptyList()) : IncomeSummarySource {
        override fun observeReceivedBaseAmounts(monthKey: String): Flow<List<Long>> = flowOf(amounts)
    }

    private class FakeCategories : CategoryRepository {
        var seedCalls = 0
        override fun observeCategories(): Flow<List<ExpenseCategory>> = flowOf(emptyList())
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> {
            seedCalls++
            return Result.Success(Unit)
        }
    }

    private fun viewModel(
        expenses: ExpenseRepository,
        income: IncomeSummarySource = FakeIncome(),
        categories: CategoryRepository = FakeCategories()
    ) = DashboardViewModel(
        ObserveMonthlySummaryUseCase(expenses, income, CalculateMonthlySummaryUseCase()),
        categories
    )

    private fun expenseThisMonth(base: Long) = Expense(
        id = "e$base", amountMinor = base, currency = "LKR", exchangeRate = "1",
        baseAmountMinor = base, categoryId = "food",
        classification = ExpenseClassification.DISCRETIONARY,
        date = LocalDate.now(), monthKey = MonthKey.current()
    )

    @Test
    fun noData_emitsSuccessWithEmptySummary() = runTest {
        val vm = viewModel(FakeExpenses())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.isEmpty)
    }

    @Test
    fun withData_emitsCalculatedSummary() = runTest {
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(500_000))), FakeIncome(listOf(13_000_000L)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val summary = (vm.uiState.value as UiState.Success).data
        assertEquals(13_000_000L, summary.totalIncomeMinor)
        assertEquals(500_000L, summary.totalExpensesMinor)
        assertEquals(12_500_000L, summary.remainingMinor)
    }

    @Test
    fun repositoryFailure_emitsErrorState() = runTest {
        val vm = viewModel(FailingExpenses())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value is UiState.Error)
    }

    @Test
    fun monthNavigation_updatesSelectedMonth() = runTest {
        val vm = viewModel(FakeExpenses())
        val current = MonthKey.current()

        vm.showPreviousMonth()
        assertEquals(MonthKey.previous(current), vm.selectedMonth.value)

        vm.showNextMonth()
        vm.showNextMonth()
        assertEquals(MonthKey.next(current), vm.selectedMonth.value)
    }

    @Test
    fun init_seedsDefaultCategoriesOnce() = runTest {
        val categories = FakeCategories()
        viewModel(FakeExpenses(), categories = categories)
        advanceUntilIdle()

        assertEquals(1, categories.seedCalls)
    }
}
