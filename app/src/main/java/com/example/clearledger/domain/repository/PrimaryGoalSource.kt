package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.GoalSummary
import kotlinx.coroutines.flow.Flow

/**
 * Contract between the dashboard (Member 3) and the savings-goal module (Member 2), like
 * IncomeSummarySource for income. Emits null when the user has no primary goal.
 *
 * Member 2 implements it on top of the real goal repository and changes one line in ExpenseModule.
 */
interface PrimaryGoalSource {
    fun observePrimaryGoal(): Flow<GoalSummary?>
}
