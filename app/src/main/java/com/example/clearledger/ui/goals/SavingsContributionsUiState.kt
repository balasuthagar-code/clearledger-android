package com.example.clearledger.ui.goals

import com.example.clearledger.domain.model.SavingsContribution
import com.example.clearledger.domain.model.SavingsGoal

data class SavingsContributionsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val contributions: List<SavingsContribution> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val totalSaved: Double
        get() = (selectedGoal?.startingAmount ?: 0.0) +
                contributions.sumOf { it.amount }

    val remainingAmount: Double
        get() = ((selectedGoal?.targetAmount ?: 0.0) - totalSaved)
            .coerceAtLeast(0.0)

    val progress: Float
        get() {
            val target = selectedGoal?.targetAmount ?: 0.0
            if (target <= 0.0) return 0f
            return (totalSaved / target).toFloat().coerceIn(0f, 1f)
        }
}