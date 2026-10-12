package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getSavingsGoals(): Flow<List<SavingsGoal>>

    suspend fun addSavingsGoal(goal: SavingsGoal)
}