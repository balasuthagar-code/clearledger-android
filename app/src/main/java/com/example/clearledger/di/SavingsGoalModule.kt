package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreSavingsGoalRepository
import com.example.clearledger.data.repository.SavingsGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SavingsGoalModule {

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(
        repository: FirestoreSavingsGoalRepository
    ): SavingsGoalRepository
}