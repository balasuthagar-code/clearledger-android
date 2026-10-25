package com.example.clearledger.di

import com.example.clearledger.data.source.CurrencyAuthSource
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.domain.repository.RateMemoryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Optional access for existing non-Hilt Compose hosts during later manual integration. No UI is wired here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RateMemoryEntryPoint {
    fun rates(): RateMemoryRepository
    fun account(): RateMemoryAccount
    fun authEvents(): CurrencyAuthSource
}
