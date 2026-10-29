package com.example.clearledger.ui.connectivity

import com.example.clearledger.core.network.NetworkMonitor
import com.example.clearledger.core.network.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        private val statuses = MutableStateFlow(NetworkStatus.Checking)
        override val status: Flow<NetworkStatus> = statuses

        fun emit(value: NetworkStatus) {
            statuses.value = value
        }
    }

    @Test
    fun initialStateIsChecking() = runTest {
        val viewModel = ConnectivityViewModel(FakeNetworkMonitor())
        assertEquals(NetworkStatus.Checking, viewModel.status.value)
    }

    @Test
    fun onlineAndOfflineArePropagated() = runTest {
        val monitor = FakeNetworkMonitor()
        val viewModel = ConnectivityViewModel(monitor)
        runCurrent()

        monitor.emit(NetworkStatus.Online)
        runCurrent()
        assertEquals(NetworkStatus.Online, viewModel.status.value)

        monitor.emit(NetworkStatus.Offline)
        runCurrent()
        assertEquals(NetworkStatus.Offline, viewModel.status.value)
    }

    @Test
    fun identicalStatesDoNotCreateExtraUiTransitions() = runTest {
        val monitor = FakeNetworkMonitor()
        val viewModel = ConnectivityViewModel(monitor)
        val transitions = mutableListOf<NetworkStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.status.collect { transitions += it }
        }
        runCurrent()

        monitor.emit(NetworkStatus.Online)
        runCurrent()
        monitor.emit(NetworkStatus.Online)
        runCurrent()
        monitor.emit(NetworkStatus.Offline)
        runCurrent()
        monitor.emit(NetworkStatus.Offline)
        runCurrent()

        assertEquals(
            listOf(NetworkStatus.Checking, NetworkStatus.Online, NetworkStatus.Offline),
            transitions
        )
    }
}
