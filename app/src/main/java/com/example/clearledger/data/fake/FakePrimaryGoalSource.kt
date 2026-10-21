package com.example.clearledger.data.fake

import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.repository.PrimaryGoalSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * TEMPORARY: "no primary goal" (the dashboard then shows its placeholder card).
 * Member 2 replaces it by implementing PrimaryGoalSource on the real goal repository.
 */
class FakePrimaryGoalSource @Inject constructor() : PrimaryGoalSource {
    override fun observePrimaryGoal(): Flow<GoalSummary?> = flowOf(null)
}
