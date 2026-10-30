package com.example.personalfinanceapp.feature.recurring.di

import com.example.personalfinanceapp.feature.auth.AuthSessionCleanup
import com.example.personalfinanceapp.feature.recurring.FirestoreRecurringRepository
import com.example.personalfinanceapp.feature.recurring.RecurringReminderCoordinator
import com.example.personalfinanceapp.feature.recurring.RecurringReminderScheduler
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

    @Binds
    abstract fun bindAuthSessionCleanup(
        implementation: RecurringReminderScheduler
    ): AuthSessionCleanup

    @Binds
    abstract fun bindRecurringReminderCoordinator(
        implementation: RecurringReminderScheduler
    ): RecurringReminderCoordinator
}
