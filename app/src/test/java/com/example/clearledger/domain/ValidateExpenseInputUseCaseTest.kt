package com.example.clearledger.domain

import com.example.clearledger.domain.usecase.ValidateExpenseInputUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateExpenseInputUseCaseTest {

    private val validate = ValidateExpenseInputUseCase()

    @Test
    fun validBaseCurrencyExpense_hasNoErrors() {
        val errors = validate("1250.50", "LKR", "", "food")
        assertFalse(errors.hasErrors)
    }

    @Test
    fun blankAmount_isRejected() {
        assertEquals("Enter an amount", validate("  ", "LKR", "", "food").amount)
    }

    @Test
    fun nonNumericAmount_isRejected() {
        assertNotNull(validate("abc", "LKR", "", "food").amount)
        assertNotNull(validate("12.345", "LKR", "", "food").amount)   // 3 decimals
        assertNotNull(validate("-50", "LKR", "", "food").amount)
    }

    @Test
    fun zeroAmount_isRejected() {
        assertEquals("Amount must be greater than zero", validate("0", "LKR", "", "food").amount)
        assertEquals("Amount must be greater than zero", validate("0.00", "LKR", "", "food").amount)
    }

    @Test
    fun missingCategory_isRejected() {
        assertEquals("Choose a category", validate("100", "LKR", "", null).category)
        assertEquals("Choose a category", validate("100", "LKR", "", "").category)
    }

    @Test
    fun foreignCurrency_needsPositiveRate() {
        assertNotNull(validate("100", "USD", "", "food").rate)
        assertNotNull(validate("100", "USD", "0", "food").rate)
        assertNotNull(validate("100", "USD", "abc", "food").rate)
        assertNull(validate("100", "USD", "300.50", "food").rate)
    }

    @Test
    fun baseCurrency_ignoresRateText() {
        assertNull(validate("100", "LKR", "garbage", "food").rate)
    }

    @Test
    fun foreignCurrency_thatConvertsToZero_isRejected() {
        // 0.01 USD at 0.001 = 0.00001 LKR -> rounds to 0 minor units
        assertEquals("Converted amount is too small", validate("0.01", "USD", "0.001", "food").rate)
    }

    @Test
    fun allProblemsAreReportedTogether() {
        val errors = validate("", "USD", "", null)
        assertTrue(errors.hasErrors)
        assertNotNull(errors.amount)
        assertNotNull(errors.category)
        assertNotNull(errors.rate)
    }
}
