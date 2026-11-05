package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


interface PrimaryGoalSource {
    fun observePrimaryGoal(): Flow<GoalSummary?>

    fun observeSavingsGoals(): Flow<List<SavingsGoal>> = flowOf(emptyList())
}
