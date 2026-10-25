package com.example.clearledger.domain.repository

import com.example.clearledger.domain.model.BaseCurrencyState
import kotlinx.coroutines.flow.Flow

/** Read-only; no profile creation, default currency, completion or locking policy. */
interface BaseCurrencySource {
    /**
     * Cold, conflated stream: collection owns its auth/document listeners.
     * To retry a terminal document-listener failure, cancel the current collection
     * and collect again. That emits fresh auth/loading states and opens a new listener.
     * Auth changes also switch the listener. Cancel on lifecycle exit.
     */
    fun observe(): Flow<BaseCurrencyState>
}
