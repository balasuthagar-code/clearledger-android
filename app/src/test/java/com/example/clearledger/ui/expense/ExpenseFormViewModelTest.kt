package com.example.clearledger.ui.expense

import androidx.lifecycle.SavedStateHandle
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.usecase.SaveExpenseUseCase
import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import com.example.clearledger.navigation.ExpenseRoutes
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.testExpense
import com.example.clearledger.ui.expense.form.ExpenseFormEvent
import com.example.clearledger.ui.expense.form.ExpenseFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FakeCategories : CategoryRepository {
        var seedCalls = 0
        override fun observeCategories(): Flow<List<ExpenseCategory>> = flowOf(
            listOf(
                ExpenseCategory("food", "Food", ExpenseClassification.DISCRETIONARY, sortOrder = 0),
                ExpenseCategory("rent", "Rent", ExpenseClassification.COMMITTED, sortOrder = 1),
                ExpenseCategory("transport", "Transport", ExpenseClassification.DISCRETIONARY, isFavourite = true, sortOrder = 2)
            )
        )
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> { seedCalls++; return Result.Success(Unit) }
    }

    private fun viewModel(
        repo: InMemoryExpenseRepository,
        expenseId: String? = null,
        categories: FakeCategories = FakeCategories()
    ): ExpenseFormViewModel {
        val handle = SavedStateHandle(if (expenseId == null) emptyMap() else mapOf(ExpenseRoutes.ARG_EXPENSE_ID to expenseId))
        return ExpenseFormViewModel(handle, repo, categories, SaveExpenseUseCase(repo, ValidateExpenseInputUseCase()))
    }

    private fun TestScope.collectCategories(vm: ExpenseFormViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.categories.collect {} }
        advanceUntilIdle()
    }

    @Test
    fun addMode_startsEmpty_andSeedsCategories() = runTest {
        val categories = FakeCategories()
        val vm = viewModel(InMemoryExpenseRepository(), categories = categories)
        advanceUntilIdle()

        assertFalse(vm.state.value.isEditing)
        assertFalse(vm.state.value.isLoading)
        assertEquals("", vm.state.value.amountText)
        assertEquals(1, categories.seedCalls)
    }

    @Test
    fun favouriteCategories_areListedFirst() = runTest {
        val vm = viewModel(InMemoryExpenseRepository())
        collectCategories(vm)

        assertEquals(listOf("transport", "food", "rent"), vm.categories.value.map { it.id })
    }

    @Test
    fun save_withEmptyForm_showsFieldErrors_andSavesNothing() = runTest {
        val repo = InMemoryExpenseRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertNotNull(vm.state.value.errors.amount)
        assertNotNull(vm.state.value.errors.category)
        assertTrue(repo.added.isEmpty())
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun quickAdd_amountCategorySave_createsExpense_andEmitsSaved() = runTest {
        val repo = InMemoryExpenseRepository()
        val vm = viewModel(repo)
        collectCategories(vm)

        vm.onAmountChange("450.00")
        vm.onCategorySelected("food")
        vm.save()
        advanceUntilIdle()

        assertEquals(45_000L, repo.added.single().amountMinor)
        assertEquals("food", repo.added.single().categoryId)
        assertEquals(ExpenseFormEvent.Saved, vm.events.first())
    }

    @Test
    fun typingInAField_clearsThatFieldsError() = runTest {
        val vm = viewModel(InMemoryExpenseRepository())
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errors.amount)

        vm.onAmountChange("5")

        assertNull(vm.state.value.errors.amount)
    }

    @Test
    fun pickingCategory_appliesItsDefaultClassification() = runTest {
        val vm = viewModel(InMemoryExpenseRepository())
        collectCategories(vm)

        vm.onCategorySelected("rent")
        assertEquals(ExpenseClassification.COMMITTED, vm.state.value.classification)

        vm.onCategorySelected("food")
        assertEquals(ExpenseClassification.DISCRETIONARY, vm.state.value.classification)
    }

    @Test
    fun userChosenClassification_isNotOverriddenByCategory() = runTest {
        val vm = viewModel(InMemoryExpenseRepository())
        collectCategories(vm)

        vm.onClassificationSelected(ExpenseClassification.COMMITTED)
        vm.onCategorySelected("food")          // food defaults to DISCRETIONARY

        assertEquals(ExpenseClassification.COMMITTED, vm.state.value.classification)
    }

    @Test
    fun saveFailure_showsMessage_andStaysOnForm() = runTest {
        val repo = InMemoryExpenseRepository()
        repo.failWrites = true
        val vm = viewModel(repo)
        collectCategories(vm)
        vm.onAmountChange("10")
        vm.onCategorySelected("food")

        vm.save()
        advanceUntilIdle()

        assertEquals("Could not save the expense. Please try again.", vm.state.value.saveError)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun editMode_loadsTheExpenseIntoTheForm() = runTest {
        val repo = InMemoryExpenseRepository(
            listOf(testExpense(id = "e1", baseMinor = 125_050, categoryId = "rent", merchant = "Landlord", classification = ExpenseClassification.COMMITTED))
        )
        val vm = viewModel(repo, expenseId = "e1")
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.isEditing)
        assertFalse(s.isLoading)
        assertEquals("1250.50", s.amountText)
        assertEquals("rent", s.categoryId)
        assertEquals("Landlord", s.merchant)
        assertEquals(ExpenseClassification.COMMITTED, s.classification)
    }

    @Test
    fun editMode_save_updatesInsteadOfAdding() = runTest {
        val repo = InMemoryExpenseRepository(listOf(testExpense(id = "e1", baseMinor = 100_000, createdAtMillis = 555)))
        val vm = viewModel(repo, expenseId = "e1")
        advanceUntilIdle()

        vm.onAmountChange("75.00")
        vm.save()
        advanceUntilIdle()

        assertTrue(repo.added.isEmpty())
        val updated = repo.updated.single()
        assertEquals(7_500L, updated.amountMinor)
        assertEquals(555L, updated.createdAtMillis)
        assertEquals(ExpenseFormEvent.Saved, vm.events.first())
    }

    @Test
    fun editMode_keepsSavedClassification_whenCategoryChanges() = runTest {
        val repo = InMemoryExpenseRepository(listOf(testExpense(id = "e1", categoryId = "rent", classification = ExpenseClassification.COMMITTED)))
        val vm = viewModel(repo, expenseId = "e1")
        collectCategories(vm)

        vm.onCategorySelected("food")

        assertEquals(ExpenseClassification.COMMITTED, vm.state.value.classification)
    }

    @Test
    fun editMode_unknownExpense_showsLoadError() = runTest {
        val vm = viewModel(InMemoryExpenseRepository(), expenseId = "missing")
        advanceUntilIdle()

        assertEquals("This expense no longer exists.", vm.state.value.loadError)
        vm.save()                                  // must be ignored
        advanceUntilIdle()
        assertTrue(vm.state.value.loadError != null)
    }
}
