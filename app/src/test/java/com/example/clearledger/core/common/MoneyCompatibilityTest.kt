package com.example.clearledger.core.common

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class MoneyCompatibilityTest {
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected IllegalArgumentException") }
        catch (_: IllegalArgumentException) { /* expected */ }
    }

    @Test fun legacyFormSyntaxAndBoundsRemain() {
        for (text in listOf("1,2,3.45", "1 23.45", " \t123.45\n")) {
            assertEquals(12345L, MoneyInput.parseToMinor(text))
        }
        assertEquals(999999999999L, MoneyInput.parseToMinor("9999999999.99"))
        for (text in listOf("10000000000", "1.000", "+1", "-1", "1e2", "1\t2", "1\u00a02")) {
            assertNull(text, MoneyInput.parseToMinor(text))
        }
        assertNotNull(MoneyInput.parseRate("9999.123456"))
        for (text in listOf("10000", "1.1234567", "1,000", "1 2", "1e2", "+1")) {
            assertNull(text, MoneyInput.parseRate(text))
        }
    }

    @Test fun legacyConversionIsArithmeticWithoutRateValidation() {
        assertEquals(0L, MoneyInput.toBaseMinor(1, BigDecimal("0.4")))
        assertEquals(-1L, MoneyInput.toBaseMinor(1, BigDecimal("-0.5")))
        assertEquals(0L, MoneyInput.toBaseMinor(100, BigDecimal.ZERO))
        assertNull(MoneyInput.toBaseMinor(Long.MAX_VALUE, BigDecimal("2")))
    }

    @Test fun moneyUtilsSyntaxAndPrecisionRemain() {
        for (text in listOf("+1", "1.", "1e0", " 1 ")) assertEquals(100L, MoneyUtils.majorToMinorLong(text))
        assertEquals(50L, MoneyUtils.majorToMinorLong(".5"))
        assertEquals(-100L, MoneyUtils.majorToMinorLong("-1"))
        assertEquals(10000L, MoneyUtils.majorToMinorLong("1e2"))
        assertEquals(1L, MoneyUtils.majorToMinorLong("1e-2"))
        for (text in listOf("1.000", "1e-3", "1,000", "1 000", "", "92233720368547758.08")) {
            rejects { MoneyUtils.majorToMinorLong(text) }
        }
        // No legacy digit cap; zeros can be arbitrarily long without overflowing.
        assertEquals(100L, MoneyUtils.majorToMinorLong("0".repeat(100) + "1"))
        for (scale in listOf(-1, 5)) {
            rejects { MoneyUtils.majorToMinorLong("1", scale) }
            rejects { MoneyUtils.minorToMajorString(1, scale) }
        }
    }
}
