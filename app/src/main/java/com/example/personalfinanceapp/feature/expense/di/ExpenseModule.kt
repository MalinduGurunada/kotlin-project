package com.example.personalfinanceapp.feature.expense.di

import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.expense.FirestoreExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: FirestoreExpenseRepository
    ): ExpenseRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }
    }
}
