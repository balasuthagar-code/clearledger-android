package com.example.clearledger.di

import android.content.Context
import com.example.clearledger.data.repository.LocalRateMemoryRepository
import com.example.clearledger.data.source.FirebaseRateMemoryAccount
import com.example.clearledger.data.source.RateMemoryStorage
import com.example.clearledger.data.source.SharedPreferencesRateMemoryStorage
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.domain.repository.RateMemoryClock
import com.example.clearledger.domain.repository.RateMemoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RateMemoryIo

@Module
@InstallIn(SingletonComponent::class)
abstract class RateMemoryModule {
    @Binds @Singleton
    abstract fun bindRepository(impl: LocalRateMemoryRepository): RateMemoryRepository
    @Binds @Singleton
    abstract fun bindAccount(impl: FirebaseRateMemoryAccount): RateMemoryAccount

    companion object {
        @Provides @RateMemoryIo
        fun provideIo(): CoroutineDispatcher = Dispatchers.IO

        @Provides @Singleton
        fun provideClock(): RateMemoryClock = RateMemoryClock { System.currentTimeMillis() }

        @Provides @Singleton
        fun provideStorage(
            @ApplicationContext context: Context,
            @RateMemoryIo io: CoroutineDispatcher
        ): RateMemoryStorage = SharedPreferencesRateMemoryStorage(context, io)
    }
}
