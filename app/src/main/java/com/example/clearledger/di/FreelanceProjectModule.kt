package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreFreelanceProjectRepository
import com.example.clearledger.data.repository.FreelanceProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FreelanceProjectModule {

    @Binds
    @Singleton
    abstract fun bindFreelanceProjectRepository(
        implementation: FirestoreFreelanceProjectRepository
    ): FreelanceProjectRepository
}
