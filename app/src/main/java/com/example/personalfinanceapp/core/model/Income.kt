
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
    LOSS,
    BREAK_EVEN
}

/**
 * Original-currency inputs used to derive one realised crypto result.
 *
 * The transaction's [Money] supplies the shared currency and historical exchange-rate snapshot.
 * Keeping proceeds here as calculation metadata prevents a sale/withdrawal from being counted as
 * a second receipt: only the derived net magnitude in [Income.money] enters income totals.
 */
data class CryptoRealizedDetails(
    val costBasisOriginalDecimal: String,
    val saleProceedsOriginalDecimal: String,
    val feesOriginalDecimal: String
)

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
    val cryptoRealizedDetails: CryptoRealizedDetails? = null,
    val note: String? = null
)
