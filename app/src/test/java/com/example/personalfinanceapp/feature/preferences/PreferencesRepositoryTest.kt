package com.example.personalfinanceapp.feature.preferences

import com.example.personalfinanceapp.core.model.PaymentChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferencesRepositoryTest {

    private lateinit var repository: DataStorePreferencesRepository

    @Before
    fun setUp() {
        repository = DataStorePreferencesRepository()
    }

    @Test
    fun updateDefaultCategoryAndChannel() = runTest {
        repository.updateDefaultCategory("user-1", "utilities")
        repository.updateDefaultPaymentChannel("user-1", PaymentChannel.CARD)

        val prefs = repository.observeUserPreferences("user-1").first()
        assertEquals("utilities", prefs.defaultCategoryId)
        assertEquals(PaymentChannel.CARD, prefs.defaultPaymentChannel)
    }

    @Test
    fun updateNotificationToggles() = runTest {
        repository.updateNotificationEnabled("user-1", false)

        val prefs = repository.observeUserPreferences("user-1").first()
        assertFalse(prefs.notificationsEnabled)
    }

    @Test
    fun clearPreferencesOnLogout() = runTest {
        repository.updateDefaultCategory("user-1", "utilities")
        val result = repository.clearUserPreferencesOnLogout("user-1")

        assertTrue(result.isSuccess)
    }
}
