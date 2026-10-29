package com.example.personalfinanceapp.feature.goal.di

import com.example.personalfinanceapp.feature.goal.FirestoreGoalRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GoalModule {

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        impl: FirestoreGoalRepository
    ): GoalRepository
}
