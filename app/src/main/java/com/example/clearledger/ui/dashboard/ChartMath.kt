package com.example.clearledger.ui.dashboard

/**
 * Pure helpers for the dashboard visuals, kept out of the Composables so they can be unit tested.
 * All results are 0f..1f fractions used for bar widths (display only - never for money maths).
 */
internal object ChartMath {

    /** Income vs expenses: the larger value fills the bar, the other is scaled against it. */
    fun incomeExpenseFractions(incomeMinor: Long, expensesMinor: Long): Pair<Float, Float> {
        val max = maxOf(incomeMinor, expensesMinor, 0L)
        if (max == 0L) return 0f to 0f
        return fraction(incomeMinor, max) to fraction(expensesMinor, max)
    }

    /** Committed share of total expenses (the rest is discretionary). 0f when there are no expenses. */
    fun committedShare(committedMinor: Long, totalExpensesMinor: Long): Float =
        if (totalExpensesMinor <= 0L) 0f else fraction(committedMinor, totalExpensesMinor)

    /** Percentage 0..100 (rounded) for labels; see CategoryTotal.percentage for the exact value. */
    fun percentLabel(fraction: Float): Int = Math.round(fraction.coerceIn(0f, 1f) * 100f)

    private fun fraction(part: Long, whole: Long): Float =
        (part.coerceAtLeast(0L).toDouble() / whole.toDouble()).coerceIn(0.0, 1.0).toFloat()
}
