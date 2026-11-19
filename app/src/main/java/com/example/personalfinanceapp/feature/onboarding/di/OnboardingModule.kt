package com.example.personalfinanceapp.feature.onboarding.di

import com.example.personalfinanceapp.feature.onboarding.FirestoreOnboardingRepository
import com.example.personalfinanceapp.feature.onboarding.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        implementation: FirestoreOnboardingRepository
    ): OnboardingRepository
}
