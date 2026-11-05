package com.example.personalfinanceapp.feature.notification

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceNotificationInstrumentedTest {
    @Test
    fun notificationTapParsesAppAndFirebaseConsoleIntentFormats() {
        val expected = FinanceReviewPayload(
            userId = "user-a",
            reviewId = "review-1",
            title = "Title",
            body = "Body"
        )
        val appIntent = expected.putInto(Intent())
        val consoleIntent = Intent()
            .putExtra("type", FinanceReviewPayload.TYPE_FINANCE_REVIEW)
            .putExtra("destination", FinanceReviewPayload.DESTINATION_ACTIVITY)
            .putExtra("userId", "user-a")
            .putExtra("reviewId", "review-1")

        assertEquals("user-a", FinanceReviewPayload.fromIntent(appIntent)?.userId)
        assertEquals("review-1", FinanceReviewPayload.fromIntent(consoleIntent)?.reviewId)
    }

    @Test
    fun notificationPreferenceIsPersistentAndUserScoped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = System.nanoTime().toString()
        val firstUser = "notification-user-a-$suffix"
        val secondUser = "notification-user-b-$suffix"
        val store = SharedPreferencesNotificationPreferenceStore(context)

        assertTrue(store.isEnabled(firstUser))
        store.setEnabled(firstUser, false)

        val reloadedStore = SharedPreferencesNotificationPreferenceStore(context)
        assertFalse(reloadedStore.isEnabled(firstUser))
        assertTrue(reloadedStore.isEnabled(secondUser))
    }
}
