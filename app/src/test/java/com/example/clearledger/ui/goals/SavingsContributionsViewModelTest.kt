package com.example.clearledger.ui.goals

import com.example.clearledger.data.repository.SavingsContributionRepository
import com.example.clearledger.data.repository.SavingsGoalRepository
import com.example.clearledger.domain.model.SavingsContribution
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
class SavingsContributionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGoals(
        private val currency: String = "USD"
    ) : SavingsGoalRepository {
        override fun getSavingsGoals(): Flow<List<SavingsGoal>> =
            flowOf(
                listOf(
                    SavingsGoal(
                        id = "goal-1",
                        name = "Test goal",
                        currency = currency,
                        targetAmountMinor = 100_000L
                    )
                )
            )

        override suspend fun addSavingsGoal(goal: SavingsGoal) = Unit
    }

    private class FakeContributions : SavingsContributionRepository {
        val added = mutableListOf<SavingsContribution>()
        val updated = mutableListOf<SavingsContribution>()

        override fun getContributions(
            goalId: String
        ): Flow<List<SavingsContribution>> = flowOf(emptyList())

        override suspend fun addContribution(
            contribution: SavingsContribution
        ) {
            added += contribution
        }

        override suspend fun updateContribution(
            contribution: SavingsContribution
        ) {
            updated += contribution
        }

        override suspend fun deleteContribution(
            goalId: String,
            contributionId: String
        ) = Unit
    }

    @Test
    fun savesExactMinorUnitsInGoalCurrency() = runTest(dispatcher) {
        val repository = FakeContributions()
        val viewModel = SavingsContributionsViewModel(
            FakeGoals("USD"), repository
        )
        runCurrent()

        viewModel.saveContribution(
            "0.29", LocalDate.now().toString(), " Test "
        )
        runCurrent()

        val contribution = repository.added.single()
        assertEquals(29L, contribution.amountMinor)
        assertEquals("goal-1", contribution.goalId)
        assertEquals("Test", contribution.note)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun rejectsInvalidAmountsWithoutWriting() = runTest(dispatcher) {
        val repository = FakeContributions()
        val viewModel = SavingsContributionsViewModel(
            FakeGoals(), repository
        )
        runCurrent()

        for (amount in listOf(
            "1.234", "0", "-1", "abc", "92233720368547758.08"
        )) {
            viewModel.saveContribution(
                amount, LocalDate.now().toString(), ""
            )
            runCurrent()

            assertTrue(repository.added.isEmpty())
            assertTrue(repository.updated.isEmpty())
            assertNotNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun rejectsUnsupportedGoalCurrency() = runTest(dispatcher) {
        val repository = FakeContributions()
        val viewModel = SavingsContributionsViewModel(
            FakeGoals("JPY"), repository
        )
        runCurrent()

        viewModel.saveContribution(
            "100", LocalDate.now().toString(), ""
        )
        runCurrent()

        assertTrue(repository.added.isEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun editingPreservesIdentityAndCreationTime() = runTest(dispatcher) {
        val repository = FakeContributions()
        val viewModel = SavingsContributionsViewModel(
            FakeGoals(), repository
        )
        runCurrent()

        val existing = SavingsContribution(
            id = "contribution-1",
            goalId = "goal-1",
            createdAt = 1234L
        )

        viewModel.saveContribution(
            "12.34", LocalDate.now().toString(), "", existing
        )
        runCurrent()

        assertTrue(repository.added.isEmpty())
        val contribution = repository.updated.single()
        assertEquals("contribution-1", contribution.id)
        assertEquals("goal-1", contribution.goalId)
        assertEquals(1234L, contribution.createdAt)
        assertEquals(1234L, contribution.amountMinor)
    }

    @Test
    fun futureDateStillPreventsSaving() = runTest(dispatcher) {
        val repository = FakeContributions()
        val viewModel = SavingsContributionsViewModel(
            FakeGoals(), repository
        )
        runCurrent()

        viewModel.saveContribution(
            "100", LocalDate.now().plusDays(1).toString(), ""
        )
        runCurrent()

        assertTrue(repository.added.isEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }
}