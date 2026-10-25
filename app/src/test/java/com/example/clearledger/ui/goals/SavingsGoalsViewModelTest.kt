package com.example.clearledger.ui.goals

import com.example.clearledger.data.repository.SavingsGoalRepository
import com.example.clearledger.domain.model.SavingsGoal
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsGoalsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepository : SavingsGoalRepository {
        val saved = mutableListOf<SavingsGoal>()

        override fun getSavingsGoals(): Flow<List<SavingsGoal>> =
            flowOf(emptyList())

        override suspend fun addSavingsGoal(goal: SavingsGoal) {
            saved += goal
        }
    }

    private fun create(
        viewModel: SavingsGoalsViewModel,
        target: String = "100.29",
        starting: String = "0.29",
        currency: String = "USD",
        name: String = "Laptop",
        deadline: String = LocalDate.now().plusDays(30).toString()
    ) {
        viewModel.createGoal(
            name = name,
            targetAmount = target,
            currency = currency,
            deadline = deadline,
            startingAmount = starting
        )
    }

    @Test
    fun savesExactTargetAndStartingAmounts() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        create(viewModel)
        runCurrent()

        val goal = repository.saved.single()
        assertEquals(10_029L, goal.targetAmountMinor)
        assertEquals(29L, goal.startingSavedMinor)
        assertEquals("USD", goal.currency)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun allowsZeroStartingAmount() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        create(viewModel, starting = "0")
        runCurrent()

        assertEquals(0L, repository.saved.single().startingSavedMinor)
    }

    @Test
    fun rejectsInvalidTargetAmountsWithoutSaving() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        for (target in listOf(
            "1.234", "0", "-1", "abc", "92233720368547758.08"
        )) {
            create(viewModel, target = target, starting = "0")
            runCurrent()

            assertTrue(repository.saved.isEmpty())
            assertNotNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun rejectsInvalidStartingAmountsWithoutSaving() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        for (starting in listOf(
            "0.001", "-1", "101", "abc", "92233720368547758.08"
        )) {
            create(viewModel, target = "100", starting = starting)
            runCurrent()

            assertTrue(repository.saved.isEmpty())
            assertNotNull(viewModel.uiState.value.error)
        }
    }

    @Test
    fun comparesLargeAmountsExactly() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        create(
            viewModel,
            target = "90071992547409.92",
            starting = "90071992547409.93"
        )
        runCurrent()

        assertTrue(repository.saved.isEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun preservesNameCurrencyAndDeadlineValidation() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = SavingsGoalsViewModel(repository)
        runCurrent()

        create(viewModel, name = "")
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)

        create(viewModel, currency = "JPY")
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)

        create(viewModel, deadline = LocalDate.now().toString())
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)

        assertTrue(repository.saved.isEmpty())
    }
}