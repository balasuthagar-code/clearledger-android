package com.example.clearledger.profile

import com.example.clearledger.core.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProfileRepository @Inject constructor(private val store: ProfileStore,
    private val account: ProfileAccount, private val eligibility: CurrencyEligibilityPolicy) : ProfileRepository {
    private val writes = Mutex()
    private suspend fun <T> operation(uid: String, action: suspend () -> T): Result<T> = try {
        account.requireOwner(uid)
        val result = action()
        account.requireOwner(uid)
        Result.Success(result)
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { Result.Error(e) }

    override suspend fun load(uid: String): Result<ProfileRecord> = operation(uid) {
        ProfileContract.decode(uid, store.read(uid))
    }
    override suspend fun currencyEligibility(uid: String) = eligibility.check(uid)

    override suspend fun save(uid: String, displayName: String, baseCurrency: String,
        expectedCurrency: Any?): Result<ProfileRecord> = writes.withLock {
        operation(uid) {
            // Validate before any storage work. Never accept an email or UID from a form payload.
            val email = account.email ?: ""
            ProfileMerge.fields(uid, email, displayName, baseCurrency, null)
            val old = store.read(uid)
            account.requireOwner(uid)
            ProfileContract.decode(uid, old)
            check(old?.get("baseCurrency") == expectedCurrency) {
                "Your profile currency changed elsewhere. Reload the profile before saving."
            }
            if (old?.get("baseCurrency") != baseCurrency) {
                when (val result = eligibility.check(uid)) {
                    is Result.Error -> throw result.exception
                    is Result.Success -> check(result.data == CurrencyEligibility.Allowed) {
                        "Base currency cannot change because account records already exist (including categories)."
                    }
                }
            }
            account.requireOwner(uid)
            store.save(uid, email, displayName, baseCurrency, expectedCurrency)
            account.requireOwner(uid)
            val saved = ProfileContract.decode(uid, store.read(uid))
            check(saved.complete) { "Profile write could not be verified. Check your connection and retry." }
            saved
        }
    }
}
