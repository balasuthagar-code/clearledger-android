package com.example.clearledger.profile

import com.example.clearledger.core.common.Result

interface ProfileRepository {
    suspend fun load(uid: String): Result<ProfileRecord>
    suspend fun save(uid: String, displayName: String, baseCurrency: String, expectedCurrency: Any?): Result<ProfileRecord>
    suspend fun currencyEligibility(uid: String): Result<CurrencyEligibility>
}
sealed interface CurrencyEligibility {
    data object Allowed : CurrencyEligibility
    data object HistoryExists : CurrencyEligibility
}
