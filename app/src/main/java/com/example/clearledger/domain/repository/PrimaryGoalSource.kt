package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.GoalSummary
import kotlinx.coroutines.flow.Flow


interface PrimaryGoalSource {
    fun observePrimaryGoal(): Flow<GoalSummary?>
}
