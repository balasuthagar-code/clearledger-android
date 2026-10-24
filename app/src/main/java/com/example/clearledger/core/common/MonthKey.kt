package com.example.clearledger.core.common

import java.time.LocalDate
import java.time.YearMonth

/**
 * Team contract: every dated record stores a month key in the form "YYYY-MM" (e.g. "2026-10").
 * "All expenses of October" then becomes one equality query (whereEqualTo("monthKey", "2026-10"))
 * instead of a date-range query. java.time is built into Android API 26+ (our minSdk).
 */
object MonthKey {
    fun of(date: LocalDate): String = YearMonth.from(date).toString()   // "2026-10"
    fun current(): String = of(LocalDate.now())
    fun previous(key: String): String = YearMonth.parse(key).minusMonths(1).toString()
    fun next(key: String): String = YearMonth.parse(key).plusMonths(1).toString()
}
