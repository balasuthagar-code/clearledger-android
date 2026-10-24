package com.example.clearledger.domain

import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.usecase.RecurringFormErrors
import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import com.example.clearledger.domain.usecase.ValidateRecurringInputUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRecurringInputUseCaseTest {

    private val validate = ValidateRecurringInputUseCase(ValidateExpenseInputUseCase())

    private fun run(
        name: String = "Rent",
        amount: String = "34000",
        currency: String = "LKR",
        rate: String = "",
        category: String? = "rent",
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        day: String = "1",
        month: String = "1"
    ): RecurringFormErrors = validate(name, amount, currency, rate, category, frequency, day, month)

    @Test
    fun validMonthlyItem_hasNoErrors() {
        assertFalse(run().hasErrors)
    }

    @Test
    fun blankName_isRejected() {
        assertNotNull(run(name = "   ").name)
    }

    @Test
    fun tooLongName_isRejected() {
        assertNotNull(run(name = "x".repeat(61)).name)
        assertNull(run(name = "x".repeat(60)).name)
    }

    @Test
    fun amountAndCategoryRules_matchTheExpenseForm() {
        assertEquals("Amount must be greater than zero", run(amount = "0").amount)
        assertNotNull(run(amount = "abc").amount)
        assertEquals("Choose a category", run(category = null).category)
    }

    @Test
    fun foreignCurrency_needsRate() {
        assertNotNull(run(currency = "USD", rate = "").rate)
        assertNull(run(currency = "USD", rate = "300.5").rate)
    }

    @Test
    fun monthlyDueDay_mustBe1To31() {
        assertNull(run(day = "1").dueDay)
        assertNull(run(day = "31").dueDay)
        assertNotNull(run(day = "0").dueDay)
        assertNotNull(run(day = "32").dueDay)
        assertNotNull(run(day = "abc").dueDay)
        assertNotNull(run(day = "").dueDay)
    }

    @Test
    fun weeklyDueDay_mustBe1To7() {
        assertNull(run(frequency = RecurringFrequency.WEEKLY, day = "7").dueDay)
        assertNotNull(run(frequency = RecurringFrequency.WEEKLY, day = "8").dueDay)
        assertNotNull(run(frequency = RecurringFrequency.WEEKLY, day = "0").dueDay)
    }

    @Test
    fun yearly_needsValidMonth_andADayThatExistsInThatMonth() {
        assertNull(run(frequency = RecurringFrequency.YEARLY, day = "15", month = "6").dueMonth)
        assertNotNull(run(frequency = RecurringFrequency.YEARLY, day = "15", month = "13").dueMonth)
        assertNotNull(run(frequency = RecurringFrequency.YEARLY, day = "15", month = "").dueMonth)
        assertEquals("That month has fewer days", run(frequency = RecurringFrequency.YEARLY, day = "31", month = "4").dueDay)
        assertNull(run(frequency = RecurringFrequency.YEARLY, day = "29", month = "2").dueDay)     // leap day allowed
        assertNotNull(run(frequency = RecurringFrequency.YEARLY, day = "30", month = "2").dueDay)
    }

    @Test
    fun monthIsIgnoredForNonYearlyItems() {
        assertNull(run(frequency = RecurringFrequency.MONTHLY, month = "99").dueMonth)
    }

    @Test
    fun allProblemsAreReportedTogether() {
        val errors = run(name = "", amount = "", category = null, day = "")
        assertTrue(errors.hasErrors)
        assertNotNull(errors.name); assertNotNull(errors.amount); assertNotNull(errors.category); assertNotNull(errors.dueDay)
    }
}
