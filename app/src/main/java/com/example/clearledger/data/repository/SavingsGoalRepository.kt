package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getSavingsGoals(): Flow<List<SavingsGoal>>

    suspend fun addSavingsGoal(goal: SavingsGoal)

    suspend fun updateSavingsGoal(goal: SavingsGoal) {
        error("Updating savings goals is not supported by this repository.")
    }

    suspend fun deleteSavingsGoal(goalId: String) {
        error("Deleting savings goals is not supported by this repository.")
    }
}