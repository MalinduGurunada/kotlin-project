
package com.example.personalfinanceapp.core.model

import java.time.LocalDate

enum class IncomeSource {
    SALARY,
    FREELANCE,
    ADSENSE,
    CRYPTO
}

enum class IncomeEntryKind {
    RECEIPT,
    REALIZED_PNL
}

enum class PnlDirection {
    GAIN,
    LOSS
}

data class Income(
    val id: DocumentId,
    val source: IncomeSource,
    val entryKind: IncomeEntryKind,
    val money: Money,
    val occurredDate: LocalDate,
    val pnlDirection: PnlDirection? = null,
    val projectReference: String? = null,
    val receivableId: DocumentId? = null,
    val assetSymbol: String? = null,
    val note: String? = null
)
