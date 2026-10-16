package com.example.personalfinanceapp.feature.category.di

import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.category.FakeCategoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoryModule {

    @Provides
    @Singleton
    fun provideCategoryRepository(): CategoryRepository {
        return FakeCategoryRepository()
    }
}
