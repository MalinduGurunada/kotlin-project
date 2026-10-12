
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

data class Receivable(
    val id: DocumentId,
    val title: String,
    val projectReference: String,
    val expectedOriginalDecimal: String,
    val currency: CurrencyCode,
    val expectedDate: LocalDate,
    val archived: Boolean = false
)
