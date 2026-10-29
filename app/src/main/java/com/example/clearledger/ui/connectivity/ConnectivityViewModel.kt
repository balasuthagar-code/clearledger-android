package com.example.clearledger.ui.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.network.NetworkMonitor
import com.example.clearledger.core.network.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ConnectivityViewModel @Inject constructor(monitor: NetworkMonitor) : ViewModel() {
    val status: StateFlow<NetworkStatus> = monitor.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NetworkStatus.Checking
    )
}
