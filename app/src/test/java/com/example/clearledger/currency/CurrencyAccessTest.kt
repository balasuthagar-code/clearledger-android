package com.example.clearledger.currency

import com.example.clearledger.domain.model.*
import com.example.clearledger.domain.repository.BaseCurrencySource
import com.example.clearledger.domain.repository.RateMemoryAccount
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyAccessTest {
    private val server = CurrencySnapshotMetadata(false,false)
    @Test fun explicitBaseAndServerVerificationRequired() {
        assertTrue(HistoricalBasePolicy.accepts("LKR"))
        for (raw in listOf(null, "", "USD", "lkr", 1, false)) assertFalse(HistoricalBasePolicy.accepts(raw))
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.SignedOut) is CurrencyAccess.SignedOut)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.MissingDocument("A",server)) is CurrencyAccess.Setup)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.Unconfigured("A",server)) is CurrencyAccess.Setup)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.Configured("A","LKR",server)) is CurrencyAccess.Ready)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.Configured("A","USD",server)) is CurrencyAccess.Blocked)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.Configured("A","LKR",CurrencySnapshotMetadata(true,false))) is CurrencyAccess.Blocked)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.Invalid("A",InvalidBaseCurrencyReason.BLANK,server)) is CurrencyAccess.Blocked)
        assertTrue(CurrencyAccessPolicy.resolve(BaseCurrencyState.ReadFailure("A",Exception())) is CurrencyAccess.Blocked)
    }
    @Test fun actualViewModelSetupFailureRetryAndAccountSwitch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = ViewModelStore()
        try {
            val sourceState = MutableStateFlow<BaseCurrencyState>(BaseCurrencyState.MissingDocument("A",server))
            val source = object : BaseCurrencySource { override fun observe() = sourceState }
            var uid: String? = "A"; var saves=0; var denied=true
            val setup = object : CurrencySetupStore {
                override suspend fun initializeLkr(uid:String) { saves++; if(denied) error("denied"); sourceState.value=BaseCurrencyState.Configured(uid,"LKR",server) }
                override suspend fun verifyHistory(uid:String) { if(denied) error("unknown history") }
            }
            val vm=CurrencyAccessViewModel(source,setup,RateMemoryAccount { uid },CurrencyLogout { uid=null; sourceState.value=BaseCurrencyState.SignedOut })
            owner.put("gate",vm); runCurrent()
            assertTrue(vm.state.value is CurrencyAccess.Setup)
            vm.setup("A"); vm.setup("A"); runCurrent()
            assertEquals(1,saves); assertTrue(vm.state.value is CurrencyAccess.Blocked)
            denied=false; vm.retry(); runCurrent(); vm.setup("A"); runCurrent()
            assertTrue(vm.state.value is CurrencyAccess.Ready)
            sourceState.value=BaseCurrencyState.Configured("A","LKR",CurrencySnapshotMetadata(true,false)); runCurrent()
            assertTrue(vm.state.value is CurrencyAccess.Ready) // same-session verified offline lease
            sourceState.value=BaseCurrencyState.Configured("B","USD",server); uid="B"; runCurrent()
            assertTrue(vm.state.value is CurrencyAccess.Blocked)
            vm.logout(); runCurrent(); assertEquals(CurrencyAccess.SignedOut,vm.state.value)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
}
