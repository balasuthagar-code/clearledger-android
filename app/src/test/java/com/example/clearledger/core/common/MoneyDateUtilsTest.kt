package com.example.clearledger.core.common

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MoneyDateUtilsTest {

    @Test
    fun testMinorToMajorConversion() {
        assertEquals("1250.75", MoneyUtils.minorToMajorString(125075, 2))
        assertEquals("0.05", MoneyUtils.minorToMajorString(5, 2))
        assertEquals("10.00", MoneyUtils.minorToMajorString(1000, 2))
    }

    @Test
    fun testMajorToMinorConversion() {
        assertEquals(125075L, MoneyUtils.majorToMinorLong("1250.75", 2))
        assertEquals(5L, MoneyUtils.majorToMinorLong("0.05", 2))
        assertEquals(1000L, MoneyUtils.majorToMinorLong("10", 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidMoneyFormatThrowsException() {
        MoneyUtils.majorToMinorLong("invalid_amount", 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnsupportedPrecisionThrowsException() {
        MoneyUtils.majorToMinorLong("10.123", 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOverflowThrowsException() {
        MoneyUtils.majorToMinorLong("92233720368547759.00", 2)
    }

    @Test
    fun testMonthKeyFormattingWithExplicitTimeZone() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val date = cal.time
        val timestamp = Timestamp(date)
        val tz = TimeZone.getTimeZone("UTC")

        assertEquals("2026-10", DateUtils.dateToMonthKey(date, tz))
        assertEquals("2026-10", DateUtils.timestampToMonthKey(timestamp, tz))
    }

    @Test
    fun testMonthBoundaryMonthKeyWithExplicitTimeZone() {
        val tz = TimeZone.getTimeZone("UTC")
        val calStart = Calendar.getInstance(tz).apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("2026-01", DateUtils.dateToMonthKey(calStart.time, tz))

        val calEnd = Calendar.getInstance(tz).apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("2026-12", DateUtils.dateToMonthKey(calEnd.time, tz))
    }
}
