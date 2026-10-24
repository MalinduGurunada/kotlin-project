package com.example.personalfinanceapp.feature.income.di

import com.example.personalfinanceapp.feature.income.FakeIncomeRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IncomeModule {

    @Provides
    @Singleton
    fun provideIncomeRepository(): IncomeRepository {
        return FakeIncomeRepository()
    }
}
