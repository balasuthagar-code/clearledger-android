package com.example.clearledger.currency

import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.repository.RateMemoryAccount
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RateSuggestionsTest {
    private val context = RateMemoryContext("A", "USD", "LKR")
    @Test fun manualTypingWinsEvenWhenTextIsClearedAgain() = runTest {
        val session = RateSuggestionSession(context, false)
        val delayed = CompletableDeferred<String>(); var field = ""
        val ticket = session.ticket()
        launch { val rate = delayed.await(); if (session.accepts(ticket,"A")) field = rate }
        runCurrent(); session.typed(); field = "123"; session.typed(); field = ""
        delayed.complete("300"); runCurrent(); assertEquals("", field)
    }
    @Test fun userPairAndEditModeFenceResults() {
        val old = RateSuggestionSession(context, false); val ticket = old.ticket()
        assertFalse(old.accepts(ticket,"B")); assertFalse(old.accepts(ticket,null))
        old.close(); assertFalse(old.accepts(ticket,"A"))
        val current = RateSuggestionSession(context.copy(originalCurrency="EUR"),false)
        assertEquals("EUR",current.context.originalCurrency)
        val edit = RateSuggestionSession(context,true)
        assertFalse(edit.accepts(edit.ticket(),"A")); assertFalse(edit.acceptsExplicit(edit.ticket(),"A"))
    }
    @Test fun explicitApiSuggestionDoesNotApplyAfterFurtherTyping() {
        val session = RateSuggestionSession(context,false)
        session.typed(); val ticket = session.ticket()
        assertTrue(session.acceptsExplicit(ticket,"A"))
        assertFalse(session.accepts(ticket,"A"))
        session.typed(); assertFalse(session.acceptsExplicit(ticket,"A"))
    }
    @Test fun postSaveFailureCannotRepeatFinancialWrite() = runTest {
        val memory = RecordingRateMemory().apply { fail=true }
        val recorder = testRateRecorder(memory)
        var financialWrites = 0
        val applied = recorder.capture("USD","LKR","300.500")
        financialWrites++ // The feature's agreed success boundary occurred once.
        assertFalse(recorder.afterSave(applied))
        assertEquals(1,financialWrites); assertEquals(1,memory.writes)
        assertEquals("300.5",memory.last!!.second)
        assertNull(recorder.capture("LKR","LKR","1"))
    }
    @Test fun capturesUidRatherThanLookingUpLaterAccount() = runTest {
        var uid = "A"
        val memory = RecordingRateMemory()
        val recorder = testRateRecorder(memory, RateMemoryAccount { uid })
        val capture = recorder.capture("USD","LKR","300")!!
        uid="B"
        assertFalse(recorder.stillCurrent(capture))
        assertEquals("A",capture.context.uid)
    }
    @Test fun projectionPreservesLongPrecision() {
        assertEquals(4_611_686_018_427_387_904L,ExactProjection.monthly(Long.MAX_VALUE,2))
        assertEquals(0L,ExactProjection.monthly(0,1))
    }
}
