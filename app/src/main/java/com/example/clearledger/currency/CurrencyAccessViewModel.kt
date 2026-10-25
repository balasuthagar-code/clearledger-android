package com.example.clearledger.currency

import com.example.clearledger.domain.repository.RateMemoryAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.domain.repository.BaseCurrencySource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.clearledger.domain.model.BaseCurrencyState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class CurrencyAccessViewModel @Inject constructor(private val source: BaseCurrencySource,
    private val store: CurrencySetupStore, private val account: RateMemoryAccount, private val logout: CurrencyLogout) : ViewModel() {
    private val mutable = MutableStateFlow<CurrencyAccess>(CurrencyAccess.Checking)
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    private var save: Job? = null
    private var setupOwner: String? = null
    private var verifiedUid: String? = null
    fun isSignedOut(): Boolean = account.currentUid() == null
    fun isOwner(uid: String): Boolean = account.currentUid() == uid
    init { retry() }
    fun retry() {
        if (save?.isActive == true) return
        observation?.cancel()
        verifiedUid = null
        observation = viewModelScope.launch {
            mutable.value = CurrencyAccess.Checking
            source.observe().distinctUntilChanged().collectLatest { raw ->
                if (setupOwner != null && account.currentUid() != setupOwner) {
                    save?.cancel(); setupOwner = null
                }
                // A server-verified lease lasts only for this authenticated in-memory session.
                // Offline metadata alone does not destroy an admitted user's draft.
                val cachedLease = raw is BaseCurrencyState.Configured && raw.currency == "LKR" &&
                    !raw.metadata.hasPendingWrites && verifiedUid == raw.uid && isOwner(raw.uid)
                val access = if (cachedLease) CurrencyAccess.Ready(verifiedUid!!) else CurrencyAccessPolicy.resolve(raw)
                if (access !is CurrencyAccess.Ready && access !is CurrencyAccess.Checking) verifiedUid = null
                if (raw is BaseCurrencyState.Loading && raw.uid != verifiedUid) verifiedUid = null
                if (access is CurrencyAccess.Ready) {
                    if (verifiedUid != access.uid) mutable.value = CurrencyAccess.Checking
                    try {
                        if (verifiedUid != access.uid) withTimeout(20_000) { store.verifyHistory(access.uid) }
                        if (account.currentUid() == access.uid) {
                            verifiedUid = access.uid
                            mutable.value = access
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        mutable.value = CurrencyAccess.Blocked("Connect to verify your financial history, then retry.")
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) {
                        mutable.value = CurrencyAccess.Blocked("Financial history could not be verified or needs currency review. Retry online or contact support.")
                    }
                } else mutable.value = access
            }
        }
    }
    fun setup(uid: String) {
        if (save?.isActive == true || mutable.value != CurrencyAccess.Setup(uid)) return
        if (!isOwner(uid)) return
        setupOwner = uid
        mutable.value = CurrencyAccess.Checking
        save = viewModelScope.launch {
            try {
                withTimeout(20_000) { store.initializeLkr(uid) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                if (isOwner(uid)) mutable.value = CurrencyAccess.Blocked("Setup was not confirmed. Connect and retry to check its status.")
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (isOwner(uid)) mutable.value = CurrencyAccess.Blocked("Unable to set the base currency. Existing history may need review. Connect and retry, or contact support.")
            } finally { if (setupOwner == uid) { save = null; setupOwner = null } }
            if (isOwner(uid) && mutable.value == CurrencyAccess.Checking) retry()
        }
    }
    fun logout() { save?.cancel(); logout.signOut() }
}
