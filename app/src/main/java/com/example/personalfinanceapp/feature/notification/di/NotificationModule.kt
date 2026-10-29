package com.example.personalfinanceapp.feature.notification.di

import com.example.personalfinanceapp.feature.notification.FirestorePushTokenRepository
import com.example.personalfinanceapp.feature.notification.PushTokenRepository
import com.google.firebase.installations.FirebaseInstallations
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun bindPushTokenRepository(
        implementation: FirestorePushTokenRepository
    ): PushTokenRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseInstallations(): FirebaseInstallations = FirebaseInstallations.getInstance()
    }
}
