
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

enum class ContributionKind {
    OPENING,
    ALLOCATION,
    REVERSAL
}

data class Contribution(
    val id: DocumentId,
    val goalId: DocumentId,
    val amountMinor: Long,
    val kind: ContributionKind,
    val occurredDate: LocalDate,
    val reversesId: DocumentId? = null,
    val note: String? = null
)
