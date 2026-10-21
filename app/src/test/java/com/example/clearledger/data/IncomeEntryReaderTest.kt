package com.example.clearledger.data

import com.example.clearledger.data.dto.IncomeEntryReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomeEntryReaderTest {

    private fun entry(vararg overrides: Pair<String, Any?>): Map<String, Any?> =
        mapOf<String, Any?>("status" to "RECEIVED", "baseAmountMinor" to 13_000_000L, "monthKey" to "2026-10") + overrides

    @Test
    fun receivedEntry_isCountedByItsBaseAmount() {
        assertEquals(13_000_000L, IncomeEntryReader.receivedBaseAmount(entry()))
    }

    @Test
    fun expectedEntry_isNotCounted() {                       // FR-INC-07 / BR-6
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("status" to "EXPECTED")))
    }

    @Test
    fun statusIsCaseInsensitive() {
        assertEquals(13_000_000L, IncomeEntryReader.receivedBaseAmount(entry("status" to "Received")))
        assertEquals(13_000_000L, IncomeEntryReader.receivedBaseAmount(entry("status" to " received ")))
    }

    @Test
    fun missingOrUnknownStatus_isNotCounted() {
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("status" to null)))
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("status" to "PENDING")))
    }

    @Test
    fun missingOrNonPositiveAmount_isNotCounted() {
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("baseAmountMinor" to null)))
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("baseAmountMinor" to 0L)))
        assertNull(IncomeEntryReader.receivedBaseAmount(entry("baseAmountMinor" to -5L)))
    }

    @Test
    fun amountMayBeAnyNumericType() {
        assertEquals(5_000L, IncomeEntryReader.receivedBaseAmount(entry("baseAmountMinor" to 5000)))
    }
}
