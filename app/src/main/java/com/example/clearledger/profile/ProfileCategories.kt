package com.example.clearledger.profile

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileCategories @Inject constructor(private val store: ProfileStore,
    private val account: ProfileAccount, private val categories: CategoryRepository) {
    private val mutex = Mutex()
    suspend fun ensureReady(uid: String): Result<Unit> = mutex.withLock {
        try {
            account.requireOwner(uid)
            val profile = ProfileContract.decode(uid, store.read(uid))
            check(profile.complete) { "Complete your profile before continuing." }
            account.requireOwner(uid)
            if (!profile.categoriesReady) {
                when (val result = categories.seedDefaultCategoriesIfEmpty()) {
                    is Result.Error -> return@withLock result
                    is Result.Success -> Unit
                }
                account.requireOwner(uid)
                store.markCategoriesReady(uid, profile.storedCurrency)
            }
            account.requireOwner(uid)
            Result.Success(Unit)
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { Result.Error(e) }
    }
}
