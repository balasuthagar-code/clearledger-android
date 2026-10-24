package com.example.clearledger.ui.expense

import androidx.lifecycle.SavedStateHandle
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.model.MerchantSuggestion
import com.example.clearledger.domain.usecase.SaveExpenseUseCase
import com.example.clearledger.domain.usecase.SuggestMerchantsUseCase
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
        val favouriteCalls = mutableListOf<Pair<String, Boolean>>()
        var failFavourite = false
        override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> {
            if (failFavourite) return Result.Error(Exception("nope"))
            favouriteCalls += id to favourite
            return Result.Success(Unit)
        }
    }

    private fun viewModel(
        repo: InMemoryExpenseRepository,
        expenseId: String? = null,
        categories: FakeCategories = FakeCategories()
    ): ExpenseFormViewModel {
        val handle = SavedStateHandle(if (expenseId == null) emptyMap() else mapOf(ExpenseRoutes.ARG_EXPENSE_ID to expenseId))
        return ExpenseFormViewModel(
            handle, repo, categories,
            SaveExpenseUseCase(repo, ValidateExpenseInputUseCase()),
            SuggestMerchantsUseCase()
        )
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

    // ---------- FR-EXP-08: merchant memory ----------

    private fun TestScope.collectSuggestions(vm: ExpenseFormViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.merchantSuggestions.collect {} }
        advanceUntilIdle()
    }

    private fun pastExpenses() = InMemoryExpenseRepository(
        listOf(
            testExpense(id = "1", merchant = "PickMe", categoryId = "transport"),
            testExpense(id = "2", merchant = "pickme", categoryId = "transport"),
            testExpense(id = "3", merchant = "Keells", categoryId = "food"),
            testExpense(id = "4", merchant = "", categoryId = "food")
        )
    )

    @Test
    fun merchantSuggestions_comeFromRecentExpenses_mostUsedFirst() = runTest {
        val vm = viewModel(pastExpenses())
        collectSuggestions(vm)

        assertEquals(listOf("pickme", "keells"), vm.merchantSuggestions.value.map { it.name.lowercase() })
        assertEquals(2, vm.merchantSuggestions.value.first().useCount)
    }

    @Test
    fun typingAMerchant_narrowsTheSuggestions() = runTest {
        val vm = viewModel(pastExpenses())
        collectSuggestions(vm)

        vm.onMerchantChange("kee")
        advanceUntilIdle()

        assertEquals(listOf("Keells"), vm.merchantSuggestions.value.map { it.name })
    }

    @Test
    fun selectingASuggestion_fillsMerchant_andPrefillsTheUsualCategory() = runTest {
        val vm = viewModel(pastExpenses())
        collectCategories(vm)
        collectSuggestions(vm)

        vm.onMerchantSuggestionSelected(MerchantSuggestion("PickMe", "transport", 2))

        assertEquals("PickMe", vm.state.value.merchant)
        assertEquals("transport", vm.state.value.categoryId)
        assertEquals(ExpenseClassification.DISCRETIONARY, vm.state.value.classification)
    }

    @Test
    fun selectingASuggestion_prefillsTheCategorysDefaultClassification() = runTest {
        val vm = viewModel(pastExpenses())
        collectCategories(vm)

        vm.onMerchantSuggestionSelected(MerchantSuggestion("Landlord", "rent", 3))

        assertEquals(ExpenseClassification.COMMITTED, vm.state.value.classification)
    }

    @Test
    fun selectingASuggestion_neverReplacesACategoryTheUserAlreadyPicked() = runTest {
        val vm = viewModel(pastExpenses())
        collectCategories(vm)
        vm.onCategorySelected("food")

        vm.onMerchantSuggestionSelected(MerchantSuggestion("PickMe", "transport", 2))

        assertEquals("PickMe", vm.state.value.merchant)
        assertEquals("food", vm.state.value.categoryId)
    }

    @Test
    fun selectingASuggestionWithoutACategory_onlyFillsTheMerchant() = runTest {
        val vm = viewModel(pastExpenses())
        collectCategories(vm)

        vm.onMerchantSuggestionSelected(MerchantSuggestion("Someone", "", 1))

        assertEquals("Someone", vm.state.value.merchant)
        assertNull(vm.state.value.categoryId)
    }

    @Test
    fun savingWithASuggestedMerchant_storesTheMerchantAndCategory() = runTest {
        val repo = pastExpenses()
        val vm = viewModel(repo)
        collectCategories(vm)

        vm.onAmountChange("250")
        vm.onMerchantSuggestionSelected(MerchantSuggestion("PickMe", "transport", 2))
        vm.save()
        advanceUntilIdle()

        val saved = repo.added.single()
        assertEquals("PickMe", saved.merchant)
        assertEquals("transport", saved.categoryId)
    }

    // ---------- FR-EXP-09: favourite categories ----------

    @Test
    fun toggleFavourite_starsTheSelectedCategory() = runTest {
        val categories = FakeCategories()
        val vm = viewModel(InMemoryExpenseRepository(), categories = categories)
        collectCategories(vm)

        vm.onCategorySelected("food")           // not a favourite yet
        vm.toggleFavourite()
        advanceUntilIdle()

        assertEquals(listOf("food" to true), categories.favouriteCalls)
    }

    @Test
    fun toggleFavourite_unstarsAFavouriteCategory() = runTest {
        val categories = FakeCategories()
        val vm = viewModel(InMemoryExpenseRepository(), categories = categories)
        collectCategories(vm)

        vm.onCategorySelected("transport")      // already a favourite in the fake
        vm.toggleFavourite()
        advanceUntilIdle()

        assertEquals(listOf("transport" to false), categories.favouriteCalls)
    }

    @Test
    fun toggleFavourite_withoutASelectedCategory_doesNothing() = runTest {
        val categories = FakeCategories()
        val vm = viewModel(InMemoryExpenseRepository(), categories = categories)
        collectCategories(vm)

        vm.toggleFavourite()
        advanceUntilIdle()

        assertTrue(categories.favouriteCalls.isEmpty())
    }

    @Test
    fun toggleFavourite_failure_showsAMessage() = runTest {
        val categories = FakeCategories().apply { failFavourite = true }
        val vm = viewModel(InMemoryExpenseRepository(), categories = categories)
        collectCategories(vm)
        vm.onCategorySelected("food")

        vm.toggleFavourite()
        advanceUntilIdle()

        assertEquals("Could not update your favourite categories. Please try again.", vm.state.value.saveError)
    }
}
