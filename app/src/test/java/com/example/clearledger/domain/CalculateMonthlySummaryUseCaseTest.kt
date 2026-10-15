package com.example.clearledger.domain

import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.ExpenseClassification.COMMITTED
import com.example.clearledger.domain.model.ExpenseClassification.DISCRETIONARY
import com.example.clearledger.domain.usecase.CalculateMonthlySummaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalculateMonthlySummaryUseCaseTest {

    private val calculate = CalculateMonthlySummaryUseCase()
    private val month = "2026-10"

    private fun expense(
        base: Long,
        category: String,
        c: ExpenseClassification,
        monthKey: String = month
    ) = Expense(
        id = "$category-$base", amountMinor = base, currency = "LKR", exchangeRate = "1",
        baseAmountMinor = base, categoryId = category, classification = c,
        date = LocalDate.of(2026, 10, 14), monthKey = monthKey
    )

    @Test
    fun noData_givesZerosAndIsEmpty() {
        val s = calculate(month, emptyList(), emptyList())
        assertEquals(0L, s.totalIncomeMinor)
        assertEquals(0L, s.totalExpensesMinor)
        assertEquals(0L, s.remainingMinor)
        assertTrue(s.categoryTotals.isEmpty())
        assertTrue(s.isEmpty)
    }

    @Test
    fun incomeOnly_keepsEverythingAsRemaining() {
        val s = calculate(month, listOf(13_000_000L, 2_000_000L), emptyList())
        assertEquals(15_000_000L, s.totalIncomeMinor)
        assertEquals(15_000_000L, s.remainingMinor)
        assertEquals(0L, s.totalExpensesMinor)
        assertFalse(s.isEmpty)
    }

    @Test
    fun expensesOnly_givesNegativeRemaining() {
        val s = calculate(month, emptyList(), listOf(expense(3_400_000, "rent", COMMITTED)))
        assertEquals(3_400_000L, s.totalExpensesMinor)
        assertEquals(-3_400_000L, s.remainingMinor)
        assertFalse(s.isEmpty)
    }

    @Test
    fun incomeAndExpenses_splitCommittedAndDiscretionary() {
        val s = calculate(
            month, listOf(13_000_000L),
            listOf(
                expense(3_400_000, "rent", COMMITTED),
                expense(480_000, "subscriptions", COMMITTED),
                expense(1_200_000, "food", DISCRETIONARY)
            )
        )
        assertEquals(3_880_000L, s.committedMinor)
        assertEquals(1_200_000L, s.discretionaryMinor)
        assertEquals(5_080_000L, s.totalExpensesMinor)
        assertEquals(s.totalExpensesMinor, s.committedMinor + s.discretionaryMinor)
        assertEquals(7_920_000L, s.remainingMinor)
    }

    @Test
    fun multipleCategories_groupedSortedAndPercentagesAddUp() {
        val s = calculate(
            month, listOf(13_000_000L),
            listOf(
                expense(1_000_000, "food", DISCRETIONARY),
                expense(500_000, "food", DISCRETIONARY),
                expense(500_000, "transport", DISCRETIONARY),
                expense(2_000_000, "rent", COMMITTED)
            )
        )
        assertEquals(listOf("rent", "food", "transport"), s.categoryTotals.map { it.categoryId })
        assertEquals(1_500_000L, s.categoryTotals[1].totalMinor)
        assertEquals(100.0, s.categoryTotals.sumOf { it.percentage }, 0.0001)
        assertEquals(50.0, s.categoryTotals[0].percentage, 0.0001)
    }

    @Test
    fun expensesFromOtherMonthsAreIgnored() {
        val s = calculate(
            month, emptyList(),
            listOf(
                expense(100_000, "food", DISCRETIONARY),
                expense(999_999, "food", DISCRETIONARY, "2026-09")
            )
        )
        assertEquals(100_000L, s.totalExpensesMinor)
    }

    @Test
    fun foreignCurrencyExpense_isCountedByItsBaseAmount() {
        val usd = expense(base = 3_005_000, category = "subscriptions", c = COMMITTED)
            .copy(amountMinor = 10_000, currency = "USD", exchangeRate = "300.50")
        val s = calculate(month, emptyList(), listOf(usd))
        assertEquals(3_005_000L, s.totalExpensesMinor)   // LKR 30,050.00, not USD 100.00
    }
}
