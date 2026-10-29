package com.example.clearledger.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.clearledger.core.network.NetworkMonitor
import com.example.clearledger.core.network.NetworkStatus
import com.example.clearledger.core.network.networkStatusForCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) : NetworkMonitor {
    private val connectivityManager = requireNotNull(context.getSystemService(ConnectivityManager::class.java))

    override val status: Flow<NetworkStatus> = callbackFlow {
        fun statusFor(capabilities: NetworkCapabilities?): NetworkStatus =
            networkStatusForCapabilities(
                hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            )

        fun currentStatus(): NetworkStatus {
            val capabilities = connectivityManager.activeNetwork
                ?.let { connectivityManager.getNetworkCapabilities(it) }
            return statusFor(capabilities)
        }

        trySend(currentStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(statusFor(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Offline)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        // Close the gap between the first snapshot and callback registration.
        trySend(currentStatus())
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
