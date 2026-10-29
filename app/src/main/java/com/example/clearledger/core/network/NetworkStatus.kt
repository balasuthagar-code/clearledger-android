package com.example.clearledger.core.network

import kotlinx.coroutines.flow.Flow

enum class NetworkStatus {
    Checking,
    Online,
    Offline
}

interface NetworkMonitor {
    val status: Flow<NetworkStatus>
}
