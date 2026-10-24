package com.example.clearledger.domain

import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.usecase.GetDueRecurringUseCase
import com.example.clearledger.domain.usecase.ObserveDueRecurringUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.InMemoryRecurringRepository
import com.example.clearledger.testutil.testExpense
import com.example.clearledger.testutil.testRecurring
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObserveDueRecurringUseCaseTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun useCase(recurring: InMemoryRecurringRepository, expenses: InMemoryExpenseRepository) =
        ObserveDueRecurringUseCase(recurring, expenses, GetDueRecurringUseCase())

    @Test
    fun anExpenseLinkedToTheItem_inThisMonth_makesItConfirmed() = runTest {
        val recurring = InMemoryRecurringRepository(listOf(testRecurring("r1")))
        val expenses = InMemoryExpenseRepository(
            listOf(testExpense(id = "x", date = LocalDate.of(2026, 10, 3), recurringId = "r1"))
        )

        val due = useCase(recurring, expenses)(today).first().single()

        assertEquals(DueStatus.CONFIRMED, due.status)
        assertEquals("x", due.expenseId)
    }

    @Test
    fun anExpenseFromAnotherMonth_doesNotConfirmThisMonth() = runTest {
        val recurring = InMemoryRecurringRepository(listOf(testRecurring("r1")))
        val expenses = InMemoryExpenseRepository(
            listOf(testExpense(id = "x", date = LocalDate.of(2026, 9, 3), recurringId = "r1"))
        )

        assertEquals(DueStatus.PENDING, useCase(recurring, expenses)(today).first().single().status)
    }

    @Test
    fun anOrdinaryExpenseWithTheSameAmount_doesNotConfirmAnything() = runTest {
        val recurring = InMemoryRecurringRepository(listOf(testRecurring("r1", baseMinor = 3_400_000)))
        val expenses = InMemoryExpenseRepository(
            listOf(testExpense(id = "x", baseMinor = 3_400_000, categoryId = "rent", date = LocalDate.of(2026, 10, 1)))
        )

        assertEquals(DueStatus.PENDING, useCase(recurring, expenses)(today).first().single().status)
    }

    @Test
    fun deletingTheLinkedExpense_makesTheItemPendingAgain() = runTest {
        val recurring = InMemoryRecurringRepository(listOf(testRecurring("r1")))
        val expenses = InMemoryExpenseRepository(
            listOf(testExpense(id = "x", date = LocalDate.of(2026, 10, 3), recurringId = "r1"))
        )
        val observe = useCase(recurring, expenses)
        assertEquals(DueStatus.CONFIRMED, observe(today).first().single().status)

        expenses.deleteExpense("x")

        assertEquals(DueStatus.PENDING, observe(today).first().single().status)
    }
}
