package com.example.personalfinanceapp.feature.notification

import android.content.Intent
import android.os.Build

data class FinanceReviewPayload(
    val userId: String,
    val reviewId: String,
    val title: String,
    val body: String
) {
    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_TYPE, TYPE_FINANCE_REVIEW)
        .putExtra(EXTRA_USER_ID, userId)
        .putExtra(EXTRA_REVIEW_ID, reviewId)

    companion object {
        const val TYPE_FINANCE_REVIEW = "finance_review"
        const val DESTINATION_ACTIVITY = "activity"
        private const val EXTRA_TYPE = "finance_notification_type"
        private const val EXTRA_USER_ID = "finance_notification_user_id"
        private const val EXTRA_REVIEW_ID = "finance_notification_review_id"

        fun parse(data: Map<String, String>): FinanceReviewPayload? {
            if (data["type"] != TYPE_FINANCE_REVIEW) return null
            if (data["destination"] != DESTINATION_ACTIVITY) return null
            val userId = data["userId"]?.trim().validIdentifier() ?: return null
            val reviewId = data["reviewId"]?.trim().validIdentifier() ?: return null
            val title = data["title"]?.trim().boundedText(100)
                ?: "Finance review ready"
            val body = data["body"]?.trim().boundedText(300)
                ?: "Review your recent finance activity in FinClarity."
            return FinanceReviewPayload(userId, reviewId, title, body)
        }

        fun fromIntent(intent: Intent?): FinanceReviewPayload? {
            if (intent?.getStringExtra(EXTRA_TYPE) != TYPE_FINANCE_REVIEW) return null
            val userId = intent.getStringExtra(EXTRA_USER_ID)?.trim().validIdentifier()
                ?: return null
            val reviewId = intent.getStringExtra(EXTRA_REVIEW_ID)?.trim().validIdentifier()
                ?: return null
            return FinanceReviewPayload(userId, reviewId, "", "")
        }

        private fun String?.validIdentifier(): String? =
            this?.takeIf { it.isNotBlank() && it.length <= 128 }

        private fun String?.boundedText(maxLength: Int): String? =
            this?.takeIf { it.isNotBlank() && it.length <= maxLength }
    }
}

enum class FinanceReviewNavigationDecision {
    WAIT_FOR_AUTH,
    NAVIGATE_TO_ACTIVITY,
    IGNORE
}

object FinanceReviewNavigationResolver {
    fun resolve(
        payload: FinanceReviewPayload,
        authenticatedUserId: String?
    ): FinanceReviewNavigationDecision = when {
        authenticatedUserId == null -> FinanceReviewNavigationDecision.WAIT_FOR_AUTH
        payload.userId == authenticatedUserId ->
            FinanceReviewNavigationDecision.NAVIGATE_TO_ACTIVITY
        else -> FinanceReviewNavigationDecision.IGNORE
    }
}

object NotificationPermissionPolicy {
    fun shouldRequest(
        sdkInt: Int = Build.VERSION.SDK_INT,
        isGranted: Boolean,
        wasRequested: Boolean,
        isAuthenticated: Boolean
    ): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        !isGranted &&
        !wasRequested &&
        isAuthenticated
}
