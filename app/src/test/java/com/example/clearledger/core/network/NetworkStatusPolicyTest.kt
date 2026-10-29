package com.example.clearledger.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkStatusPolicyTest {
    @Test
    fun internetAndValidatedIsOnline() {
        assertEquals(NetworkStatus.Online, networkStatusForCapabilities(true, true))
    }

    @Test
    fun internetWithoutValidationIsOffline() {
        assertEquals(NetworkStatus.Offline, networkStatusForCapabilities(true, false))
    }

    @Test
    fun validationWithoutInternetIsOffline() {
        assertEquals(NetworkStatus.Offline, networkStatusForCapabilities(false, true))
    }

    @Test
    fun noCapabilitiesIsOffline() {
        assertEquals(NetworkStatus.Offline, networkStatusForCapabilities(false, false))
    }
}
