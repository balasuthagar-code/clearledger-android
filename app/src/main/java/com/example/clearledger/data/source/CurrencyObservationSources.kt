package com.example.clearledger.data.source

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.CurrencySnapshotMetadata

/** Local handle, with no Firebase SDK types leaking into observation logic. Cancel must be idempotent. */
fun interface CurrencySubscription {
    fun cancel()
}

/** SDK-independent view of just the fields this read-only source needs. */
data class CurrencyDocument(
    val exists: Boolean,
    val baseCurrency: Any?,
    val metadata: CurrencySnapshotMetadata
)

interface CurrencyAuthSource {
    /** Emits the current UID (or null) on registration and then on auth changes. */
    fun observeUid(onUid: (String?) -> Unit): CurrencySubscription
}

interface CurrencyDocumentSource {
    /**
     * Observe users/{uid}. Callbacks may run synchronously during registration.
     * Result.Error is terminal for this registration. A new registration retries.
     * Already queued callbacks may arrive after cancellation; consumers must guard them.
     */
    fun observe(uid: String, onResult: (Result<CurrencyDocument>) -> Unit): CurrencySubscription
}
