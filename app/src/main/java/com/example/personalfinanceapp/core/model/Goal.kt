
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

data class Goal(
    val id: DocumentId,
    val name: String,
    val targetAmountMinor: Long,
    val startDate: LocalDate,
    val targetDate: LocalDate,
    val archived: Boolean = false
)
