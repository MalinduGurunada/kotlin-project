package com.example.personalfinanceapp.feature.recurring.di

import com.example.personalfinanceapp.feature.recurring.FirestoreRecurringRepository
import com.example.personalfinanceapp.feature.recurring.RecurringRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecurringModule {

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(
        implementation: FirestoreRecurringRepository
    ): RecurringRepository
}
