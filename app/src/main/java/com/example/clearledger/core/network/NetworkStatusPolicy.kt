package com.example.clearledger.core.network

fun networkStatusForCapabilities(hasInternet: Boolean, isValidated: Boolean): NetworkStatus =
    if (hasInternet && isValidated) NetworkStatus.Online else NetworkStatus.Offline
