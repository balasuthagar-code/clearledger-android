package com.example.clearledger.domain.model

/** Observation metadata only: none of these states proves a financial-write currency lock. */
data class CurrencySnapshotMetadata(
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean
)

enum class InvalidBaseCurrencyReason { WRONG_TYPE, BLANK, UNSUPPORTED_CODE }

/** Every authenticated state identifies its owner; never carry a prior user's currency forward. */
sealed interface BaseCurrencyState {
    data object SignedOut : BaseCurrencyState
    data class Loading(val uid: String) : BaseCurrencyState
    data class Configured(
        val uid: String,
        val currency: String,
        val metadata: CurrencySnapshotMetadata
    ) : BaseCurrencyState
    data class MissingDocument(
        val uid: String,
        val metadata: CurrencySnapshotMetadata
    ) : BaseCurrencyState
    data class Unconfigured(
        val uid: String,
        val metadata: CurrencySnapshotMetadata
    ) : BaseCurrencyState
    data class Invalid(
        val uid: String,
        val reason: InvalidBaseCurrencyReason,
        val metadata: CurrencySnapshotMetadata
    ) : BaseCurrencyState
    data class ReadFailure(val uid: String, val cause: Exception) : BaseCurrencyState
}
