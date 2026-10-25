package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.Result
import com.example.clearledger.data.source.CorruptRateMemoryStorageException
import com.example.clearledger.data.source.RateMemoryStorage
import com.example.clearledger.di.RateMemoryIo
import com.example.clearledger.domain.model.*
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.domain.repository.RateMemoryClock
import com.example.clearledger.domain.repository.RateMemoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** DI supplies one repository/store. Per-instance mutex orders confirmation time and complete operations. */
class LocalRateMemoryRepository @Inject constructor(
    private val storage: RateMemoryStorage,
    private val account: RateMemoryAccount,
    private val clock: RateMemoryClock,
    @RateMemoryIo private val io: CoroutineDispatcher
) : RateMemoryRepository {
    private val operations = Mutex()

    override suspend fun lookup(context: RateMemoryContext): Result<RateMemoryLookup> = execute(context) {
        if (context.originalCurrency == context.baseCurrency) RateMemoryLookup.Identity
        else {
            val record = try { storage.read(RateMemoryCodec.key(context)) }
            catch (e: CancellationException) { throw e }
            catch (_: CorruptRateMemoryStorageException) { fail(RateMemoryFailure.CORRUPT_RECORD) }
            catch (_: Exception) { fail(RateMemoryFailure.READ_FAILED) }
            if (record == null) RateMemoryLookup.Missing
            else RateMemoryLookup.Remembered(RateMemoryCodec.decode(record))
        }
    }

    override suspend fun remember(context: RateMemoryContext, confirmedRateText: String): Result<RateMemoryWrite> = execute(context) {
        if (context.originalCurrency == context.baseCurrency) RateMemoryWrite.IdentityIgnored
        else {
            val plain = RateMemoryCodec.canonicalInput(confirmedRateText)
            val now = try { clock.nowEpochMillis() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(RateMemoryFailure.INVALID_CLOCK) }
            if (now < 0L) fail(RateMemoryFailure.INVALID_CLOCK)
            val rate = RememberedRate(plain, now)
            // Recheck immediately before storage dispatch; key is ALWAYS the captured explicit context.
            requireAccount(context.uid)
            try { storage.write(RateMemoryCodec.key(context), RateMemoryCodec.encode(rate)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(RateMemoryFailure.WRITE_FAILED) }
            RateMemoryWrite.Recorded(rate)
        }
    }

    private suspend fun <T> execute(context: RateMemoryContext, action: suspend () -> T): Result<T> = try {
        currentCoroutineContext().ensureActive()
        if (context.uid.length > RateMemoryCodec.MAX_UID_LENGTH || context.uid.isBlank()) fail(RateMemoryFailure.INVALID_UID)
        if (context.originalCurrency !in Currencies.supported || context.baseCurrency !in Currencies.supported) {
            fail(RateMemoryFailure.INVALID_PAIR)
        }
        requireAccount(context.uid, initial = true)
        val value = withContext(io) {
            operations.withLock {
                currentCoroutineContext().ensureActive()
                requireAccount(context.uid)
                action().also {
                    currentCoroutineContext().ensureActive()
                    requireAccount(context.uid)
                }
            }
        }
        // Also guard the caller-dispatcher resumption, not only the background I/O completion.
        currentCoroutineContext().ensureActive()
        requireAccount(context.uid)
        Result.Success(value)
    } catch (e: CancellationException) { throw e }
    catch (e: RateMemoryException) { Result.Error(e) }

    private fun requireAccount(uid: String, initial: Boolean = false) {
        val current = try { account.currentUid() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { fail(RateMemoryFailure.ACCOUNT_UNAVAILABLE) }
        if (current == null && initial) fail(RateMemoryFailure.SIGNED_OUT)
        if (current != uid) fail(RateMemoryFailure.ACCOUNT_CHANGED)
    }

    private fun fail(reason: RateMemoryFailure): Nothing = throw RateMemoryException(reason)
}
