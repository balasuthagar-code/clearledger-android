package com.example.clearledger.data.repository

import com.example.clearledger.core.common.Result
import com.example.clearledger.data.source.CurrencyAuthSource
import com.example.clearledger.data.source.CurrencyDocument
import com.example.clearledger.data.source.CurrencyDocumentSource
import com.example.clearledger.data.source.CurrencySubscription
import com.example.clearledger.domain.model.BaseCurrencyState
import com.example.clearledger.domain.model.CurrencySnapshotMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservedBaseCurrencySourceTest {
    private val metadata = CurrencySnapshotMetadata(isFromCache = false, hasPendingWrites = false)
    private fun configured(uid: String, code: String) = BaseCurrencyState.Configured(uid, code, metadata)
    private fun snapshot(code: Any?, exists: Boolean = true) =
        Result.Success(CurrencyDocument(exists, code, metadata))

    private class Handle(val callback: (String?) -> Unit) : CurrencySubscription {
        var cancelled = false
        var cancelCount = 0
        override fun cancel() {
            if (!cancelled) { cancelled = true; cancelCount++ }
        }
    }

    private class FakeAuth(var uid: String?) : CurrencyAuthSource {
        val handles = mutableListOf<Handle>()
        override fun observeUid(onUid: (String?) -> Unit): CurrencySubscription {
            val handle = Handle(onUid)
            handles += handle
            onUid(uid)
            return handle
        }
        fun change(uid: String?) {
            this.uid = uid
            handles.filterNot { it.cancelled }.toList().forEach { it.callback(uid) }
        }
    }

    private class Registration(
        val uid: String,
        private val callback: (Result<CurrencyDocument>) -> Unit
    ) : CurrencySubscription {
        var cancelled = false
        var cancelCount = 0
        // Intentionally permits events after cancel, to exercise the repository's own guards.
        fun emit(value: Result<CurrencyDocument>) = callback(value)
        override fun cancel() {
            if (!cancelled) { cancelled = true; cancelCount++ }
        }
    }

    private class FakeDocuments : CurrencyDocumentSource {
        val registrations = mutableListOf<Registration>()
        var onObserve: ((Registration) -> Unit)? = null
        var setupError: Exception? = null
        override fun observe(uid: String, onResult: (Result<CurrencyDocument>) -> Unit): CurrencySubscription {
            setupError?.let { throw it }
            val registration = Registration(uid, onResult)
            registrations += registration
            onObserve?.invoke(registration)
            return registration
        }
        fun latest(uid: String) = registrations.last { it.uid == uid }
    }

    private class Fixture(uid: String?) {
        val auth = FakeAuth(uid)
        val documents = FakeDocuments()
        val source = ObservedBaseCurrencySource(auth, documents)
        val states = mutableListOf<BaseCurrencyState>()
        fun start(scope: CoroutineScope): Job = scope.launch {
            source.observe().collect { states += it }
        }
    }

    @Test fun signedOutHasNoDocumentSubscription() = runTest {
        val f = Fixture(null)
        val job = f.start(backgroundScope)
        runCurrent()
        assertEquals(listOf(BaseCurrencyState.SignedOut), f.states)
        assertTrue(f.documents.registrations.isEmpty())
        job.cancelAndJoin()
        assertTrue(f.auth.handles.single().cancelled)
    }

    @Test fun loadingThenConfiguredLkrAndProfileUpdates() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope)
        runCurrent()
        assertEquals(listOf(BaseCurrencyState.Loading("a")), f.states)
        f.documents.latest("a").emit(snapshot("LKR"))
        runCurrent()
        assertEquals(configured("a", "LKR"), f.states.last())
        f.documents.latest("a").emit(snapshot("EUR"))
        runCurrent()
        assertEquals(configured("a", "EUR"), f.states.last())
        assertEquals(1, f.documents.registrations.size)
    }

    @Test fun missingNullInvalidAndNonLkrSnapshotsRemainDistinct() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope)
        runCurrent()
        val listener = f.documents.latest("a")
        listener.emit(snapshot(null, exists = false)); runCurrent()
        assertEquals(BaseCurrencyState.MissingDocument("a", metadata), f.states.last())
        listener.emit(snapshot(null)); runCurrent()
        assertEquals(BaseCurrencyState.Unconfigured("a", metadata), f.states.last())
        listener.emit(snapshot(7L)); runCurrent()
        assertTrue(f.states.last() is BaseCurrencyState.Invalid)
        listener.emit(snapshot("USD")); runCurrent()
        assertEquals(configured("a", "USD"), f.states.last())
    }

    @Test fun switchingUsersClearsOldStateAndCancelsOldListener() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val a = f.documents.latest("a")
        a.emit(snapshot("USD")); runCurrent()
        f.auth.change("b"); runCurrent()
        assertTrue(a.cancelled)
        assertEquals(BaseCurrencyState.Loading("b"), f.states.last())
        val count = f.states.size
        a.emit(snapshot("GBP")); runCurrent()
        a.emit(Result.Error(IllegalStateException("late a failure"))); runCurrent()
        assertEquals(count, f.states.size)
        f.documents.latest("b").emit(snapshot("INR")); runCurrent()
        assertEquals(configured("b", "INR"), f.states.last())
    }

    @Test fun logoutClearsPriorCurrencyAndRejectsLateResults() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val listener = f.documents.latest("a")
        listener.emit(snapshot("USD")); runCurrent()
        f.auth.change(null); runCurrent()
        assertEquals(BaseCurrencyState.SignedOut, f.states.last())
        assertTrue(listener.cancelled)
        val count = f.states.size
        listener.emit(snapshot("LKR")); runCurrent()
        assertEquals(count, f.states.size)
    }

    @Test fun sameUidNewSessionRejectsFirstSessionsLateCallbacks() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val firstA = f.documents.latest("a")
        f.auth.change("b"); runCurrent()
        val b = f.documents.latest("b")
        f.auth.change("a"); runCurrent()
        val secondA = f.documents.latest("a")
        assertNotSame(firstA, secondA)
        assertTrue(firstA.cancelled && b.cancelled)
        firstA.emit(snapshot("USD")); runCurrent()
        assertEquals(BaseCurrencyState.Loading("a"), f.states.last())
        secondA.emit(snapshot("AUD")); runCurrent()
        assertEquals(configured("a", "AUD"), f.states.last())
    }

    @Test fun readFailureDoesNotFallbackAndRetryRecollectsSameUid() = runTest {
        val f = Fixture("a")
        val firstJob = f.start(backgroundScope); runCurrent()
        val first = f.documents.latest("a")
        first.emit(snapshot("USD")); runCurrent()
        val error = IllegalStateException("permission denied")
        first.emit(Result.Error(error)); runCurrent()
        assertEquals(BaseCurrencyState.ReadFailure("a", error), f.states.last())
        assertTrue(first.cancelled)
        first.emit(snapshot("LKR")); runCurrent()
        assertEquals(BaseCurrencyState.ReadFailure("a", error), f.states.last())
        firstJob.cancelAndJoin()
        f.start(backgroundScope); runCurrent()
        assertEquals(BaseCurrencyState.Loading("a"), f.states.last())
        val retry = f.documents.latest("a")
        assertNotSame(first, retry)
        retry.emit(snapshot("GBP")); runCurrent()
        assertEquals(configured("a", "GBP"), f.states.last())
        assertEquals(2, f.auth.handles.size)
        assertTrue(f.auth.handles.first().cancelled)
    }

    @Test fun duplicateAuthCallbackDoesNotChurnListener() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val registration = f.documents.latest("a")
        registration.emit(snapshot("USD")); runCurrent()
        f.auth.change("a"); runCurrent()
        assertEquals(1, f.documents.registrations.size)
        assertFalse(registration.cancelled)
        assertEquals(configured("a", "USD"), f.states.last())
    }

    @Test fun cancellationRemovesBothSubscriptionsAndGuardsQueuedCallbacks() = runTest {
        val f = Fixture("a")
        val job = f.start(backgroundScope); runCurrent()
        val listener = f.documents.latest("a")
        val auth = f.auth.handles.single()
        job.cancelAndJoin()
        assertEquals(1, listener.cancelCount)
        assertEquals(1, auth.cancelCount)
        val count = f.states.size
        listener.emit(snapshot("USD"))
        auth.callback("b")
        runCurrent()
        assertEquals(count, f.states.size)
        assertEquals(1, f.documents.registrations.size)
    }

    @Test fun synchronousRegistrationErrorStillCancelsReturnedHandle() = runTest {
        val f = Fixture("a")
        val error = IllegalStateException("synchronous failure")
        f.documents.onObserve = { it.emit(Result.Error(error)) }
        val job = f.start(backgroundScope); runCurrent()
        assertEquals(BaseCurrencyState.ReadFailure("a", error), f.states.last())
        assertTrue(f.documents.latest("a").cancelled)
        job.cancelAndJoin()
        assertEquals(1, f.documents.latest("a").cancelCount)
    }

    @Test fun userSwitchDuringRegistrationDoesNotInstallOldHandle() = runTest {
        val f = Fixture("a")
        f.documents.onObserve = { registration ->
            if (registration.uid == "a") f.auth.change("b")
        }
        f.start(backgroundScope); runCurrent()
        assertTrue(f.documents.latest("a").cancelled)
        assertFalse(f.documents.latest("b").cancelled)
        assertEquals(BaseCurrencyState.Loading("b"), f.states.last())
        f.documents.latest("a").emit(snapshot("LKR")); runCurrent()
        assertEquals(BaseCurrencyState.Loading("b"), f.states.last())
        f.documents.latest("b").emit(snapshot("EUR")); runCurrent()
        assertEquals(configured("b", "EUR"), f.states.last())
    }

    @Test fun setupFailureIsReadFailureAndRecollectionRetries() = runTest {
        val f = Fixture("a")
        val error = IllegalStateException("cannot register")
        f.documents.setupError = error
        val first = f.start(backgroundScope); runCurrent()
        assertEquals(BaseCurrencyState.ReadFailure("a", error), f.states.last())
        first.cancelAndJoin()
        f.documents.setupError = null
        f.start(backgroundScope); runCurrent()
        assertEquals(BaseCurrencyState.Loading("a"), f.states.last())
        f.documents.latest("a").emit(snapshot("USD")); runCurrent()
        assertEquals(configured("a", "USD"), f.states.last())
    }

    @Test fun metadataOnlyUpdateRemainsObservable() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val listener = f.documents.latest("a")
        val cached = CurrencySnapshotMetadata(true, true)
        listener.emit(Result.Success(CurrencyDocument(true, "USD", cached))); runCurrent()
        assertEquals(BaseCurrencyState.Configured("a", "USD", cached), f.states.last())
        listener.emit(snapshot("USD")); runCurrent()
        assertEquals(configured("a", "USD"), f.states.last())
    }

    @Test fun authChangeRecoversFromPreviousUsersReadFailure() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        f.documents.latest("a").emit(Result.Error(IllegalStateException("denied")))
        runCurrent()
        assertTrue(f.states.last() is BaseCurrencyState.ReadFailure)
        f.auth.change("b"); runCurrent()
        assertEquals(BaseCurrencyState.Loading("b"), f.states.last())
        f.documents.latest("b").emit(snapshot("INR")); runCurrent()
        assertEquals(configured("b", "INR"), f.states.last())
    }

    @Test fun collectorsOwnIndependentListeners() = runTest {
        val f = Fixture("a")
        val first = f.start(backgroundScope); runCurrent()
        val firstRegistration = f.documents.latest("a")
        val otherStates = mutableListOf<BaseCurrencyState>()
        val second = backgroundScope.launch { f.source.observe().collect { otherStates += it } }
        runCurrent()
        val secondRegistration = f.documents.latest("a")
        assertNotSame(firstRegistration, secondRegistration)
        first.cancelAndJoin()
        assertTrue(firstRegistration.cancelled)
        assertFalse(secondRegistration.cancelled)
        secondRegistration.emit(snapshot("GBP")); runCurrent()
        assertEquals(configured("a", "GBP"), otherStates.last())
        second.cancelAndJoin()
        assertTrue(secondRegistration.cancelled)
        assertTrue(f.auth.handles.all { it.cancelled })
    }

    @Test fun immediateErrorConsumerCanSwitchAccountWithoutTerminatingNewSession() = runTest {
        val f = Fixture("a")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            f.source.observe().collect { state ->
                f.states += state
                if (state is BaseCurrencyState.ReadFailure) f.auth.change("b")
            }
        }
        runCurrent()
        val a = f.documents.latest("a")
        a.emit(Result.Error(IllegalStateException("denied"))); runCurrent()
        assertTrue(a.cancelled)
        assertEquals(BaseCurrencyState.Loading("b"), f.states.last())
        val b = f.documents.latest("b")
        assertFalse(b.cancelled)
        b.emit(snapshot("EUR")); runCurrent()
        assertEquals(configured("b", "EUR"), f.states.last())
    }

    @Test fun cancellationExceptionIsNotCurrencyFailure() = runTest {
        val f = Fixture("a")
        f.start(backgroundScope); runCurrent()
        val listener = f.documents.latest("a")
        listener.emit(Result.Error(CancellationException("cancel"))); runCurrent()
        assertTrue(listener.cancelled)
        assertTrue(f.auth.handles.single().cancelled)
        assertFalse(f.states.any { it is BaseCurrencyState.ReadFailure })
    }
}
