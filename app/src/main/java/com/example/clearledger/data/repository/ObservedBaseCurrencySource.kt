package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.source.CurrencyAuthSource
import com.example.clearledger.data.source.CurrencyDocument
import com.example.clearledger.data.source.CurrencyDocumentSource
import com.example.clearledger.data.source.CurrencySubscription
import com.example.clearledger.domain.model.BaseCurrencyState
import com.example.clearledger.domain.repository.BaseCurrencySource
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/** Per-collection session state only. No unmanaged scope or global cached currency. */
class ObservedBaseCurrencySource @Inject constructor(
    private val authSource: CurrencyAuthSource,
    private val documentSource: CurrencyDocumentSource
) : BaseCurrencySource {
    override fun observe(): Flow<BaseCurrencyState> = callbackFlow {
        val guard = Any()
        var closed = false
        var initialized = false
        var activeUid: String? = null
        var generation = 0L
        var readTerminated = false
        var documentSubscription: CurrencySubscription? = null

        fun receive(uid: String, token: Long, result: Result<CurrencyDocument>) {
            val toCancel = synchronized(guard) {
                if (closed || token != generation || uid != activeUid || readTerminated) return
                when (result) {
                    is Result.Success -> {
                        trySend(BaseCurrencyStateResolver.resolve(uid, result.data))
                        null
                    }
                    is Result.Error -> {
                        val previous = documentSubscription
                        readTerminated = true
                        documentSubscription = null
                        // Finish session mutation BEFORE delivery: an immediate collector may
                        // synchronously trigger another auth event in response to this failure.
                        // Cancellation is never presented as a profile/read failure.
                        if (result.exception is CancellationException) {
                            close(result.exception)
                        } else {
                            trySend(BaseCurrencyState.ReadFailure(uid, result.exception))
                        }
                        previous
                    }
                }
            }
            toCancel?.cancel()
        }

        fun switchUser(uid: String?) {
            val token: Long
            val previous: CurrencySubscription?
            synchronized(guard) {
                if (closed || (initialized && uid == activeUid)) return
                initialized = true
                activeUid = uid
                generation += 1
                token = generation
                readTerminated = false
                previous = documentSubscription
                documentSubscription = null
                // Invalidate old callbacks BEFORE cancelling their listener. Conflation replaces
                // queued prior-user states, even if the downstream collector is currently slow.
                trySend(if (uid == null) BaseCurrencyState.SignedOut else BaseCurrencyState.Loading(uid))
            }
            previous?.cancel()
            if (uid == null) return
            val next = try {
                documentSource.observe(uid) { result -> receive(uid, token, result) }
            } catch (e: CancellationException) {
                receive(uid, token, Result.Error(e))
                return
            } catch (e: Exception) {
                receive(uid, token, Result.Error(e))
                return
            }
            // Covers synchronous callbacks, errors, logout or even A -> B -> A during registration.
            val keep = synchronized(guard) {
                if (closed || token != generation || uid != activeUid || readTerminated) false
                else {
                    documentSubscription = next
                    true
                }
            }
            if (!keep) next.cancel()
        }

        var authSubscription: CurrencySubscription? = null
        try {
            authSubscription = authSource.observeUid(::switchUser)
            awaitClose { /* All cleanup, including registration exceptions, is below. */ }
        } finally {
            val previous = synchronized(guard) {
                closed = true
                generation += 1
                documentSubscription.also { documentSubscription = null }
            }
            try {
                previous?.cancel()
            } finally {
                authSubscription?.cancel()
            }
        }
    }.buffer(Channel.CONFLATED)
}
