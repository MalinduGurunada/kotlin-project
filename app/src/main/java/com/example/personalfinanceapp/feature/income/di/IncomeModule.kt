package com.example.personalfinanceapp.feature.income.di

import com.example.personalfinanceapp.feature.income.FirestoreIncomeRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncomeModule {

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(
        impl: FirestoreIncomeRepository
    ): IncomeRepository
}
