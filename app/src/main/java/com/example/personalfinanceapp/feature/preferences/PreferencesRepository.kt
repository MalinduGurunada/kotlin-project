package com.example.personalfinanceapp.feature.preferences

import com.example.personalfinanceapp.core.model.PaymentChannel
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    fun observeUserPreferences(userId: String): Flow<UserPreferences>

    suspend fun updateDefaultCategory(userId: String, categoryId: String): Result<Unit>

    suspend fun updateDefaultPaymentChannel(userId: String, channel: PaymentChannel): Result<Unit>

    suspend fun updateNotificationEnabled(userId: String, enabled: Boolean): Result<Unit>

    suspend fun updateReminderLeadDays(userId: String, leadDays: Int): Result<Unit>

    suspend fun clearUserPreferencesOnLogout(userId: String): Result<Unit>
}
