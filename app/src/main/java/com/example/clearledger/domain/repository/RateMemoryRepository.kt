package com.example.clearledger.domain.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.model.RateMemoryLookup
import com.example.clearledger.domain.model.RateMemoryWrite

interface RateMemoryRepository {
    suspend fun lookup(context: RateMemoryContext): Result<RateMemoryLookup>
    /** Call only AFTER a financial save reaches the feature's agreed success state. */
    suspend fun remember(context: RateMemoryContext, confirmedRateText: String): Result<RateMemoryWrite>
}

/** Synchronous identity read, with no disk operations or registration/scopes. */
fun interface RateMemoryAccount { fun currentUid(): String? }
fun interface RateMemoryClock { fun nowEpochMillis(): Long }
