package com.example.clearledger.ui.recurring

import com.example.clearledger.currency.testRateRecorder

import androidx.lifecycle.SavedStateHandle
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.usecase.SaveRecurringUseCase
import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import com.example.clearledger.domain.usecase.ValidateRecurringInputUseCase
import com.example.clearledger.navigation.RecurringRoutes
import com.example.clearledger.testutil.InMemoryRecurringRepository
import com.example.clearledger.testutil.testRecurring
import com.example.clearledger.ui.recurring.form.RecurringFormEvent
import com.example.clearledger.ui.recurring.form.RecurringFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class RecurringFormViewModelTest {

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

    private fun viewModel(repo: InMemoryRecurringRepository, id: String? = null): RecurringFormViewModel {
        val handle = SavedStateHandle(if (id == null) emptyMap() else mapOf(RecurringRoutes.ARG_RECURRING_ID to id))
        return RecurringFormViewModel(
            handle, repo, FakeCategories(),
            SaveRecurringUseCase(repo, ValidateRecurringInputUseCase(ValidateExpenseInputUseCase())), testRateRecorder()
        )
    }

    @Test
    fun addMode_startsWithMonthlyDayOneActive() = runTest {
        val vm = viewModel(InMemoryRecurringRepository())
        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.isEditing)
        assertFalse(s.isLoading)
        assertEquals(RecurringFrequency.MONTHLY, s.frequency)
        assertEquals("1", s.dueDayText)
        assertTrue(s.isActive)
    }

    @Test
    fun save_withEmptyForm_showsErrors_andSavesNothing() = runTest {
        val repo = InMemoryRecurringRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        assertNotNull(vm.state.value.errors.name)
        assertNotNull(vm.state.value.errors.amount)
        assertNotNull(vm.state.value.errors.category)
        assertTrue(repo.added.isEmpty())
    }

    @Test
    fun typingInAField_clearsThatFieldsError() = runTest {
        val vm = viewModel(InMemoryRecurringRepository())
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.errors.name)

        vm.onNameChange("R")

        assertNull(vm.state.value.errors.name)
    }

    @Test
    fun validForm_savesItem_andEmitsSaved() = runTest {
        val repo = InMemoryRecurringRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.onNameChange("Rent")
        vm.onAmountChange("34000")
        vm.onCategorySelected("rent")
        vm.save()
        advanceUntilIdle()

        val item = repo.added.single()
        assertEquals("Rent", item.name)
        assertEquals(3_400_000L, item.amountMinor)
        assertEquals(RecurringFormEvent.Saved, vm.events.first())
    }

    @Test
    fun changingFrequency_resetsTheDueDay() = runTest {
        val vm = viewModel(InMemoryRecurringRepository())
        advanceUntilIdle()
        vm.onDueDayChange("25")

        vm.onFrequencyChange(RecurringFrequency.WEEKLY)

        assertEquals("1", vm.state.value.dueDayText)
        assertEquals(RecurringFrequency.WEEKLY, vm.state.value.frequency)
    }

    @Test
    fun saveFailure_showsMessage() = runTest {
        val repo = InMemoryRecurringRepository()
        repo.failWrites = true
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onNameChange("Rent"); vm.onAmountChange("100"); vm.onCategorySelected("rent")

        vm.save()
        advanceUntilIdle()

        assertEquals("Could not save the item. Please try again.", vm.state.value.saveError)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun editMode_loadsTheItemIntoTheForm() = runTest {
        val repo = InMemoryRecurringRepository(
            listOf(testRecurring("r1", name = "Domain", baseMinor = 1_200_000, frequency = RecurringFrequency.YEARLY, dueDay = 15, dueMonth = 6, isActive = false))
        )
        val vm = viewModel(repo, id = "r1")
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(s.isEditing)
        assertFalse(s.isLoading)
        assertEquals("Domain", s.name)
        assertEquals("12000.00", s.amountText)
        assertEquals(RecurringFrequency.YEARLY, s.frequency)
        assertEquals("15", s.dueDayText)
        assertEquals("6", s.dueMonthText)
        assertFalse(s.isActive)
    }

    @Test
    fun editMode_save_updatesInsteadOfAdding() = runTest {
        val repo = InMemoryRecurringRepository(listOf(testRecurring("r1", baseMinor = 3_400_000)))
        val vm = viewModel(repo, id = "r1")
        advanceUntilIdle()

        vm.onAmountChange("35000")
        vm.save()
        advanceUntilIdle()

        assertTrue(repo.added.isEmpty())
        assertEquals(3_500_000L, repo.updated.single().amountMinor)
        assertEquals(RecurringFormEvent.Saved, vm.events.first())
    }

    @Test
    fun editMode_unknownItem_showsLoadError_andIgnoresSave() = runTest {
        val vm = viewModel(InMemoryRecurringRepository(), id = "missing")
        advanceUntilIdle()

        assertEquals("This item no longer exists.", vm.state.value.loadError)
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.loadError != null && !vm.state.value.isSaving)
    }
}
