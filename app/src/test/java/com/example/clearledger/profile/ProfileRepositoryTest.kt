package com.example.clearledger.profile

import com.example.clearledger.core.common.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {
    private fun repo(store: FakeProfileStore, account: FakeProfileAccount) =
        DefaultProfileRepository(store, account, CurrencyEligibilityPolicy(store, account))
    @Test fun emptyHistoryAllowsNewLkrAndCurrencyChange() = runTest {
        val store = FakeProfileStore(); val repo = repo(store, FakeProfileAccount())
        assertTrue(repo.save("A", " Nila ", "LKR", null) is Result.Success)
        assertTrue(repo.save("A", "Nila", "USD", "LKR") is Result.Success)
        assertEquals("USD", store.records["A"]?.get("baseCurrency"))
        assertEquals(CurrencyEligibilityPolicy.COLLECTIONS.toSet(), store.checks.map { it.second }.toSet())
    }
    @Test fun eachRequestedAndActualHistoryPathBlocksCurrencyChange() = runTest {
        for (path in CurrencyEligibilityPolicy.COLLECTIONS) {
            val store = FakeProfileStore().apply { records["A"] = completeFields(); history += "A" to path }
            val repo = repo(store, FakeProfileAccount())
            assertTrue(path, repo.save("A", "Nila", "USD", "LKR") is Result.Error)
            assertTrue(store.writes.isEmpty())
        }
    }
    @Test fun historyFailureNeverMeansEmpty() = runTest {
        val store = FakeProfileStore().apply { records["A"] = completeFields(); deniedCollection = "projects" }
        val repo = repo(store, FakeProfileAccount())
        assertTrue(repo.currencyEligibility("A") is Result.Error)
        assertTrue(repo.save("A", "Nila", "USD", "LKR") is Result.Error)
        assertTrue(store.writes.isEmpty())
    }
    @Test fun nameOnlyEditDoesNotRequireHistoryAndPreservesForeignCurrency() = runTest {
        val store = FakeProfileStore().apply { records["A"] = completeFields() + mapOf("baseCurrency" to "EUR", "custom" to 42); deniedCollection = "expenses" }
        val repo = repo(store, FakeProfileAccount())
        assertTrue(repo.save("A", " New Name ", "EUR", "EUR") is Result.Success)
        assertEquals("EUR", store.records["A"]?.get("baseCurrency"))
        assertEquals(42, store.records["A"]?.get("custom")); assertTrue(store.checks.isEmpty())
    }
    @Test fun staleCurrencyDoesNotOverwriteConcurrentChange() = runTest {
        val store = FakeProfileStore().apply { records["A"] = completeFields() + ("baseCurrency" to "EUR") }
        assertTrue(repo(store, FakeProfileAccount()).save("A", "Nila", "USD", "LKR") is Result.Error)
        assertTrue(store.writes.isEmpty())
    }
    @Test fun currencyChangedAfterHistoryCheckIsRejectedAtWrite() = runTest {
        val store = FakeProfileStore().apply { records["A"] = completeFields() }
        store.beforeSave = { store.records["A"] = completeFields() + ("baseCurrency" to "EUR") }
        assertTrue(repo(store, FakeProfileAccount()).save("A", "Nila", "USD", "LKR") is Result.Error)
        assertTrue(store.writes.isEmpty())
    }
    @Test fun accountMismatchAndSignedOutCannotAccess() = runTest {
        val store = FakeProfileStore(); val account = FakeProfileAccount(); val repo = repo(store, account)
        assertTrue(repo.save("B", "Nila", "LKR", null) is Result.Error)
        account.logout(); assertTrue(repo.load("A") is Result.Error); assertTrue(store.writes.isEmpty())
    }
    @Test fun delayedReadDoesNotReturnSuccessForChangedAccount() = runTest {
        val store = FakeProfileStore().apply { records["A"] = completeFields() }; val account = FakeProfileAccount()
        val gate = CompletableDeferred<Unit>(); store.beforeRead = { gate.await() }
        val result = async { repo(store, account).load("A") }; runCurrent()
        account.users.value = "B"; gate.complete(Unit)
        assertTrue(result.await() is Result.Error)
    }
    @Test fun delayedWriteUsesCapturedUidAndDoesNotReportSuccessAfterSwitch() = runTest {
        val store = FakeProfileStore(); val account = FakeProfileAccount()
        val gate = CompletableDeferred<Unit>(); store.beforeSave = { gate.await() }
        val result = async { repo(store, account).save("A", "Nila", "LKR", null) }; runCurrent()
        account.users.value = "B"; gate.complete(Unit)
        assertTrue(result.await() is Result.Error); assertFalse(store.records.containsKey("B"))
    }
    @Test fun cancellationPropagates() = runTest {
        val store = FakeProfileStore().apply { beforeRead = { throw CancellationException("Cancelled") } }
        try { repo(store, FakeProfileAccount()).load("A"); fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
    @Test fun invalidInputsNeverWrite() = runTest {
        val store = FakeProfileStore(); val repo = repo(store, FakeProfileAccount())
        assertTrue(repo.save("A", "", "LKR", null) is Result.Error)
        assertTrue(repo.save("A", "Nila", "XYZ", null) is Result.Error)
        assertTrue(store.writes.isEmpty())
    }
}
