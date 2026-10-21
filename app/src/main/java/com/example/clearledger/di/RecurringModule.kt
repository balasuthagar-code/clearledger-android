package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreRecurringRepository
import com.example.clearledger.domain.repository.RecurringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecurringModule {
    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: FirestoreRecurringRepository): RecurringRepository
}
