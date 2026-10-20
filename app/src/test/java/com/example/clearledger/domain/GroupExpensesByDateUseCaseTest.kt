package com.example.clearledger.domain

import com.example.clearledger.domain.usecase.GroupExpensesByDateUseCase
import com.example.clearledger.testutil.testExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupExpensesByDateUseCaseTest {

    private val group = GroupExpensesByDateUseCase()
    private val oct20 = LocalDate.of(2026, 10, 20)
    private val oct18 = LocalDate.of(2026, 10, 18)

    @Test
    fun emptyList_givesNoGroups() {
        assertTrue(group(emptyList()).isEmpty())
    }

    @Test
    fun groupsByDate_newestDayFirst_withDayTotals() {
        val groups = group(
            listOf(
                testExpense(id = "a", baseMinor = 100_000, date = oct18),
                testExpense(id = "b", baseMinor = 250_000, date = oct20),
                testExpense(id = "c", baseMinor = 50_000, date = oct20)
            )
        )

        assertEquals(listOf(oct20, oct18), groups.map { it.date })
        assertEquals(300_000L, groups[0].totalBaseMinor)
        assertEquals(100_000L, groups[1].totalBaseMinor)
    }

    @Test
    fun withinADay_newestCreatedFirst() {
        val groups = group(
            listOf(
                testExpense(id = "old", date = oct20, createdAtMillis = 1_000),
                testExpense(id = "new", date = oct20, createdAtMillis = 5_000),
                testExpense(id = "mid", date = oct20, createdAtMillis = 3_000)
            )
        )
        assertEquals(listOf("new", "mid", "old"), groups.single().expenses.map { it.id })
    }

    @Test
    fun categoryFilter_keepsOnlyThatCategory_andRecomputesTotals() {
        val all = listOf(
            testExpense(id = "a", baseMinor = 100_000, categoryId = "food", date = oct20),
            testExpense(id = "b", baseMinor = 400_000, categoryId = "rent", date = oct20),
            testExpense(id = "c", baseMinor = 70_000, categoryId = "food", date = oct18)
        )

        val food = group(all, categoryId = "food")

        assertEquals(listOf("a", "c"), food.flatMap { it.expenses }.map { it.id })
        assertEquals(100_000L, food[0].totalBaseMinor)
    }

    @Test
    fun categoryFilter_withNoMatch_givesNoGroups() {
        assertTrue(group(listOf(testExpense(categoryId = "food")), categoryId = "rent").isEmpty())
    }

    @Test
    fun dayTotal_usesBaseAmount_forForeignCurrency() {
        // USD 10.00 recorded, worth LKR 3,005.00 in base currency
        val usd = testExpense(id = "usd", baseMinor = 300_500, amountMinor = 1_000, currency = "USD", date = oct20)
        assertEquals(300_500L, group(listOf(usd)).single().totalBaseMinor)
    }
}
