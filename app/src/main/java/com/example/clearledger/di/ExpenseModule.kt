package com.example.clearledger.di

import com.example.clearledger.data.fake.FakeExpenseRepository
import com.example.clearledger.data.fake.FakeIncomeSummarySource
import com.example.clearledger.data.repository.FirestoreCategoryRepository
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

    // TEMPORARY bindings - replaced in Sprint 2 (expenses) and by Member 2 (income)
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: FakeExpenseRepository): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindIncomeSummarySource(impl: FakeIncomeSummarySource): IncomeSummarySource
}
