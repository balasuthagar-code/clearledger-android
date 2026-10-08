package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.User

interface AuthRepository {

    suspend fun register(
        email: String,
        password: String
    ): Result<User>

    suspend fun retryProfileSetup(): Result<User>

    suspend fun checkIncompleteSetup(): Result<SetupStatus>

    sealed interface SetupStatus {
        data object NotAuthenticated : SetupStatus
        data class Confirmed(val user: User) : SetupStatus
        data class Incomplete(val user: User) : SetupStatus
    }
}