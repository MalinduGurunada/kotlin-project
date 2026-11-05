package com.example.personalfinanceapp.feature.preferences

import com.example.personalfinanceapp.core.model.PaymentChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferencesRepository @Inject constructor() : PreferencesRepository {

    private val userPrefsMap = MutableStateFlow<Map<String, UserPreferences>>(emptyMap())
    private val mutex = Mutex()

    override fun observeUserPreferences(userId: String): Flow<UserPreferences> {
        val currentMap = userPrefsMap.value
        val prefs = currentMap[userId] ?: UserPreferences(userId = userId)
        return MutableStateFlow(prefs).asStateFlow()
    }

    override suspend fun updateDefaultCategory(
        userId: String,
        categoryId: String
    ): Result<Unit> = mutex.withLock {
        val current = userPrefsMap.value.toMutableMap()
        val existing = current[userId] ?: UserPreferences(userId = userId)
        current[userId] = existing.copy(defaultCategoryId = categoryId)
        userPrefsMap.value = current
        Result.success(Unit)
    }

    override suspend fun updateDefaultPaymentChannel(
        userId: String,
        channel: PaymentChannel
    ): Result<Unit> = mutex.withLock {
        val current = userPrefsMap.value.toMutableMap()
        val existing = current[userId] ?: UserPreferences(userId = userId)
        current[userId] = existing.copy(defaultPaymentChannel = channel)
        userPrefsMap.value = current
        Result.success(Unit)
    }

    override suspend fun updateNotificationEnabled(
        userId: String,
        enabled: Boolean
    ): Result<Unit> = mutex.withLock {
        val current = userPrefsMap.value.toMutableMap()
        val existing = current[userId] ?: UserPreferences(userId = userId)
        current[userId] = existing.copy(notificationsEnabled = enabled)
        userPrefsMap.value = current
        Result.success(Unit)
    }

    override suspend fun updateReminderLeadDays(
        userId: String,
        leadDays: Int
    ): Result<Unit> = mutex.withLock {
        val current = userPrefsMap.value.toMutableMap()
        val existing = current[userId] ?: UserPreferences(userId = userId)
        current[userId] = existing.copy(reminderLeadDays = leadDays)
        userPrefsMap.value = current
        Result.success(Unit)
    }

    override suspend fun clearUserPreferencesOnLogout(
        userId: String
    ): Result<Unit> = mutex.withLock {
        val current = userPrefsMap.value.toMutableMap()
        current.remove(userId)
        userPrefsMap.value = current
        Result.success(Unit)
    }
}
