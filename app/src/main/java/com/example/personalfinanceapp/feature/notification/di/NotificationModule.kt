package com.example.personalfinanceapp.feature.notification.di

import com.example.personalfinanceapp.feature.notification.FirestorePushTokenRepository
import com.example.personalfinanceapp.feature.notification.NotificationPreferenceStore
import com.example.personalfinanceapp.feature.notification.PushTokenRepository
import com.example.personalfinanceapp.feature.notification.SharedPreferencesNotificationPreferenceStore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
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

    @Binds
    @Singleton
    abstract fun bindNotificationPreferenceStore(
        implementation: SharedPreferencesNotificationPreferenceStore
    ): NotificationPreferenceStore

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseInstallations(): FirebaseInstallations = FirebaseInstallations.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    }
}
