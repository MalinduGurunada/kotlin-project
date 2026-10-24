package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.UserId
import java.time.LocalDate

enum class IncomeEditorMode {
    ADD,
    EDIT
}

data class IncomeFormErrors(
    val amount: String? = null,
    val transactionDate: String? = null,
    val pnlDirection: String? = null,
    val form: String? = null
) {
    val hasErrors: Boolean
        get() = amount != null ||
            transactionDate != null ||
            pnlDirection != null ||
            form != null
}

data class IncomeFormUiState(
    val mode: IncomeEditorMode,
    val draftId: String,
    val ownerId: UserId,
    val originalIncome: Income?,
    val selectedSource: IncomeSource,
    val amountText: String,
    val transactionDate: LocalDate?,
    val projectReference: String,
    val assetSymbol: String,
    val pnlDirection: PnlDirection?,
    val note: String,
    val errors: IncomeFormErrors = IncomeFormErrors(),
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    init {
        require(draftId.isNotBlank()) { "Income draft ID cannot be empty" }
        require(ownerId.isNotBlank()) { "Income draft owner cannot be empty" }
        require((mode == IncomeEditorMode.EDIT) == (originalIncome != null)) {
            "Edit mode requires the original income record"
        }
        if (originalIncome != null) {
            require(originalIncome.id == draftId) {
                "An edited income must keep its original ID"
            }
        }
    }
}
