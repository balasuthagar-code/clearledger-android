package com.example.clearledger.domain.model

data class CategoryTotal(
    val categoryId: String,
    val totalMinor: Long,
    val percentage: Double            // 0.0..100.0 - for DISPLAY only, never used in money maths
)

data class MonthlySummary(
    val monthKey: String,
    val totalIncomeMinor: Long,
    val totalExpensesMinor: Long,
    val committedMinor: Long,
    val discretionaryMinor: Long,
    val remainingMinor: Long,         // income - expenses (negative = overspent)
    val categoryTotals: List<CategoryTotal>
) {
    /** True when there is nothing to show -> the UI shows the "add your first record" empty state. */
    val isEmpty: Boolean get() = totalIncomeMinor == 0L && totalExpensesMinor == 0L
}
