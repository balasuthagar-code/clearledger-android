package com.example.clearledger.domain

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.usecase.ConfirmRecurringResult
import com.example.clearledger.domain.usecase.ConfirmRecurringUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.testRecurring
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConfirmRecurringUseCaseTest {

    private class FakeCategories(private val categories: List<ExpenseCategory> = emptyList()) : CategoryRepository {
        override fun observeCategories(): Flow<List<ExpenseCategory>> = flowOf(categories)
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = Result.Success(Unit)
        override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> = Result.Success(Unit)
    }

    private class BrokenCategories : CategoryRepository {
        override fun observeCategories(): Flow<List<ExpenseCategory>> = flow { throw RuntimeException("boom") }
        override suspend fun seedDefaultCategoriesIfEmpty(): Result<Unit> = Result.Success(Unit)
        override suspend fun setFavourite(id: String, favourite: Boolean): Result<Unit> = Result.Success(Unit)
    }

    private val today = LocalDate.of(2026, 10, 15)

    private fun due(
        id: String = "r1",
        dueDate: LocalDate = LocalDate.of(2026, 10, 1),
        category: String = "rent",
        currency: String = "LKR",
        amountMinor: Long = 3_400_000,
        baseMinor: Long = 3_400_000
    ) = DueRecurring(
        item = testRecurring(id, categoryId = category, currency = currency, amountMinor = amountMinor, baseMinor = baseMinor),
        dueDate = dueDate,
        status = DueStatus.PENDING
    )

    private val userCategories = listOf(
        ExpenseCategory("rent", "Rent", ExpenseClassification.COMMITTED),
        ExpenseCategory("food", "Food", ExpenseClassification.DISCRETIONARY)
    )

    @Test
    fun confirm_createsOneLinkedExpenseWithAPredictableId() = runTest {
        val expenses = InMemoryExpenseRepository()

        val result = ConfirmRecurringUseCase(expenses, FakeCategories(userCategories))(due(), today)

        assertEquals(ConfirmRecurringResult.Confirmed, result)
        val expense = expenses.upserted.single()
        assertEquals("rec_r1_2026-10", expense.id)
        assertEquals("r1", expense.recurringId)
        assertEquals("Rent", expense.merchant)
        assertEquals(3_400_000L, expense.baseAmountMinor)
        assertEquals(LocalDate.of(2026, 10, 1), expense.date)
        assertEquals("2026-10", expense.monthKey)
        assertEquals(PaymentMethod.OTHER, expense.paymentMethod)
    }

    @Test
    fun confirm_usesTheCategorysDefaultClassification() = runTest {
        val expenses = InMemoryExpenseRepository()
        val useCase = ConfirmRecurringUseCase(expenses, FakeCategories(userCategories))

        useCase(due("r1", category = "rent"), today)
        useCase(due("r2", category = "food"), today)

        assertEquals(ExpenseClassification.COMMITTED, expenses.upserted[0].classification)
        assertEquals(ExpenseClassification.DISCRETIONARY, expenses.upserted[1].classification)
    }

    @Test
    fun confirm_withoutLoadedCategories_fallsBackToTheBuiltInDefaults() = runTest {
        val expenses = InMemoryExpenseRepository()
        val useCase = ConfirmRecurringUseCase(expenses, BrokenCategories())

        useCase(due("r1", category = "subscriptions"), today)
        useCase(due("r2", category = "my_custom_category"), today)

        assertEquals(ExpenseClassification.COMMITTED, expenses.upserted[0].classification)
        assertEquals(ExpenseClassification.DISCRETIONARY, expenses.upserted[1].classification)
    }

    @Test
    fun confirmingBeforeTheDueDay_recordsTodayNotAFutureDate() = runTest {
        val expenses = InMemoryExpenseRepository()

        ConfirmRecurringUseCase(expenses, FakeCategories())(due(dueDate = LocalDate.of(2026, 10, 25)), today)

        val expense = expenses.upserted.single()
        assertEquals(today, expense.date)
        assertEquals("rec_r1_2026-10", expense.id)         // still the id of the due month
    }

    @Test
    fun foreignCurrencyItem_keepsOriginalAmountRateAndBaseAmount() = runTest {
        val expenses = InMemoryExpenseRepository()

        ConfirmRecurringUseCase(expenses, FakeCategories())(
            due(currency = "USD", amountMinor = 1_000, baseMinor = 300_500), today
        )

        val expense = expenses.upserted.single()
        assertEquals("USD", expense.currency)
        assertEquals(1_000L, expense.amountMinor)
        assertEquals("300", expense.exchangeRate)
        assertEquals(300_500L, expense.baseAmountMinor)
    }

    @Test
    fun confirmingTwice_leavesExactlyOneExpense() = runTest {
        val expenses = InMemoryExpenseRepository()
        val useCase = ConfirmRecurringUseCase(expenses, FakeCategories(userCategories))

        useCase(due(), today)
        useCase(due(), today)

        assertEquals(1, expenses.data.value.size)
    }

    @Test
    fun repositoryFailure_isReturned_andNothingIsRecorded() = runTest {
        val expenses = InMemoryExpenseRepository().apply { failWrites = true }

        val result = ConfirmRecurringUseCase(expenses, FakeCategories())(due(), today)

        assertTrue(result is ConfirmRecurringResult.Failed)
        assertEquals("Could not save the expense. Please try again.", (result as ConfirmRecurringResult.Failed).message)
        assertTrue(expenses.data.value.isEmpty())
    }

    @Test
    fun expenseIdFor_isStable() {
        assertEquals("rec_abc_2026-12", ConfirmRecurringUseCase.expenseIdFor("abc", "2026-12"))
    }
}
