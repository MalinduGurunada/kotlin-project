package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.Receivable
import java.math.BigDecimal

object ReceivableValidator {
    private val amountPattern = Regex("[0-9]+([.][0-9]{1,2})?")

    fun normalize(receivable: Receivable): Result<Receivable> = runCatching {
        val title = receivable.title.trim()
        val projectReference = receivable.projectReference.trim()
        val amountText = receivable.expectedOriginalDecimal.trim()

        require(receivable.id.isNotBlank()) { "Receivable ID cannot be empty" }
        require(title.isNotBlank()) { "Title cannot be empty" }
        require(projectReference.isNotBlank()) { "Project reference cannot be empty" }
        require(title.length <= 200) { "Title is too long" }
        require(projectReference.length <= 200) { "Project reference is too long" }
        require(amountPattern.matches(amountText)) { "Expected amount must have at most two decimal places" }
        val amount = amountText.toBigDecimal()
        require(amount > BigDecimal.ZERO) { "Expected amount must be greater than zero" }
        amount.movePointRight(2).longValueExact()

        receivable.copy(
            title = title,
            projectReference = projectReference,
            expectedOriginalDecimal = amountText
        )
    }
}
