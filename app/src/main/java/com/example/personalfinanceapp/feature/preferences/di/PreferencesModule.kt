package com.example.personalfinanceapp.feature.preferences.di

import com.example.personalfinanceapp.feature.preferences.DataStorePreferencesRepository
import com.example.personalfinanceapp.feature.preferences.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: DataStorePreferencesRepository
    ): PreferencesRepository
}
