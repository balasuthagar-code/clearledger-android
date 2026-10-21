package com.example.clearledger.ui.dashboard

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.IncomeSummarySource
import com.example.clearledger.domain.repository.PrimaryGoalSource
import com.example.clearledger.domain.usecase.CalculateMonthlySummaryUseCase
import com.example.clearledger.domain.usecase.CalculateSafeToSpendUseCase
import com.example.clearledger.domain.usecase.GetDueRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveDueRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveMonthlySummaryUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.InMemoryRecurringRepository
import com.example.clearledger.testutil.testRecurring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> = Result.Success(Unit)
    }

    private class FakeGoal(private val goal: GoalSummary? = null) : PrimaryGoalSource {
        override fun observePrimaryGoal(): Flow<GoalSummary?> = flowOf(goal)
    }

    private class FailingGoal : PrimaryGoalSource {
        override fun observePrimaryGoal(): Flow<GoalSummary?> = flow { throw RuntimeException("goal boom") }
    }

    private fun viewModel(
        expenses: ExpenseRepository,
        income: IncomeSummarySource = FakeIncome(),
        categories: CategoryRepository = FakeCategories(),
        goal: PrimaryGoalSource = FakeGoal(),
        recurring: InMemoryRecurringRepository = InMemoryRecurringRepository()
    ) = DashboardViewModel(
        ObserveMonthlySummaryUseCase(expenses, income, CalculateMonthlySummaryUseCase()),
        categories,
        goal,
        ObserveDueRecurringUseCase(recurring, expenses, GetDueRecurringUseCase()),
        CalculateSafeToSpendUseCase()
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

    @Test
    fun primaryGoal_isNullWhenNoGoalExists() = runTest {
        val vm = viewModel(FakeExpenses())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.primaryGoal.collect {} }
        advanceUntilIdle()

        assertEquals(null, vm.primaryGoal.value)
    }

    @Test
    fun primaryGoal_isExposedToTheDashboard() = runTest {
        val goal = GoalSummary("MacBook Pro M4", savedMinor = 9_800_000, targetMinor = 49_000_000)
        val vm = viewModel(FakeExpenses(), goal = FakeGoal(goal))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.primaryGoal.collect {} }
        advanceUntilIdle()

        assertEquals(goal, vm.primaryGoal.value)
        assertEquals(20, vm.primaryGoal.value!!.progressPercent)
    }

    // ---------- FR-DASH-08 / BR-10: safe to spend ----------

    private fun TestScope.collectSafe(vm: DashboardViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.safeToSpend.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun safeToSpend_subtractsExpenses_pendingRecurring_andGoalSaving() = runTest {
        val goal = GoalSummary("MacBook", savedMinor = 0, targetMinor = 49_000_000, requiredMonthlyMinor = 4_083_334)
        val vm = viewModel(
            FakeExpenses(listOf(expenseThisMonth(500_000))),
            FakeIncome(listOf(13_000_000L)),
            goal = FakeGoal(goal),
            recurring = InMemoryRecurringRepository(listOf(testRecurring("r1", baseMinor = 3_400_000)))
        )
        collectSafe(vm)

        val safe = vm.safeToSpend.value!!
        assertEquals(13_000_000L, safe.incomeMinor)
        assertEquals(500_000L, safe.expensesMinor)
        assertEquals(3_400_000L, safe.pendingCommitmentsMinor)
        assertEquals(4_083_334L, safe.goalSavingMinor)
        assertEquals(13_000_000L - 500_000L - 3_400_000L - 4_083_334L, safe.amountMinor)
    }

    @Test
    fun safeToSpend_doesNotCountConfirmedOrSkippedItems() = runTest {
        val confirmed = expenseThisMonth(3_400_000).copy(id = "paid", recurringId = "r1")
        val vm = viewModel(
            FakeExpenses(listOf(confirmed)),
            FakeIncome(listOf(13_000_000L)),
            recurring = InMemoryRecurringRepository(
                listOf(
                    testRecurring("r1", baseMinor = 3_400_000),                                            // confirmed
                    testRecurring("r2", name = "Gym", baseMinor = 480_000, skippedMonthKey = MonthKey.current()) // skipped
                )
            )
        )
        collectSafe(vm)

        val safe = vm.safeToSpend.value!!
        assertEquals(0L, safe.pendingCommitmentsMinor)
        assertEquals(13_000_000L - 3_400_000L, safe.amountMinor)   // the confirmed rent is already in the expenses
    }

    @Test
    fun safeToSpend_withoutAGoal_hasNoGoalSaving() = runTest {
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(500_000))), FakeIncome(listOf(1_000_000L)))
        collectSafe(vm)

        assertEquals(0L, vm.safeToSpend.value!!.goalSavingMinor)
    }

    @Test
    fun safeToSpend_isNegative_whenOverBudget() = runTest {
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(900_000))), FakeIncome(listOf(500_000L)))
        collectSafe(vm)

        val safe = vm.safeToSpend.value!!
        assertTrue(safe.isOverBudget)
        assertEquals(-400_000L, safe.amountMinor)
    }

    @Test
    fun safeToSpend_isHiddenForOtherMonths_andForAnEmptyMonth() = runTest {
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(500_000))), FakeIncome(listOf(1_000_000L)))
        collectSafe(vm)
        assertTrue(vm.safeToSpend.value != null)

        vm.showPreviousMonth()
        advanceUntilIdle()
        assertNull(vm.safeToSpend.value)

        val emptyVm = viewModel(FakeExpenses(), FakeIncome())
        collectSafe(emptyVm)
        assertNull(emptyVm.safeToSpend.value)
    }

    @Test
    fun safeToSpend_isHidden_whenRecurringItemsCannotBeLoaded() = runTest {
        val failingRecurring = object : InMemoryRecurringRepository() {
            override fun observeRecurring(): Flow<List<com.example.clearledger.domain.model.RecurringItem>> =
                flow { throw RuntimeException("boom") }
        }
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(500_000))), FakeIncome(listOf(1_000_000L)), recurring = failingRecurring)
        collectSafe(vm)

        assertNull(vm.safeToSpend.value)                     // never show an optimistic number
        assertTrue(vm.uiState.value is UiState.Success)      // the rest of the dashboard still works
    }

    @Test
    fun goalFailure_doesNotBreakTheDashboard() = runTest {
        val vm = viewModel(FakeExpenses(listOf(expenseThisMonth(500_000))), goal = FailingGoal())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.primaryGoal.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(null, vm.primaryGoal.value)
        assertTrue(vm.uiState.value is UiState.Success)
    }
}
