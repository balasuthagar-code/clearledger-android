package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.SavingsContribution
import kotlinx.coroutines.flow.Flow

interface SavingsContributionRepository {

    fun getContributions(
        goalId: String
    ): Flow<List<SavingsContribution>>

    suspend fun addContribution(
        contribution: SavingsContribution
    )

    suspend fun updateContribution(
        contribution: SavingsContribution
    )

    suspend fun deleteContribution(
        goalId: String,
        contributionId: String
    )
}