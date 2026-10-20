package com.example.clearledger.di

import com.example.clearledger.data.fake.FakeIncomeSummarySource
import com.example.clearledger.data.repository.FirestoreCategoryRepository
import com.example.clearledger.data.repository.FirestoreExpenseRepository
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import com.example.clearledger.domain.repository.IncomeSummarySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt which implementation to give when someone asks for an interface.
 * FirebaseAuth / FirebaseFirestore come from Member 1's FirebaseModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: FirestoreCategoryRepository): CategoryRepository

    // Sprint 2: expenses are now real (Firestore). The Fake is no longer bound.
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: FirestoreExpenseRepository): ExpenseRepository

    // TEMPORARY until Member 2 implements IncomeSummarySource on the real income repository
    @Binds
    @Singleton
    abstract fun bindIncomeSummarySource(impl: FakeIncomeSummarySource): IncomeSummarySource
}
