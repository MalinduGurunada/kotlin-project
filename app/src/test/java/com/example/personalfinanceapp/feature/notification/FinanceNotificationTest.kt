package com.example.personalfinanceapp.feature.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceNotificationTest {
    @Test
    fun validFinanceReviewPayloadIsParsed() {
        val payload = FinanceReviewPayload.parse(
            mapOf(
                "type" to "finance_review",
                "destination" to "activity",
                "userId" to "user-a",
                "reviewId" to "review-1",
                "title" to "Weekly review",
                "body" to "Check this week's spending."
            )
        )

        assertEquals("user-a", payload?.userId)
        assertEquals("review-1", payload?.reviewId)
        assertEquals("Weekly review", payload?.title)
    }

    @Test
    fun malformedOrUnsupportedPayloadIsRejected() {
        assertNull(FinanceReviewPayload.parse(emptyMap()))
        assertNull(FinanceReviewPayload.parse(mapOf(
            "type" to "finance_review",
            "destination" to "overview",
            "userId" to "user-a",
            "reviewId" to "review-1"
        )))
        assertNull(FinanceReviewPayload.parse(mapOf(
            "type" to "finance_review",
            "destination" to "activity",
            "userId" to " ",
            "reviewId" to "review-1"
        )))
    }

    @Test
    fun notificationNavigationWaitsForAuthAndRejectsCrossAccountTap() {
        val payload = FinanceReviewPayload("user-a", "review-1", "Title", "Body")

        assertEquals(
            FinanceReviewNavigationDecision.WAIT_FOR_AUTH,
            FinanceReviewNavigationResolver.resolve(payload, null)
        )
        assertEquals(
            FinanceReviewNavigationDecision.NAVIGATE_TO_ACTIVITY,
            FinanceReviewNavigationResolver.resolve(payload, "user-a")
        )
        assertEquals(
            FinanceReviewNavigationDecision.IGNORE,
            FinanceReviewNavigationResolver.resolve(payload, "user-b")
        )
    }

    @Test
    fun androidNotificationPermissionIsRequestedOnceAfterAuthentication() {
        assertTrue(NotificationPermissionPolicy.shouldRequest(
            sdkInt = 33,
            isGranted = false,
            wasRequested = false,
            isAuthenticated = true
        ))
        assertFalse(NotificationPermissionPolicy.shouldRequest(32, false, false, true))
        assertFalse(NotificationPermissionPolicy.shouldRequest(33, true, false, true))
        assertFalse(NotificationPermissionPolicy.shouldRequest(33, false, true, true))
        assertFalse(NotificationPermissionPolicy.shouldRequest(33, false, false, false))
    }

    @Test
    fun tokenRefreshDeletesOnlyTheSameOwnersPreviousToken() {
        assertEquals(
            "old-token",
            FcmTokenOwnershipPolicy.previousTokenToDelete(
                "user-a", "old-token", "user-a", "new-token"
            )
        )
        assertNull(FcmTokenOwnershipPolicy.previousTokenToDelete(
            "user-b", "old-token", "user-a", "new-token"
        ))
        assertNull(FcmTokenOwnershipPolicy.previousTokenToDelete(
            "user-a", "same-token", "user-a", "same-token"
        ))
    }

    @Test
    fun signOutCleanupUsesOnlyCurrentOwnersLocallyTrackedToken() {
        assertEquals(
            "owned-token",
            FcmTokenOwnershipPolicy.localTokenForCleanup(
                "user-a", "owned-token", "user-a"
            )
        )
        assertNull(FcmTokenOwnershipPolicy.localTokenForCleanup(
            "user-b", "other-token", "user-a"
        ))
    }
}
