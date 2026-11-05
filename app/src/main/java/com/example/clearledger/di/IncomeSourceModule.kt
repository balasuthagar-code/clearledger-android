//
//package com.example.clearledger.di
package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreIncomeSourceRepository
import com.example.clearledger.data.repository.IncomeSourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncomeSourceModule {

    @Binds
    @Singleton
    abstract fun bindIncomeSourceRepository(
        implementation: FirestoreIncomeSourceRepository
    ): IncomeSourceRepository
}
