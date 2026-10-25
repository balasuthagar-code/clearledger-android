package com.example.clearledger.currency

import com.example.clearledger.domain.model.BaseCurrencyState

sealed interface CurrencyAccess {
    data object SignedOut : CurrencyAccess
    data object Checking : CurrencyAccess
    data class Ready(val uid: String) : CurrencyAccess
    data class Setup(val uid: String) : CurrencyAccess
    data class Blocked(val message: String) : CurrencyAccess
}
object CurrencyAccessPolicy {
    fun resolve(state: BaseCurrencyState): CurrencyAccess = when (state) {
        BaseCurrencyState.SignedOut -> CurrencyAccess.SignedOut
        is BaseCurrencyState.Loading -> CurrencyAccess.Checking
        is BaseCurrencyState.Configured -> when {
            state.currency != "LKR" -> CurrencyAccess.Blocked("Financial screens are unavailable for ${state.currency}. Your saved currency has been preserved. Contact support.")
            state.metadata.isFromCache || state.metadata.hasPendingWrites -> CurrencyAccess.Blocked("Connect to verify your base currency, then retry.")
            else -> CurrencyAccess.Ready(state.uid)
        }
        is BaseCurrencyState.MissingDocument -> CurrencyAccess.Setup(state.uid)
        is BaseCurrencyState.Unconfigured -> CurrencyAccess.Setup(state.uid)
        is BaseCurrencyState.Invalid -> CurrencyAccess.Blocked("Your saved currency needs review. Contact support.")
        is BaseCurrencyState.ReadFailure -> CurrencyAccess.Blocked("Could not verify your currency. Check your connection and retry.")
    }
}
interface CurrencySetupStore {
    suspend fun initializeLkr(uid: String)
    suspend fun verifyHistory(uid: String)
}

fun interface CurrencyLogout { fun signOut() }

/** Record identity is independent of today's selected profile currency. */
object HistoricalBasePolicy {
    fun accepts(raw: Any?): Boolean = raw == "LKR"
}
class CurrencyHistoryException : IllegalStateException("Saved currency is unknown or unavailable. Contact support for review.")
