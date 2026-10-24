package com.example.personalfinanceapp.feature.goal.di

import com.example.personalfinanceapp.feature.goal.FakeGoalRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoalModule {

    @Provides
    @Singleton
    fun provideGoalRepository(): GoalRepository {
        return FakeGoalRepository()
    }
}
