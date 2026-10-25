package com.example.clearledger.currency

import com.example.clearledger.core.common.CurrencyCore
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.domain.repository.RateMemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Form-local ticket fence. Call only on the UI dispatcher. Includes typing and pair generations. */
class RateSuggestionSession(val context: RateMemoryContext, val editing: Boolean) {
    private var generation = 0L
    private var active = true
    private var typed = false
    fun ticket(): Long = generation
    fun typed() { typed = true; generation++ }
    fun close() { active = false; generation++ }
    fun accepts(ticket: Long, uid: String?): Boolean = active && !editing && !typed &&
        ticket == generation && uid == context.uid
    fun acceptsExplicit(ticket: Long, uid: String?): Boolean = active && !editing &&
        ticket == generation && uid == context.uid
}

data class ConfirmedRate(val context: RateMemoryContext, val rate: String)
/** A secondary effect only. There is deliberately no financial-save callback to retry here. */
class ConfirmedRateRecorder @Inject constructor(private val memory: RateMemoryRepository,
    private val account: RateMemoryAccount) {
    fun ownerUid(): String? = account.currentUid()
    fun isOwner(uid: String?): Boolean = !uid.isNullOrBlank() && account.currentUid() == uid
    fun capture(original: String, base: String, rate: String, ownerUid: String? = account.currentUid()): ConfirmedRate? {
        val uid = ownerUid?.takeIf { it.isNotBlank() && isOwner(it) } ?: return null
        if (original == base || original !in com.example.clearledger.core.common.Currencies.supported ||
            base !in com.example.clearledger.core.common.Currencies.supported) return null
        val parsed = CurrencyCore.parseRate(rate) as? Result.Success ?: return null
        return ConfirmedRate(RateMemoryContext(uid, original, base), parsed.data.stripTrailingZeros().toPlainString())
    }
    fun stillCurrent(captured: ConfirmedRate): Boolean = account.currentUid() == captured.context.uid
    suspend fun afterSave(captured: ConfirmedRate?): Boolean {
        if (captured == null) return true
        if (!stillCurrent(captured)) return false
        return try {
            withTimeoutOrNull(1500) {
                memory.remember(captured.context, captured.rate) is Result.Success
            } ?: false
        } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
    }
}
