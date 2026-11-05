
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

enum class PaymentChannel {
    CASH,
    CARD,
    BANK,
    OTHER
}

data class Expense(
    val id: DocumentId,
    val categoryId: DocumentId,
    val categoryNameSnapshot: String,
    val spendingClass: SpendingClass,
    val paymentChannel: PaymentChannel,
    val money: Money,
    val occurredDate: LocalDate,
    val note: String? = null,
    val channelLabel: String? = null,
    val recurringExpenseId: DocumentId? = null,
    val recurringOccurrenceId: DocumentId? = null
)
