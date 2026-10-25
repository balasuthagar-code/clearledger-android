package com.example.clearledger.currency

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.*
import com.example.clearledger.domain.repository.*

class RecordingRateMemory : RateMemoryRepository {
    var writes = 0
    var fail = false
    var last: Pair<RateMemoryContext, String>? = null
    override suspend fun lookup(context: RateMemoryContext): Result<RateMemoryLookup> = Result.Success(RateMemoryLookup.Missing)
    override suspend fun remember(context: RateMemoryContext, confirmedRateText: String): Result<RateMemoryWrite> {
        writes++; last = context to confirmedRateText
        return if (fail) Result.Error(IllegalStateException("disk")) else
            Result.Success(RateMemoryWrite.Recorded(RememberedRate(confirmedRateText, 7)))
    }
}
fun testRateRecorder(memory: RateMemoryRepository = RecordingRateMemory(),
    account: RateMemoryAccount = RateMemoryAccount { "test-user" }) = ConfirmedRateRecorder(memory, account)
