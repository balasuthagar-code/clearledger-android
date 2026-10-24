package com.example.clearledger.domain

import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.usecase.ExpenseDraft
import com.example.clearledger.domain.usecase.SaveExpenseResult
import com.example.clearledger.domain.usecase.SaveExpenseUseCase
import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import com.example.clearledger.testutil.InMemoryExpenseRepository
import com.example.clearledger.testutil.testExpense
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SaveExpenseUseCaseTest {

    private val repo = InMemoryExpenseRepository()
    private val save = SaveExpenseUseCase(repo, ValidateExpenseInputUseCase())

    private fun draft(
        id: String? = null,
        amount: String = "1250.50",
        currency: String = "LKR",
        rate: String = "",
        category: String? = "food",
        date: LocalDate = LocalDate.of(2026, 10, 20),
        merchant: String = "  Coffee House  ",
        createdAt: Long = 0L
    ) = ExpenseDraft(
        id = id, amountText = amount, currency = currency, rateText = rate, categoryId = category,
        classification = ExpenseClassification.DISCRETIONARY, paymentMethod = PaymentMethod.CARD,
        merchant = merchant, note = " lunch ", date = date, createdAtMillis = createdAt
    )

    @Test
    fun newBaseCurrencyExpense_isSavedInMinorUnits() = runBlocking {
        val result = save(draft())

        assertEquals(SaveExpenseResult.Saved, result)
        val saved = repo.added.single()
        assertEquals(125_050L, saved.amountMinor)
        assertEquals(125_050L, saved.baseAmountMinor)
        assertEquals("1", saved.exchangeRate)
        assertEquals("2026-10", saved.monthKey)
        assertEquals("Coffee House", saved.merchant)     // trimmed
        assertEquals("lunch", saved.note)
    }

    @Test
    fun foreignCurrency_isConvertedWithBr2() = runBlocking {
        save(draft(amount = "100", currency = "USD", rate = "300.50"))

        val saved = repo.added.single()
        assertEquals(10_000L, saved.amountMinor)
        assertEquals(3_005_000L, saved.baseAmountMinor)
        assertEquals("300.5", saved.exchangeRate)
        assertEquals("USD", saved.currency)
    }

    @Test
    fun invalidInput_isNotSaved() = runBlocking {
        val result = save(draft(amount = "abc", category = null))

        assertTrue(result is SaveExpenseResult.Invalid)
        result as SaveExpenseResult.Invalid
        assertTrue(result.errors.amount != null && result.errors.category != null)
        assertTrue(repo.added.isEmpty() && repo.updated.isEmpty())
    }

    @Test
    fun editing_callsUpdate_andKeepsCreatedAt() = runBlocking {
        repo.data.value = listOf(testExpense(id = "e1"))

        val result = save(draft(id = "e1", amount = "99.99", createdAt = 777L))

        assertEquals(SaveExpenseResult.Saved, result)
        assertTrue(repo.added.isEmpty())
        val updated = repo.updated.single()
        assertEquals("e1", updated.id)
        assertEquals(9_999L, updated.amountMinor)
        assertEquals(777L, updated.createdAtMillis)
    }

    @Test
    fun movingTheDateToAnotherMonth_changesTheMonthKey() = runBlocking {
        repo.data.value = listOf(testExpense(id = "e1"))

        save(draft(id = "e1", date = LocalDate.of(2026, 9, 30)))

        assertEquals("2026-09", repo.updated.single().monthKey)
    }

    @Test
    fun repositoryFailure_isReportedWithItsMessage() = runBlocking {
        repo.failWrites = true

        val result = save(draft())

        assertTrue(result is SaveExpenseResult.Failed)
        assertEquals("Could not save the expense. Please try again.", (result as SaveExpenseResult.Failed).message)
    }
}
