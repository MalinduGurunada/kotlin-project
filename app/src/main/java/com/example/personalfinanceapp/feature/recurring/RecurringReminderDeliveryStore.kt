package com.example.personalfinanceapp.feature.recurring

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringReminderDeliveryStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun wasDelivered(userId: String, reminderId: String): Boolean =
        preferences.getBoolean(key(userId, reminderId), false)

    @Synchronized
    fun claimDelivery(userId: String, reminderId: String): Boolean {
        val key = key(userId, reminderId)
        if (preferences.getBoolean(key, false)) return false
        return preferences.edit().putBoolean(key, true).commit()
    }

    private fun key(userId: String, reminderId: String): String = "$userId:$reminderId"

    private companion object {
        const val PREFERENCES_NAME = "recurring_reminder_deliveries"
    }
}
