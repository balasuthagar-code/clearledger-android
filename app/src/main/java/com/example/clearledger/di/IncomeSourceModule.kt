package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreIncomeSourceRepository
import com.example.clearledger.data.repository.IncomeSourceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IncomeSourceModule {

    @Provides
    @Singleton
    fun provideIncomeSourceRepository(): IncomeSourceRepository {
        return FirestoreIncomeSourceRepository()
    }
}