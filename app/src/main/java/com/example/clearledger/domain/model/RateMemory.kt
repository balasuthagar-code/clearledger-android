package com.example.clearledger.domain.model

/** Ordered pair. Quote direction: base major units per ONE original major unit. */
data class RateMemoryContext(val uid: String, val originalCurrency: String, val baseCurrency: String)

/** Device-local time of remembering a user-confirmed quote, never a provider quote timestamp. */
data class RememberedRate(val ratePlain: String, val rememberedAtEpochMillis: Long)

sealed interface RateMemoryLookup {
    data object Missing : RateMemoryLookup
    data object Identity : RateMemoryLookup { const val ratePlain = "1" }
    data class Remembered(val rate: RememberedRate) : RateMemoryLookup
}

sealed interface RateMemoryWrite {
    data object IdentityIgnored : RateMemoryWrite
    data class Recorded(val rate: RememberedRate) : RateMemoryWrite
}

enum class RateMemoryFailure {
    INVALID_UID, INVALID_PAIR, INVALID_RATE, SIGNED_OUT, ACCOUNT_CHANGED,
    ACCOUNT_UNAVAILABLE, CORRUPT_RECORD, READ_FAILED, WRITE_FAILED, INVALID_CLOCK
}

/** Deliberately contains no UID, rate, key or underlying storage message. Do not log financial data. */
class RateMemoryException(val failure: RateMemoryFailure) : IllegalStateException(failure.name)
