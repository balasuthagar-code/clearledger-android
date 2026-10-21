package com.example.clearledger.ui.recurring

import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.usecase.CalculatePlannedCommitmentsUseCase
import com.example.clearledger.domain.usecase.ConfirmRecurringUseCase
import com.example.clearledger.domain.usecase.GetDueRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveDueRecurringUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.InMemoryRecurringRepository
import com.example.clearledger.testutil.testRecurring
import com.example.clearledger.ui.recurring.list.RecurringListViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeCategories : CategoryRepository {
        override fun observeCategories(): Flow<List<ExpenseCategory>> =
            flowOf(listOf(ExpenseCategory("rent", "Rent", ExpenseClassification.COMMITTED)))
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = Result.Success(Unit)
        override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> = Result.Success(Unit)
    }

    private fun viewModel(
        repo: InMemoryRecurringRepository,
        expenses: InMemoryExpenseRepository = InMemoryExpenseRepository()
    ) = RecurringListViewModel(
        repo,
        FakeCategories(),
        CalculatePlannedCommitmentsUseCase(),
        ObserveDueRecurringUseCase(repo, expenses, GetDueRecurringUseCase()),
        ConfirmRecurringUseCase(expenses, FakeCategories())
    )

    private fun due(vm: RecurringListViewModel) = (vm.uiState.value as UiState.Success).data.due

    private fun TestScope.collect(vm: RecurringListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.categoryNames.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun noItems_isSuccessWithEmptyList() = runTest {
        val vm = viewModel(InMemoryRecurringRepository())
        collect(vm)

        val content = (vm.uiState.value as UiState.Success).data
        assertTrue(content.items.isEmpty())
        assertEquals(0L, content.planned.monthlyTotalBaseMinor)
    }

    @Test
    fun withItems_showsListAndPlannedTotal() = runTest {
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a", baseMinor = 3_400_000), testRecurring("b", baseMinor = 480_000, name = "Gym"))))
        collect(vm)

        val content = (vm.uiState.value as UiState.Success).data
        assertEquals(2, content.items.size)
        assertEquals(3_880_000L, content.planned.monthlyTotalBaseMinor)
        assertEquals("Rent", vm.categoryNames.value["rent"])
    }

    @Test
    fun deactivating_removesItemFromThePlannedTotal() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("a", baseMinor = 3_400_000), testRecurring("b", baseMinor = 480_000, name = "Gym")))
        val vm = viewModel(repo)
        collect(vm)

        vm.setActive("a", false)
        advanceUntilIdle()

        val content = (vm.uiState.value as UiState.Success).data
        assertEquals(480_000L, content.planned.monthlyTotalBaseMinor)
        assertEquals(2, content.items.size)                 // still listed, just inactive
    }

    @Test
    fun setActiveFailure_showsMessage() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("a")))
        repo.failWrites = true
        val vm = viewModel(repo)
        collect(vm)

        vm.setActive("a", false)
        advanceUntilIdle()

        assertEquals("Could not change the item. Please try again.", vm.message.value)
    }

    @Test
    fun delete_removesItem_andShowsMessage() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("a"), testRecurring("b", name = "Gym")))
        val vm = viewModel(repo)
        collect(vm)

        vm.deleteItem("a")
        advanceUntilIdle()

        assertEquals(listOf("a"), repo.deletedIds)
        assertEquals("Recurring item deleted", vm.message.value)
        assertEquals(1, (vm.uiState.value as UiState.Success).data.items.size)

        vm.messageShown()
        assertNull(vm.message.value)
    }

    @Test
    fun deleteFailure_keepsItem_andShowsError() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("a")))
        repo.failWrites = true
        val vm = viewModel(repo)
        collect(vm)

        vm.deleteItem("a")
        advanceUntilIdle()

        assertEquals("Could not delete the item. Please try again.", vm.message.value)
        assertEquals(1, (vm.uiState.value as UiState.Success).data.items.size)
    }

    // ---------- FR-REC-03 / FR-REC-04: due tracking and confirmation ----------

    @Test
    fun monthlyItems_areListedAsPendingDue() = runTest {
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"), testRecurring("b", name = "Gym", dueDay = 5))))
        collect(vm)

        val due = due(vm)
        assertEquals(listOf("a", "b"), due.map { it.item.id })
        assertTrue(due.all { it.status == DueStatus.PENDING })
    }

    @Test
    fun confirm_createsOneLinkedExpense_andMarksTheItemConfirmed() = runTest {
        val expenses = InMemoryExpenseRepository()
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a", baseMinor = 3_400_000))), expenses)
        collect(vm)

        vm.confirm("a")
        advanceUntilIdle()

        val expense = expenses.upserted.single()
        assertEquals("a", expense.recurringId)
        assertEquals(3_400_000L, expense.baseAmountMinor)
        assertEquals(DueStatus.CONFIRMED, due(vm).single().status)
        assertEquals("Rent added to your expenses", vm.message.value)
    }

    @Test
    fun confirmingTwice_neverCreatesADuplicate() = runTest {
        val expenses = InMemoryExpenseRepository()
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"))), expenses)
        collect(vm)

        vm.confirm("a")
        vm.confirm("a")
        advanceUntilIdle()

        assertEquals(1, expenses.data.value.size)
    }

    @Test
    fun confirmFailure_showsMessage_andTheItemStaysPending() = runTest {
        val expenses = InMemoryExpenseRepository()
        expenses.failWrites = true
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"))), expenses)
        collect(vm)

        vm.confirm("a")
        advanceUntilIdle()

        assertEquals("Could not save the expense. Please try again.", vm.message.value)
        assertEquals(DueStatus.PENDING, due(vm).single().status)
    }

    @Test
    fun skip_marksTheItemSkipped_recordsNoExpense_andUndoRestoresPending() = runTest {
        val expenses = InMemoryExpenseRepository()
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"))), expenses)
        collect(vm)

        vm.skip("a")
        advanceUntilIdle()
        assertEquals(DueStatus.SKIPPED, due(vm).single().status)
        assertTrue(expenses.data.value.isEmpty())

        vm.undoSkip("a")
        advanceUntilIdle()
        assertEquals(DueStatus.PENDING, due(vm).single().status)
    }

    @Test
    fun skipFailure_showsMessage() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("a")))
        val vm = viewModel(repo)
        collect(vm)
        repo.failWrites = true

        vm.skip("a")
        advanceUntilIdle()

        assertEquals("Could not change the item. Please try again.", vm.message.value)
    }

    @Test
    fun deletingTheLinkedExpense_makesTheItemPendingAgain() = runTest {
        val expenses = InMemoryExpenseRepository()
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"))), expenses)
        collect(vm)
        vm.confirm("a")
        advanceUntilIdle()
        assertEquals(DueStatus.CONFIRMED, due(vm).single().status)

        expenses.deleteExpense(due(vm).single().expenseId!!)
        advanceUntilIdle()

        assertEquals(DueStatus.PENDING, due(vm).single().status)
    }

    @Test
    fun confirmingAnUnknownItem_doesNothing() = runTest {
        val expenses = InMemoryExpenseRepository()
        val vm = viewModel(InMemoryRecurringRepository(listOf(testRecurring("a"))), expenses)
        collect(vm)

        vm.confirm("missing")
        advanceUntilIdle()

        assertTrue(expenses.upserted.isEmpty())
        assertNull(vm.message.value)
    }

    @Test
    fun repositoryFailure_isErrorState() = runTest {
        val failing = object : InMemoryRecurringRepository() {
            override fun observeRecurring(): Flow<List<RecurringItem>> = flow { throw RuntimeException("boom") }
        }
        val vm = viewModel(failing)
        collect(vm)

        assertTrue(vm.uiState.value is UiState.Error)
    }
}
