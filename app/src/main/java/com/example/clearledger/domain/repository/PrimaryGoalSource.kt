package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Contract between the dashboard (Member 3) and the savings-goal module (Member 2), like
 * IncomeSummarySource for income. Emits null when the user has no primary goal.
 *
 * Member 2 implements it on top of the real goal repository and changes one line in ExpenseModule.
 */
interface PrimaryGoalSource {
    fun observePrimaryGoal(): Flow<GoalSummary?>

    fun observeSavingsGoals(): Flow<List<SavingsGoal>> = flowOf(emptyList())
}
