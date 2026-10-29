package com.example.clearledger.di

import com.example.clearledger.core.network.NetworkMonitor
import com.example.clearledger.data.network.AndroidNetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(implementation: AndroidNetworkMonitor): NetworkMonitor
}
