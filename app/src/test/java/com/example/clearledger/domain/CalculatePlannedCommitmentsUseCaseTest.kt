package com.example.clearledger.domain

import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.usecase.CalculatePlannedCommitmentsUseCase
import com.example.clearledger.testutil.testRecurring
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatePlannedCommitmentsUseCaseTest {

    private val calculate = CalculatePlannedCommitmentsUseCase()

    @Test
    fun noItems_isZero() {
        val planned = calculate(emptyList())
        assertEquals(0L, planned.monthlyTotalBaseMinor)
        assertEquals(0, planned.activeMonthlyCount)
        assertEquals(0, planned.otherActiveCount)
    }

    @Test
    fun sumsActiveMonthlyItems_inBaseCurrency() {
        // Sprint 1 sample: Rent 34,000.00 + Gym 4,800.00
        val planned = calculate(
            listOf(
                testRecurring(id = "a", baseMinor = 3_400_000),
                testRecurring(id = "b", baseMinor = 480_000, name = "Gym")
            )
        )
        assertEquals(3_880_000L, planned.monthlyTotalBaseMinor)
        assertEquals(2, planned.activeMonthlyCount)
    }

    @Test
    fun inactiveItems_areIgnored() {
        val planned = calculate(
            listOf(testRecurring(id = "a", baseMinor = 100_000), testRecurring(id = "b", baseMinor = 900_000, isActive = false))
        )
        assertEquals(100_000L, planned.monthlyTotalBaseMinor)
        assertEquals(1, planned.activeMonthlyCount)
    }

    @Test
    fun weeklyAndYearlyItems_areCountedSeparately_notInTheMonthlyTotal() {
        val planned = calculate(
            listOf(
                testRecurring(id = "a", baseMinor = 100_000),
                testRecurring(id = "b", baseMinor = 50_000, frequency = RecurringFrequency.WEEKLY),
                testRecurring(id = "c", baseMinor = 1_200_000, frequency = RecurringFrequency.YEARLY),
                testRecurring(id = "d", baseMinor = 70_000, frequency = RecurringFrequency.WEEKLY, isActive = false)
            )
        )
        assertEquals(100_000L, planned.monthlyTotalBaseMinor)
        assertEquals(1, planned.activeMonthlyCount)
        assertEquals(2, planned.otherActiveCount)
    }

    @Test
    fun foreignCurrencyItem_usesItsBaseAmount() {
        val usd = testRecurring(id = "u", baseMinor = 300_500, amountMinor = 1_000, currency = "USD")
        assertEquals(300_500L, calculate(listOf(usd)).monthlyTotalBaseMinor)
    }
}
