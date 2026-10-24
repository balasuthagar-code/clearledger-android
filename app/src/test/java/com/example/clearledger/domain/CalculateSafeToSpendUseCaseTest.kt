package com.example.clearledger.domain

import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.usecase.CalculateSafeToSpendUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateSafeToSpendUseCaseTest {

    private val calculate = CalculateSafeToSpendUseCase()

    private fun summary(income: Long, expenses: Long) = MonthlySummary(
        monthKey = "2026-10",
        totalIncomeMinor = income,
        totalExpensesMinor = expenses,
        committedMinor = 0,
        discretionaryMinor = expenses,
        remainingMinor = income - expenses,
        categoryTotals = emptyList()
    )

    @Test
    fun br10_incomeMinusExpensesMinusPendingMinusGoalSaving() {
        // Kavindu: LKR 130,000 income, 15,000 spent, 34,000 rent still pending, 40,833.34 needed for the goal.
        val safe = calculate(summary(13_000_000, 1_500_000), pendingCommitmentsMinor = 3_400_000, goalSavingMinor = 4_083_334)

        assertEquals(13_000_000L - 1_500_000L - 3_400_000L - 4_083_334L, safe.amountMinor)
        assertEquals(4_016_666L, safe.amountMinor)
        assertFalse(safe.isOverBudget)
    }

    @Test
    fun noPendingItemsAndNoGoal_isJustTheRecordedNet() {
        val safe = calculate(summary(500_000, 200_000), 0, 0)

        assertEquals(300_000L, safe.amountMinor)
    }

    @Test
    fun overspending_givesANegativeAmount() {
        val safe = calculate(summary(500_000, 400_000), pendingCommitmentsMinor = 300_000, goalSavingMinor = 0)

        assertEquals(-200_000L, safe.amountMinor)
        assertTrue(safe.isOverBudget)
    }

    @Test
    fun negativeInputsForCommitmentsAndGoal_areTreatedAsZero() {
        val safe = calculate(summary(500_000, 100_000), pendingCommitmentsMinor = -5, goalSavingMinor = -9)

        assertEquals(0L, safe.pendingCommitmentsMinor)
        assertEquals(0L, safe.goalSavingMinor)
        assertEquals(400_000L, safe.amountMinor)
    }
}
