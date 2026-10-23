
package com.example.clearledger.domain.model

data class SavingsGoal(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmountMinor: Long = 0L,
    val currency: String = "LKR",
    val deadline: String = "",
    val startingSavedMinor: Long = 0L,
    val contributions: List<SavingsContribution> = emptyList(),
    val isPrimary: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    // Total amount saved, including the starting amount.
    val savedAmountMinor: Long
        get() = startingSavedMinor +
                contributions.sumOf { it.amountMinor }

    // Amount still needed to reach the target.
    val remainingAmountMinor: Long
        get() = (targetAmountMinor - savedAmountMinor).coerceAtLeast(0L)

    // Progress percentage, limited to 100%.
    val progressPercentage: Float
        get() {
            if (targetAmountMinor <= 0L) return 0f

            return (
                    savedAmountMinor.toDouble() /
                            targetAmountMinor.toDouble() * 100.0
                    ).coerceIn(0.0, 100.0).toFloat()
        }

    val isAchieved: Boolean
        get() = savedAmountMinor >= targetAmountMinor &&
                targetAmountMinor > 0L
}
// thisanth


