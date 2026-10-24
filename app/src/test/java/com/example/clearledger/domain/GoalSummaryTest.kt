package com.example.clearledger.domain

import com.example.clearledger.domain.model.GoalSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalSummaryTest {

    @Test
    fun progress_isSavedOverTarget() {
        val g = GoalSummary("MacBook", savedMinor = 9_800_000, targetMinor = 49_000_000)
        assertEquals(0.2, g.progressFraction.toDouble(), 0.0001)
        assertEquals(20, g.progressPercent)
    }

    @Test
    fun progress_isCappedAt100Percent() {          // BR-3: min(saved / target x 100, 100)
        val g = GoalSummary("Done", savedMinor = 60_000_000, targetMinor = 49_000_000)
        assertEquals(1.0, g.progressFraction.toDouble(), 0.0)
        assertEquals(100, g.progressPercent)
    }

    @Test
    fun zeroOrNegativeTarget_givesZeroProgress_notACrash() {
        assertEquals(0, GoalSummary("x", 100, 0).progressPercent)
        assertEquals(0, GoalSummary("x", 100, -5).progressPercent)
    }

    @Test
    fun negativeSaved_isClampedToZero() {
        assertEquals(0, GoalSummary("x", -500, 1_000).progressPercent)
    }
}
