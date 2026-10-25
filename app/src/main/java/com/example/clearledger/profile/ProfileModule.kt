package com.example.clearledger.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds @Singleton abstract fun repository(impl: DefaultProfileRepository): ProfileRepository
    @Binds @Singleton abstract fun store(impl: FirestoreProfileStore): ProfileStore
    @Binds @Singleton abstract fun account(impl: FirebaseProfileAccount): ProfileAccount
}
