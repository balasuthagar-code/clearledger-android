package com.example.clearledger.ui.expense

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.usecase.GroupExpensesByDateUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.testExpense
import com.example.clearledger.ui.expense.list.ExpenseListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class ExpenseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeCategories : CategoryRepository {
        override fun observeCategories(): Flow<List<ExpenseCategory>> = flowOf(
            listOf(
                ExpenseCategory("food", "Food", ExpenseClassification.DISCRETIONARY, sortOrder = 0),
                ExpenseCategory("rent", "Rent", ExpenseClassification.COMMITTED, sortOrder = 1)
            )
        )
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = Result.Success(Unit)
    }

    private fun thisMonth(id: String, base: Long, category: String = "food") = testExpense(
        id = id, baseMinor = base, categoryId = category, date = LocalDate.now(), monthKey = MonthKey.current()
    )

    private fun viewModel(repo: InMemoryExpenseRepository) =
        ExpenseListViewModel(repo, FakeCategories(), GroupExpensesByDateUseCase())

    private fun TestScope.collect(vm: ExpenseListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.categories.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun noExpenses_isSuccessWithMonthEmpty() = runTest {
        val vm = viewModel(InMemoryExpenseRepository())
        collect(vm)

        val content = (vm.uiState.value as UiState.Success).data
        assertTrue(content.groups.isEmpty())
        assertEquals(false, content.monthHasExpenses)
    }

    @Test
    fun withExpenses_groupsAndTotals() = runTest {
        val vm = viewModel(InMemoryExpenseRepository(listOf(thisMonth("a", 100_000), thisMonth("b", 250_000))))
        collect(vm)

        val content = (vm.uiState.value as UiState.Success).data
        assertEquals(1, content.groups.size)
        assertEquals(2, content.shownCount)
        assertEquals(350_000L, content.shownTotalMinor)
    }

    @Test
    fun categoryFilter_narrowsList_andClearingRestoresIt() = runTest {
        val vm = viewModel(
            InMemoryExpenseRepository(listOf(thisMonth("a", 100_000, "food"), thisMonth("b", 400_000, "rent")))
        )
        collect(vm)

        vm.selectCategory("rent")
        advanceUntilIdle()
        var content = (vm.uiState.value as UiState.Success).data
        assertEquals(400_000L, content.shownTotalMinor)
        assertEquals(true, content.monthHasExpenses)

        vm.selectCategory(null)
        advanceUntilIdle()
        content = (vm.uiState.value as UiState.Success).data
        assertEquals(500_000L, content.shownTotalMinor)
    }

    @Test
    fun filterWithNoMatch_keepsMonthHasExpensesTrue() = runTest {
        val vm = viewModel(InMemoryExpenseRepository(listOf(thisMonth("a", 100_000, "food"))))
        collect(vm)

        vm.selectCategory("rent")
        advanceUntilIdle()

        val content = (vm.uiState.value as UiState.Success).data
        assertTrue(content.groups.isEmpty())
        assertEquals(true, content.monthHasExpenses)
    }

    @Test
    fun monthNavigation_changesTheMonthShown() = runTest {
        val vm = viewModel(InMemoryExpenseRepository(listOf(thisMonth("a", 100_000))))
        collect(vm)

        vm.showPreviousMonth()
        advanceUntilIdle()

        assertEquals(MonthKey.previous(MonthKey.current()), vm.selectedMonth.value)
        assertTrue((vm.uiState.value as UiState.Success).data.groups.isEmpty())
    }

    @Test
    fun repositoryFailure_isErrorState() = runTest {
        val failing = object : InMemoryExpenseRepository() {
            override fun observeExpensesForMonth(monthKey: String): Flow<List<Expense>> =
                flow { throw RuntimeException("boom") }
        }
        val vm = viewModel(failing)
        collect(vm)

        assertTrue(vm.uiState.value is UiState.Error)
    }

    @Test
    fun delete_removesExpense_andShowsMessage() = runTest {
        val repo = InMemoryExpenseRepository(listOf(thisMonth("a", 100_000), thisMonth("b", 250_000)))
        val vm = viewModel(repo)
        collect(vm)

        vm.deleteExpense("a")
        advanceUntilIdle()

        assertEquals(listOf("a"), repo.deletedIds)
        assertEquals("Expense deleted", vm.message.value)
        assertEquals(250_000L, (vm.uiState.value as UiState.Success).data.shownTotalMinor)

        vm.messageShown()
        assertNull(vm.message.value)
    }

    @Test
    fun delete_failure_showsErrorMessage_andKeepsExpense() = runTest {
        val repo = InMemoryExpenseRepository(listOf(thisMonth("a", 100_000)))
        repo.failWrites = true
        val vm = viewModel(repo)
        collect(vm)

        vm.deleteExpense("a")
        advanceUntilIdle()

        assertEquals("Could not delete the expense. Please try again.", vm.message.value)
        assertEquals(100_000L, (vm.uiState.value as UiState.Success).data.shownTotalMinor)
    }
}
