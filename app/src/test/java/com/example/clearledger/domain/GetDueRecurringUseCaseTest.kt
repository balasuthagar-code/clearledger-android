package com.example.clearledger.domain

import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.DueStatus
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.usecase.GetDueRecurringUseCase
import com.example.clearledger.testutil.testRecurring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetDueRecurringUseCaseTest {

    private val getDue = GetDueRecurringUseCase()
    private val october = YearMonth.of(2026, 10)

    @Test
    fun monthlyItem_isDueEveryMonth_onItsDueDay() {
        val item = testRecurring("r1", dueDay = 5)

        val inOctober = getDue(listOf(item), october, emptyMap())
        val inNovember = getDue(listOf(item), YearMonth.of(2026, 11), emptyMap())

        assertEquals(LocalDate.of(2026, 10, 5), inOctober.single().dueDate)
        assertEquals(LocalDate.of(2026, 11, 5), inNovember.single().dueDate)
        assertEquals(DueStatus.PENDING, inOctober.single().status)
    }

    @Test
    fun yearlyItem_isDueOnlyInItsMonth() {
        val item = testRecurring("r1", frequency = RecurringFrequency.YEARLY, dueDay = 12, dueMonth = 10)

        assertEquals(1, getDue(listOf(item), october, emptyMap()).size)
        assertTrue(getDue(listOf(item), YearMonth.of(2026, 9), emptyMap()).isEmpty())
    }

    @Test
    fun weeklyItem_isNotTracked() {
        val item = testRecurring("r1", frequency = RecurringFrequency.WEEKLY, dueDay = 3)

        assertTrue(getDue(listOf(item), october, emptyMap()).isEmpty())
    }

    @Test
    fun inactiveItem_isNotTracked() {
        val item = testRecurring("r1", isActive = false)

        assertTrue(getDue(listOf(item), october, emptyMap()).isEmpty())
    }

    @Test
    fun dueDay31_inA30DayMonth_becomesTheLastDayOfThatMonth() {
        val item = testRecurring("r1", dueDay = 31)

        assertEquals(LocalDate.of(2026, 11, 30), getDue(listOf(item), YearMonth.of(2026, 11), emptyMap()).single().dueDate)
        assertEquals(LocalDate.of(2027, 2, 28), getDue(listOf(item), YearMonth.of(2027, 2), emptyMap()).single().dueDate)
    }

    @Test
    fun linkedExpense_makesTheItemConfirmed() {
        val item = testRecurring("r1")

        val due = getDue(listOf(item), october, mapOf("r1" to "exp9")).single()

        assertEquals(DueStatus.CONFIRMED, due.status)
        assertEquals("exp9", due.expenseId)
    }

    @Test
    fun skippedThisMonth_isSkipped_butSkippedAnotherMonth_isPending() {
        val skippedNow = testRecurring("a", skippedMonthKey = "2026-10")
        val skippedBefore = testRecurring("b", name = "Gym", skippedMonthKey = "2026-09")

        val result = getDue(listOf(skippedNow, skippedBefore), october, emptyMap()).associate { it.item.id to it.status }

        assertEquals(DueStatus.SKIPPED, result["a"])
        assertEquals(DueStatus.PENDING, result["b"])
    }

    @Test
    fun aRecordedExpense_winsOverASkip() {
        val item = testRecurring("r1", skippedMonthKey = "2026-10")

        assertEquals(DueStatus.CONFIRMED, getDue(listOf(item), october, mapOf("r1" to "exp1")).single().status)
    }

    @Test
    fun itemsAreSortedByDueDate_thenByName() {
        val items = listOf(
            testRecurring("c", name = "Hosting", dueDay = 20),
            testRecurring("b", name = "Gym", dueDay = 5),
            testRecurring("a", name = "Apple", dueDay = 5)
        )

        assertEquals(listOf("a", "b", "c"), getDue(items, october, emptyMap()).map { it.item.id })
    }

    @Test
    fun overdue_isDerived_onlyForPendingItemsWhoseDateHasPassed() {
        val item = testRecurring("r1")
        val pending = DueRecurring(item, LocalDate.of(2026, 10, 5), DueStatus.PENDING)
        val confirmed = pending.copy(status = DueStatus.CONFIRMED)
        val skipped = pending.copy(status = DueStatus.SKIPPED)

        assertTrue(pending.isOverdue(LocalDate.of(2026, 10, 6)))
        assertFalse(pending.isOverdue(LocalDate.of(2026, 10, 5)))     // due today is not overdue yet
        assertFalse(confirmed.isOverdue(LocalDate.of(2026, 10, 20)))
        assertFalse(skipped.isOverdue(LocalDate.of(2026, 10, 20)))
        assertNull(pending.expenseId)
    }
}
