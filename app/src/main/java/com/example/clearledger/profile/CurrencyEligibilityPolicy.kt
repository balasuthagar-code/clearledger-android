package com.example.clearledger.profile

import com.example.clearledger.core.common.Result
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** Server-backed compatibility check, not a lock shared by existing financial writers. */
class CurrencyEligibilityPolicy @Inject constructor(private val store: ProfileStore, private val account: ProfileAccount) {
    suspend fun check(uid: String): Result<CurrencyEligibility> = try {
        account.requireOwner(uid)
        var found = false
        for (collection in COLLECTIONS) {
            account.requireOwner(uid)
            if (store.hasDocuments(uid, collection)) found = true
        }
        account.requireOwner(uid)
        Result.Success(if (found) CurrencyEligibility.HistoryExists else CurrencyEligibility.Allowed)
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { Result.Error(IllegalStateException(
          "Currency change unavailable until history is verified. Check your connection and retry.", e)) }

    companion object {
        // Include the requested contract paths AND actual current repository paths. Never rename data.
        val COLLECTIONS = listOf("incomeSources", "incomeEntries", "projects", "cryptoTrades", "categories",
            "expenses", "recurring", "goals", "checkins", "freelanceProjects", "cryptoEntries", "savingsGoals")
    }
}
