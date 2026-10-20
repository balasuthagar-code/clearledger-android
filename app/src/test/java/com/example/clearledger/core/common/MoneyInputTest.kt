package com.example.clearledger.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyInputTest {

    @Test
    fun parseToMinor_validAmounts() {
        assertEquals(125050L, MoneyInput.parseToMinor("1250.50"))
        assertEquals(125050L, MoneyInput.parseToMinor("1,250.5"))
        assertEquals(1200L, MoneyInput.parseToMinor("12"))
        assertEquals(5L, MoneyInput.parseToMinor(" 0.05 "))
        assertEquals(0L, MoneyInput.parseToMinor("0"))      // parses; the validator rejects zero
    }

    @Test
    fun parseToMinor_invalidAmounts_returnNull() {
        listOf("", "   ", "abc", "-5", "1.234", "12.", ".5", "1e5", "12 34x", "99999999999").forEach {
            assertNull("'$it' should be invalid", MoneyInput.parseToMinor(it))
        }
    }

    @Test
    fun minorToInputText_roundTrips() {
        assertEquals("1250.50", MoneyInput.minorToInputText(125050))
        assertEquals("0.05", MoneyInput.minorToInputText(5))
        assertEquals("0.00", MoneyInput.minorToInputText(0))
        assertEquals(125050L, MoneyInput.parseToMinor(MoneyInput.minorToInputText(125050)))
    }

    @Test
    fun parseRate_acceptsPositiveDecimals_only() {
        assertEquals(BigDecimal("300.5"), MoneyInput.parseRate("300.5"))
        assertNotNull(MoneyInput.parseRate("0.003333"))
        listOf("0", "0.0", "-1", "abc", "", "300.1234567").forEach {
            assertNull("'$it' should be invalid", MoneyInput.parseRate(it))
        }
    }

    @Test
    fun toBaseMinor_matchesSrsExample() {
        // BR-2 example: USD 100.00 at 300.50 LKR/USD = LKR 30,050.00 = 3,005,000 minor units
        assertEquals(3_005_000L, MoneyInput.toBaseMinor(10_000, BigDecimal("300.50")))
    }

    @Test
    fun toBaseMinor_roundsHalfUp() {
        assertEquals(1L, MoneyInput.toBaseMinor(1, BigDecimal("0.5")))   // 0.5 -> 1
        assertEquals(2L, MoneyInput.toBaseMinor(3, BigDecimal("0.5")))   // 1.5 -> 2
        assertEquals(0L, MoneyInput.toBaseMinor(1, BigDecimal("0.4")))   // 0.4 -> 0
    }

    @Test
    fun toBaseMinor_overflow_returnsNull() {
        assertNull(MoneyInput.toBaseMinor(Long.MAX_VALUE, BigDecimal("9999")))
    }
}
