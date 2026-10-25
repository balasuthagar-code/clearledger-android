package com.example.clearledger.currency

import android.content.Context
import com.example.clearledger.di.RateMemoryIo
import com.example.clearledger.domain.repository.RateMemoryClock
import com.example.clearledger.domain.repository.RateMemoryRepository
import com.example.clearledger.domain.repository.RateMemoryAccount
import com.example.clearledger.data.source.CurrencyAuthSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object CurrencyCompletionModule {
    @Provides fun setup(impl: FirestoreCurrencySetupStore): CurrencySetupStore = impl
    @Provides fun logout(auth: com.google.firebase.auth.FirebaseAuth): CurrencyLogout = CurrencyLogout { auth.signOut() }
    @Provides @Singleton fun hosted(@ApplicationContext context: Context,
        @RateMemoryIo io: CoroutineDispatcher, clock: RateMemoryClock): HostedRates =
        OpenAccessRates(HttpsRateTransport(io), PreferencesQuoteCache(context, io), clock)
}
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CurrencyCompletionEntryPoint {
    fun hosted(): HostedRates
    fun memory(): RateMemoryRepository
    fun account(): RateMemoryAccount
    fun authEvents(): CurrencyAuthSource
    fun recorder(): ConfirmedRateRecorder
}
