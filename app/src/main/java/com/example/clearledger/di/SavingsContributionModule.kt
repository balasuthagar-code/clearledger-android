package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreSavingsContributionRepository
import com.example.clearledger.data.repository.SavingsContributionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SavingsContributionModule {

    @Binds
    @Singleton
    abstract fun bindSavingsContributionRepository(
        repository: FirestoreSavingsContributionRepository
    ): SavingsContributionRepository
}