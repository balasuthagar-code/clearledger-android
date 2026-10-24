package com.example.clearledger.domain

import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.usecase.RecurringDraft
import com.example.clearledger.domain.usecase.SaveRecurringResult
import com.example.clearledger.domain.usecase.SaveRecurringUseCase
import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import com.example.clearledger.domain.usecase.ValidateRecurringInputUseCase
import com.example.clearledger.testutil.InMemoryRecurringRepository
import com.example.clearledger.testutil.testRecurring
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveRecurringUseCaseTest {

    private val repo = InMemoryRecurringRepository()
    private val save = SaveRecurringUseCase(repo, ValidateRecurringInputUseCase(ValidateExpenseInputUseCase()))

    private fun draft(
        id: String? = null,
        name: String = "  Rent ",
        amount: String = "34000",
        currency: String = "LKR",
        rate: String = "",
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        day: String = "1",
        month: String = "1",
        active: Boolean = true,
        category: String? = "rent"
    ) = RecurringDraft(id, name, amount, currency, rate, category, frequency, day, month, active)

    @Test
    fun newMonthlyItem_isSavedInMinorUnits() = runBlocking {
        val result = save(draft())

        assertEquals(SaveRecurringResult.Saved, result)
        val item = repo.added.single()
        assertEquals("Rent", item.name)                // trimmed
        assertEquals(3_400_000L, item.amountMinor)
        assertEquals(3_400_000L, item.baseAmountMinor)
        assertEquals("1", item.exchangeRate)
        assertEquals(RecurringFrequency.MONTHLY, item.frequency)
        assertEquals(1, item.dueDay)
        assertTrue(item.isActive)
    }

    @Test
    fun foreignCurrency_isConvertedWithBr2() = runBlocking {
        save(draft(name = "Hosting", amount = "10", currency = "USD", rate = "300.50"))

        val item = repo.added.single()
        assertEquals(1_000L, item.amountMinor)
        assertEquals(300_500L, item.baseAmountMinor)
        assertEquals("300.5", item.exchangeRate)
    }

    @Test
    fun yearlyItem_keepsItsMonth_otherItemsResetIt() = runBlocking {
        save(draft(name = "Domain", frequency = RecurringFrequency.YEARLY, day = "15", month = "6"))
        save(draft(name = "Gym", frequency = RecurringFrequency.MONTHLY, day = "5", month = "9"))

        assertEquals(6, repo.added[0].dueMonth)
        assertEquals(1, repo.added[1].dueMonth)
    }

    @Test
    fun invalidInput_isNotSaved() = runBlocking {
        val result = save(draft(name = "", amount = "abc", category = null, day = "40"))

        assertTrue(result is SaveRecurringResult.Invalid)
        assertTrue((result as SaveRecurringResult.Invalid).errors.hasErrors)
        assertTrue(repo.added.isEmpty() && repo.updated.isEmpty())
    }

    @Test
    fun editing_callsUpdate_notAdd() = runBlocking {
        repo.data.value = listOf(testRecurring(id = "r9"))

        val result = save(draft(id = "r9", amount = "35000", active = false))

        assertEquals(SaveRecurringResult.Saved, result)
        assertTrue(repo.added.isEmpty())
        val item = repo.updated.single()
        assertEquals("r9", item.id)
        assertEquals(3_500_000L, item.amountMinor)
        assertFalse(item.isActive)
    }

    @Test
    fun repositoryFailure_isReportedWithItsMessage() = runBlocking {
        repo.failWrites = true

        val result = save(draft())

        assertEquals("Could not save the item. Please try again.", (result as SaveRecurringResult.Failed).message)
    }
}
