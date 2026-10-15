package com.example.clearledger.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthKeyTest {
    @Test
    fun formatsDateAsYearMonth() = assertEquals("2026-10", MonthKey.of(LocalDate.of(2026, 10, 14)))

    @Test
    fun previousMonthCrossesYearBoundary() = assertEquals("2025-12", MonthKey.previous("2026-01"))

    @Test
    fun nextMonthCrossesYearBoundary() = assertEquals("2027-01", MonthKey.next("2026-12"))
}
