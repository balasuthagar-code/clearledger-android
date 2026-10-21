package com.example.clearledger.ui.recurring

import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.usecase.CalculatePlannedCommitmentsUseCase
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
    }

    private fun viewModel(repo: InMemoryRecurringRepository) =
        RecurringListViewModel(repo, FakeCategories(), CalculatePlannedCommitmentsUseCase())

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
