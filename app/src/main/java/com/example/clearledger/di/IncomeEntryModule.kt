
package com.example.clearledger.di

import com.example.clearledger.data.repository.FirestoreIncomeEntryRepository
import com.example.clearledger.data.repository.IncomeEntryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IncomeEntryModule {

    @Provides
    @Singleton
    fun provideIncomeEntryRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): IncomeEntryRepository {
        return FirestoreIncomeEntryRepository(
            auth = auth,
            db = firestore
        )
    }
}
// thisanth