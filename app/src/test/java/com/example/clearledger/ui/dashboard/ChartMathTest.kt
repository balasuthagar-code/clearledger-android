package com.example.clearledger.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartMathTest {

    @Test
    fun incomeBiggerThanExpenses_incomeFillsTheBar() {
        val (income, expenses) = ChartMath.incomeExpenseFractions(13_000_000, 5_430_000)
        assertEquals(1.0, income.toDouble(), 0.0001)
        assertEquals(0.4177, expenses.toDouble(), 0.0001)
    }

    @Test
    fun expensesBiggerThanIncome_expensesFillTheBar() {
        val (income, expenses) = ChartMath.incomeExpenseFractions(1_000_000, 4_000_000)
        assertEquals(0.25, income.toDouble(), 0.0001)
        assertEquals(1.0, expenses.toDouble(), 0.0001)
    }

    @Test
    fun noData_givesEmptyBars() {
        val (income, expenses) = ChartMath.incomeExpenseFractions(0, 0)
        assertEquals(0.0, income.toDouble(), 0.0)
        assertEquals(0.0, expenses.toDouble(), 0.0)
    }

    @Test
    fun incomeOnly_expensesBarIsEmpty() {
        val (income, expenses) = ChartMath.incomeExpenseFractions(500_000, 0)
        assertEquals(1.0, income.toDouble(), 0.0001)
        assertEquals(0.0, expenses.toDouble(), 0.0)
    }

    @Test
    fun committedShare_isPartOfTotalExpenses() {
        // From the Sprint 1 sample: committed 38,800.00 of 54,300.00 expenses
        assertEquals(0.7146, ChartMath.committedShare(3_880_000, 5_430_000).toDouble(), 0.0001)
        assertEquals(0.0, ChartMath.committedShare(0, 0).toDouble(), 0.0)
        assertEquals(1.0, ChartMath.committedShare(100, 100).toDouble(), 0.0001)
    }

    @Test
    fun percentLabel_roundsAndClamps() {
        assertEquals(71, ChartMath.percentLabel(0.7146f))
        assertEquals(100, ChartMath.percentLabel(1.7f))
        assertEquals(0, ChartMath.percentLabel(-0.2f))
    }
}
