package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.source.CorruptRateMemoryStorageException
import com.example.clearledger.data.source.RateMemoryStorage
import com.example.clearledger.domain.model.*
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.domain.repository.RateMemoryClock
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateMemoryRepositoryTest {
    private val usdLkr = RateMemoryContext("a", "USD", "LKR")
    private class Account(var uid: String? = "a") : RateMemoryAccount { override fun currentUid() = uid }
    private class Clock(var now: Long = 1_000L) : RateMemoryClock {
        var calls = 0
        override fun nowEpochMillis(): Long { calls++; return now }
    }
    private class Store : RateMemoryStorage {
        val disk = mutableMapOf<String, String>()
        var reads = 0
        var writes = 0
        var readError: Exception? = null
        var writeError: Exception? = null
        var readPause: CompletableDeferred<Unit>? = null
        var writePause: CompletableDeferred<Unit>? = null
        val writtenKeys = mutableListOf<String>()
        override suspend fun read(key: String): String? {
            reads++
            readPause?.await()
            readError?.let { throw it }
            return disk[key]
        }
        override suspend fun write(key: String, record: String) {
            writes++
            writtenKeys += key
            writePause?.await()
            writeError?.let { throw it }
            disk[key] = record
        }
    }
    private fun <T> value(result: Result<T>) = (result as Result.Success<T>).data
    private fun failure(expected: RateMemoryFailure, result: Result<*>) {
        assertEquals(expected, ((result as Result.Error).exception as RateMemoryException).failure)
    }

    @Test fun missingForeignRateIsNormalWithoutDefaultOne() = runTest {
        val store = Store()
        val repo = LocalRateMemoryRepository(store, Account(), Clock(), StandardTestDispatcher(testScheduler))
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(usdLkr)))
        assertEquals(0, store.writes)
    }

    @Test fun canonicalRateAndConfirmationTimestampRoundTripAcrossReconstruction() = runTest {
        val store = Store(); val account = Account(); val clock = Clock(123456L)
        val io = StandardTestDispatcher(testScheduler)
        val repo = LocalRateMemoryRepository(store, account, clock, io)
        val written = value(repo.remember(usdLkr, " +0300.50000 ")) as RateMemoryWrite.Recorded
        assertEquals(RememberedRate("300.5", 123456L), written.rate)
        val restored = LocalRateMemoryRepository(store, account, Clock(999999L), io)
        val found = value(restored.lookup(usdLkr)) as RateMemoryLookup.Remembered
        assertEquals(written.rate, found.rate)
        assertEquals("1|123456|300.5", store.disk.values.single())
    }

    @Test fun usersPairsAndReversePairsAreIsolated() = runTest {
        val store = Store(); val account = Account(); val clock = Clock()
        val repo = LocalRateMemoryRepository(store, account, clock, StandardTestDispatcher(testScheduler))
        value(repo.remember(usdLkr, "300"))
        val eurLkr = usdLkr.copy(originalCurrency = "EUR")
        val reverse = usdLkr.copy(originalCurrency = "LKR", baseCurrency = "USD")
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(eurLkr)))
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(reverse)))
        value(repo.remember(eurLkr, "320")); value(repo.remember(reverse, "0.0033"))
        account.uid = "b"
        val b = usdLkr.copy(uid = "b")
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(b)))
        value(repo.remember(b, "301"))
        account.uid = "a"
        assertEquals("300", (value(repo.lookup(usdLkr)) as RateMemoryLookup.Remembered).rate.ratePlain)
        assertEquals("320", (value(repo.lookup(eurLkr)) as RateMemoryLookup.Remembered).rate.ratePlain)
        assertEquals("0.0033", (value(repo.lookup(reverse)) as RateMemoryLookup.Remembered).rate.ratePlain)
        assertEquals(4, store.disk.size)
    }

    @Test fun identityNeitherReadsNorStoresNorTimestamps() = runTest {
        val store = Store(); store.readError = IOException(); store.writeError = IOException()
        val clock = Clock()
        val repo = LocalRateMemoryRepository(store, Account(), clock, StandardTestDispatcher(testScheduler))
        for (code in listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")) {
            val context = usdLkr.copy(originalCurrency = code, baseCurrency = code)
            assertEquals(RateMemoryLookup.Identity, value(repo.lookup(context)))
            assertEquals("1", RateMemoryLookup.Identity.ratePlain)
            assertEquals(RateMemoryWrite.IdentityIgnored, value(repo.remember(context, "invalid quote ignored")))
        }
        assertEquals(0, store.reads); assertEquals(0, store.writes); assertEquals(0, clock.calls)
    }

    @Test fun unsupportedPairInvalidUidAndNonPositiveRatesAreRejectedBeforeStorage() = runTest {
        val store = Store()
        val repo = LocalRateMemoryRepository(store, Account(), Clock(), StandardTestDispatcher(testScheduler))
        for (code in listOf("JPY", "usd", " USD", "")) {
            failure(RateMemoryFailure.INVALID_PAIR, repo.lookup(usdLkr.copy(originalCurrency = code)))
            failure(RateMemoryFailure.INVALID_PAIR, repo.remember(usdLkr.copy(baseCurrency = code), "1"))
        }
        failure(RateMemoryFailure.INVALID_UID, repo.lookup(usdLkr.copy(uid = "")))
        failure(RateMemoryFailure.INVALID_UID, repo.lookup(usdLkr.copy(uid = "x".repeat(257))))
        for (quote in listOf("", "abc", "0", "0.000", "-1", "1e2", "1,000", "1".repeat(129), "1.234567890".repeat(10))) {
            failure(RateMemoryFailure.INVALID_RATE, repo.remember(usdLkr, quote))
        }
        assertEquals(0, store.reads); assertEquals(0, store.writes)
    }

    @Test fun highPrecisionQuoteIsPreservedWithoutRounding() = runTest {
        val quote = "0.1234567890123456789012345678901234567890123456789"
        val repo = LocalRateMemoryRepository(Store(), Account(), Clock(), StandardTestDispatcher(testScheduler))
        value(repo.remember(usdLkr, quote))
        assertEquals(quote, (value(repo.lookup(usdLkr)) as RateMemoryLookup.Remembered).rate.ratePlain)
    }

    @Test fun corruptAndOversizedRecordsNeverBecomeMissingOrIdentity() = runTest {
        val store = Store(); val repo = LocalRateMemoryRepository(store, Account(), Clock(), StandardTestDispatcher(testScheduler))
        val key = RateMemoryCodec.key(usdLkr)
        for (record in listOf("", "2|1|300", "1|1|0", "1|1|-1", "1|1|1e2", "1|1|300.00",
            "1|-1|300", "1|01|300", "1|9223372036854775808|300", "1|1|NaN", "1|1|1|extra", "x".repeat(10_000))) {
            store.disk[key] = record
            failure(RateMemoryFailure.CORRUPT_RECORD, repo.lookup(usdLkr))
        }
        store.readError = CorruptRateMemoryStorageException()
        failure(RateMemoryFailure.CORRUPT_RECORD, repo.lookup(usdLkr))
    }

    @Test fun storageFailuresAreExplicitAndRetryable() = runTest {
        val store = Store(); val repo = LocalRateMemoryRepository(store, Account(), Clock(), StandardTestDispatcher(testScheduler))
        store.readError = IOException("sensitive underlying content must not be propagated")
        val read = repo.lookup(usdLkr)
        failure(RateMemoryFailure.READ_FAILED, read)
        assertEquals("READ_FAILED", (read as Result.Error).exception.message)
        store.readError = null
        store.writeError = IOException("write failed")
        failure(RateMemoryFailure.WRITE_FAILED, repo.remember(usdLkr, "300"))
        store.writeError = null
        value(repo.remember(usdLkr, "300"))
        assertTrue(value(repo.lookup(usdLkr)) is RateMemoryLookup.Remembered)
    }

    @Test fun signedOutAndWrongUserCannotEvenUseIdentity() = runTest {
        val account = Account(null)
        val repo = LocalRateMemoryRepository(Store(), account, Clock(), StandardTestDispatcher(testScheduler))
        failure(RateMemoryFailure.SIGNED_OUT, repo.lookup(usdLkr))
        failure(RateMemoryFailure.SIGNED_OUT, repo.remember(usdLkr.copy(originalCurrency = "LKR"), "1"))
        account.uid = "b"
        failure(RateMemoryFailure.ACCOUNT_CHANGED, repo.lookup(usdLkr))
    }

    @Test fun delayedReadAfterAccountSwitchIsNotAValidPrefill() = runTest {
        val store = Store(); val account = Account()
        val repo = LocalRateMemoryRepository(store, account, Clock(), StandardTestDispatcher(testScheduler))
        value(repo.remember(usdLkr, "300"))
        store.readPause = CompletableDeferred()
        val pending = async { repo.lookup(usdLkr) }; runCurrent()
        account.uid = "b"; store.readPause!!.complete(Unit)
        failure(RateMemoryFailure.ACCOUNT_CHANGED, pending.await())
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(usdLkr.copy(uid = "b"))))
    }

    @Test fun delayedWriteKeepsCapturedKeyAndDoesNotReportSuccessForNewUser() = runTest {
        val store = Store(); val account = Account()
        val repo = LocalRateMemoryRepository(store, account, Clock(), StandardTestDispatcher(testScheduler))
        store.writePause = CompletableDeferred()
        val pending = async { repo.remember(usdLkr, "300") }; runCurrent()
        assertFalse(pending.isCompleted)
        account.uid = "b"; store.writePause!!.complete(Unit)
        failure(RateMemoryFailure.ACCOUNT_CHANGED, pending.await())
        assertEquals(listOf(RateMemoryCodec.key(usdLkr)), store.writtenKeys)
        assertEquals(RateMemoryLookup.Missing, value(repo.lookup(usdLkr.copy(uid = "b"))))
        account.uid = "a"
        assertEquals("300", (value(repo.lookup(usdLkr)) as RateMemoryLookup.Remembered).rate.ratePlain)
    }

    @Test fun concurrentWritesFollowMutexAcquisitionOrderWithWholeRecords() = runTest {
        val store = Store(); val clock = Clock(100)
        val repo = LocalRateMemoryRepository(store, Account(), clock, StandardTestDispatcher(testScheduler))
        store.writePause = CompletableDeferred()
        val first = async { repo.remember(usdLkr, "300") }; runCurrent()
        val second = async { repo.remember(usdLkr, "301") }; runCurrent()
        assertEquals(1, store.writes); assertFalse(first.isCompleted); assertFalse(second.isCompleted)
        clock.now = 101
        store.writePause!!.complete(Unit)
        val one = value(first.await()) as RateMemoryWrite.Recorded
        val two = value(second.await()) as RateMemoryWrite.Recorded
        assertEquals(RememberedRate("300", 100), one.rate)
        assertEquals(RememberedRate("301", 101), two.rate)
        assertEquals(RememberedRate("301", 101), (value(repo.lookup(usdLkr)) as RateMemoryLookup.Remembered).rate)
        assertEquals("1|101|301", store.disk.values.single())
    }

    @Test fun cancellationIsRethrownAndNegativeClockDoesNotWrite() = runTest {
        val store = Store(); val clock = Clock(-1)
        val repo = LocalRateMemoryRepository(store, Account(), clock, StandardTestDispatcher(testScheduler))
        failure(RateMemoryFailure.INVALID_CLOCK, repo.remember(usdLkr, "300"))
        assertEquals(0, store.writes)
        store.readError = CancellationException("cancel")
        try { repo.lookup(usdLkr); fail("Expected cancellation") } catch (_: CancellationException) { /* expected */ }
    }
}
