package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.User

interface AuthRepository {

    suspend fun register(
        email: String,
        password: String
    ): Result<User>

    suspend fun retryProfileSetup(): Result<User>

    suspend fun checkIncompleteSetup(): Result<User?>
}