package com.example.personalfinanceapp.feature.preferences

import com.example.personalfinanceapp.core.model.PaymentChannel

data class UserPreferences(
    val userId: String = "",
    val defaultCategoryId: String = "food",
    val defaultPaymentChannel: PaymentChannel = PaymentChannel.CASH,
    val notificationsEnabled: Boolean = true,
    val reminderLeadDays: Int = 2
)
