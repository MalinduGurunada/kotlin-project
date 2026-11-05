package com.example.personalfinanceapp.feature.reminder.daily.di

import com.example.personalfinanceapp.feature.reminder.daily.DailyCaptureReminderStore
import com.example.personalfinanceapp.feature.reminder.daily.SharedPreferencesDailyCaptureReminderStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DailyCaptureReminderModule {
    @Binds
    @Singleton
    abstract fun bindDailyCaptureReminderStore(
        implementation: SharedPreferencesDailyCaptureReminderStore
    ): DailyCaptureReminderStore
}
