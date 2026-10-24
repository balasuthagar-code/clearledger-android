package com.example.clearledger.di

import com.example.clearledger.data.repository.ObservedBaseCurrencySource
import com.example.clearledger.data.source.CurrencyAuthSource
import com.example.clearledger.data.source.CurrencyDocumentSource
import com.example.clearledger.data.source.FirebaseCurrencyAuthSource
import com.example.clearledger.data.source.FirestoreCurrencyDocumentSource
import com.example.clearledger.domain.repository.BaseCurrencySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Adds only currency-source bindings. Firebase SDK instances come from existing FirebaseModule. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BaseCurrencyModule {
    @Binds
    @Singleton
    abstract fun bindBaseCurrencySource(impl: ObservedBaseCurrencySource): BaseCurrencySource

    @Binds
    @Singleton
    abstract fun bindCurrencyAuthSource(impl: FirebaseCurrencyAuthSource): CurrencyAuthSource

    @Binds
    @Singleton
    abstract fun bindCurrencyDocumentSource(impl: FirestoreCurrencyDocumentSource): CurrencyDocumentSource
}
