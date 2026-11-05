package com.example.personalfinanceapp.feature.reminder.daily

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DailyCaptureReminderSettings(
    val enabled: Boolean = false,
    val timeMinutes: Int = DEFAULT_TIME_MINUTES
) {
    init {
        require(timeMinutes in 0 until MINUTES_PER_DAY) {
            "Reminder time must be within a day"
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_TIME_MINUTES = 20 * 60
    }
}

interface DailyCaptureReminderStore {
    fun getSettings(userId: String): DailyCaptureReminderSettings
    fun setEnabled(userId: String, enabled: Boolean)
    fun setTimeMinutes(userId: String, timeMinutes: Int)
}

@Singleton
class SharedPreferencesDailyCaptureReminderStore @Inject constructor(
    @ApplicationContext context: Context
) : DailyCaptureReminderStore {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun getSettings(userId: String): DailyCaptureReminderSettings =
        DailyCaptureReminderSettings(
            enabled = preferences.getBoolean(enabledKey(userId), false),
            timeMinutes = preferences.getInt(
                timeKey(userId),
                DailyCaptureReminderSettings.DEFAULT_TIME_MINUTES
            ).coerceIn(0, DailyCaptureReminderSettings.MINUTES_PER_DAY - 1)
        )

    override fun setEnabled(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean(enabledKey(userId), enabled).apply()
    }

    override fun setTimeMinutes(userId: String, timeMinutes: Int) {
        require(timeMinutes in 0 until DailyCaptureReminderSettings.MINUTES_PER_DAY)
        preferences.edit().putInt(timeKey(userId), timeMinutes).apply()
    }

    private fun enabledKey(userId: String) = "${userId}_enabled"
    private fun timeKey(userId: String) = "${userId}_time_minutes"

    private companion object {
        const val PREFERENCES_NAME = "daily_capture_reminder"
    }
}

@Singleton
class DailyCaptureReminderDeliveryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun claimDelivery(userId: String, dateIso: String): Boolean {
        val key = "$userId:$dateIso"
        if (preferences.getBoolean(key, false)) return false
        return preferences.edit().putBoolean(key, true).commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_capture_reminder_deliveries"
    }
}
