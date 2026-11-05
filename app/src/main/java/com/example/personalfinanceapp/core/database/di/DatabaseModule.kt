package com.example.personalfinanceapp.core.database.di

import com.example.personalfinanceapp.core.database.AppDatabase
import com.example.personalfinanceapp.core.database.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(): AppDatabase {
        return AppDatabase()
    }

    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao
    }
}
