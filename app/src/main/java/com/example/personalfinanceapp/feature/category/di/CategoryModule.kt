package com.example.personalfinanceapp.feature.category.di

import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.category.FirestoreCategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: FirestoreCategoryRepository
    ): CategoryRepository
}
