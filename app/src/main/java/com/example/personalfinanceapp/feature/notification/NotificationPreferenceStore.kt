package com.example.personalfinanceapp.feature.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPreferenceStore {
    fun isEnabled(userId: String): Boolean
    fun setEnabled(userId: String, enabled: Boolean)
}

@Singleton
class SharedPreferencesNotificationPreferenceStore @Inject constructor(
    @ApplicationContext context: Context
) : NotificationPreferenceStore {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun isEnabled(userId: String): Boolean =
        preferences.getBoolean(key(userId), true)

    override fun setEnabled(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean(key(userId), enabled).apply()
    }

    private fun key(userId: String): String = "$KEY_PREFIX$userId"

    private companion object {
        const val PREFERENCES_NAME = "notification_preferences"
        const val KEY_PREFIX = "enabled_"
    }
}

object NotificationDeliveryPolicy {
    fun shouldDisplay(
        authenticatedUserId: String?,
        payloadUserId: String,
        notificationsEnabled: Boolean
    ): Boolean = notificationsEnabled && authenticatedUserId == payloadUserId
}
