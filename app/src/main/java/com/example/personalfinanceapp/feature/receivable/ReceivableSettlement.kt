package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Receivable
import java.math.BigDecimal

enum class ReceivableStatus { PENDING, PARTIALLY_PAID, SETTLED }

data class ReceivableSettlement(
    val receivable: Receivable,
    val payments: List<Income>,
    val expectedMinor: Long,
    val paidMinor: Long,
    val remainingMinor: Long,
    val status: ReceivableStatus
) {
    val expectedDecimal: String get() = expectedMinor.toMoneyDecimal()
    val paidDecimal: String get() = paidMinor.toMoneyDecimal()
    val remainingDecimal: String get() = remainingMinor.toMoneyDecimal()
}

object ReceivableSettlementEngine {
    fun calculate(receivable: Receivable, incomes: List<Income>): Result<ReceivableSettlement> = runCatching {
        val normalized = ReceivableValidator.normalize(receivable).getOrThrow()
        val linked = incomes.filter { it.receivableId == normalized.id }
        require(linked.map { it.id }.toSet().size == linked.size) {
            "Duplicate linked income IDs cannot be counted"
        }
        linked.forEach { income ->
            require(income.source == IncomeSource.FREELANCE && income.entryKind == IncomeEntryKind.RECEIPT) {
                "Only freelance receipts can pay a receivable"
            }
            require(income.money.originalCurrency == normalized.currency) {
                "Linked payment currency must match the receivable"
            }
        }

        val expectedMinor = normalized.expectedOriginalDecimal.toMinorExact()
        val paidMinor = linked.fold(0L) { total, income ->
            Math.addExact(total, income.money.originalAmountDecimal.toMinorExact())
        }
        val remainingMinor = if (paidMinor >= expectedMinor) 0L else Math.subtractExact(expectedMinor, paidMinor)
        val status = when {
            paidMinor == 0L -> ReceivableStatus.PENDING
            paidMinor < expectedMinor -> ReceivableStatus.PARTIALLY_PAID
            else -> ReceivableStatus.SETTLED
        }
        ReceivableSettlement(
            receivable = normalized,
            payments = linked.sortedWith(compareBy<Income> { it.occurredDate }.thenBy { it.id }),
            expectedMinor = expectedMinor,
            paidMinor = paidMinor,
            remainingMinor = remainingMinor,
            status = status
        )
    }
}

private fun String.toMinorExact(): Long = trim().toBigDecimal().movePointRight(2).longValueExact()
private fun Long.toMoneyDecimal(): String = BigDecimal.valueOf(this, 2).toPlainString()
