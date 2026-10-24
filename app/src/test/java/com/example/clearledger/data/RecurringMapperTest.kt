package com.example.clearledger.data

import com.example.clearledger.data.dto.RecurringMapper
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.testutil.testRecurring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RecurringMapperTest {

    private fun doc(vararg overrides: Pair<String, Any?>): Map<String, Any?> =
        mapOf<String, Any?>(
            "name" to "Gym", "amountMinor" to 480_000L, "currency" to "LKR", "exchangeRate" to "1",
            "baseAmountMinor" to 480_000L, "categoryId" to "subscriptions",
            "frequency" to "MONTHLY", "dueDay" to 5L, "dueMonth" to 1L, "isActive" to true
        ) + overrides

    @Test
    fun writeThenRead_roundTrips() {
        val original = testRecurring(id = "x", name = "Netflix", baseMinor = 250_000, categoryId = "subscriptions", dueDay = 12)
        val back = RecurringMapper.fromMap("x", RecurringMapper.toMap(original, Date(), isNew = true))
        assertEquals(original, back)
    }

    @Test
    fun toMap_usesSrsFieldNames_includingIsActive() {
        val keys = RecurringMapper.toMap(testRecurring(), Date(), isNew = true).keys
        listOf(
            "name", "amountMinor", "currency", "baseAmountMinor", "categoryId", "frequency",
            "dueDay", "isActive", "createdAt", "updatedAt"
        ).forEach { assertTrue("missing $it", it in keys) }
    }

    @Test
    fun toMap_createdAtOnlyForNew() {
        assertTrue(RecurringMapper.toMap(testRecurring(), Date(), isNew = true).containsKey("createdAt"))
        assertFalse(RecurringMapper.toMap(testRecurring(), Date(), isNew = false).containsKey("createdAt"))
    }

    @Test
    fun fromMap_numbersMayBeAnyNumericType() {
        val item = RecurringMapper.fromMap("a", doc("amountMinor" to 100, "baseAmountMinor" to 100.0, "dueDay" to 7))
        assertEquals(100L, item?.amountMinor)
        assertEquals(100L, item?.baseAmountMinor)
        assertEquals(7, item?.dueDay)
    }

    @Test
    fun fromMap_unknownFrequency_fallsBackToMonthly() {
        assertEquals(RecurringFrequency.MONTHLY, RecurringMapper.fromMap("a", doc("frequency" to "daily"))?.frequency)
        assertEquals(RecurringFrequency.YEARLY, RecurringMapper.fromMap("a", doc("frequency" to "yearly"))?.frequency)
    }

    @Test
    fun fromMap_missingIsActive_meansActive() {
        assertTrue(RecurringMapper.fromMap("a", doc("isActive" to null))!!.isActive)
        assertFalse(RecurringMapper.fromMap("a", doc("isActive" to false))!!.isActive)
    }

    @Test
    fun fromMap_brokenDocuments_returnNull() {
        assertNull(RecurringMapper.fromMap("a", doc("name" to null)))
        assertNull(RecurringMapper.fromMap("a", doc("name" to "  ")))
        assertNull(RecurringMapper.fromMap("a", doc("amountMinor" to null)))
    }

    @Test
    fun fromMap_missingBaseAmount_isDerived() {
        assertEquals(480_000L, RecurringMapper.fromMap("a", doc("baseAmountMinor" to null))?.baseAmountMinor)
        val usd = RecurringMapper.fromMap(
            "a", doc("currency" to "USD", "amountMinor" to 1_000L, "exchangeRate" to "300", "baseAmountMinor" to null)
        )
        assertEquals(300_000L, usd?.baseAmountMinor)
        assertNull(RecurringMapper.fromMap("a", doc("currency" to "USD", "exchangeRate" to "x", "baseAmountMinor" to null)))
    }
}
