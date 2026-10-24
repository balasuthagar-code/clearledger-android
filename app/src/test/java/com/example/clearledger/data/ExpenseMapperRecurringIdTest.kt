package com.example.clearledger.data

import com.example.clearledger.data.dto.ExpenseMapper
import com.example.clearledger.testutil.testExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ExpenseMapperRecurringIdTest {

    @Test
    fun recurringId_roundTrips() {
        val original = testExpense(id = "rec_r1_2026-10", recurringId = "r1")
        val now = Date(1_700_000_000_000)

        val back = ExpenseMapper.fromMap(original.id, ExpenseMapper.toMap(original, now, isNew = true))

        assertEquals("r1", back?.recurringId)
    }

    @Test
    fun anOrdinaryExpense_writesNoRecurringIdField() {
        val map = ExpenseMapper.toMap(testExpense(), Date(), isNew = true)

        assertFalse(map.containsKey("recurringId"))
    }

    @Test
    fun aMissingOrBlankRecurringId_isReadAsNull() {
        val base = ExpenseMapper.toMap(testExpense(), Date(), isNew = true)

        assertNull(ExpenseMapper.fromMap("x", base)?.recurringId)
        assertNull(ExpenseMapper.fromMap("x", base + ("recurringId" to "  "))?.recurringId)
    }
}
