package com.example.clearledger.ui.goals

import com.example.clearledger.domain.model.SavingsGoal

data class SavingsGoalsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)