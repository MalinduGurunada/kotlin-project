package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import java.time.LocalDate

data class CryptoResultTotals(
    val totalGainLkrMinor: Long = 0L,
    val totalLossLkrMinor: Long = 0L,
    val netLkrMinor: Long = 0L
)

fun Income.signedBaseAmountMinor(): Long = when {
    source != IncomeSource.CRYPTO || entryKind != IncomeEntryKind.REALIZED_PNL ->
        money.baseAmountMinor
    pnlDirection == PnlDirection.LOSS -> Math.negateExact(money.baseAmountMinor)
    pnlDirection == PnlDirection.BREAK_EVEN -> 0L
    else -> money.baseAmountMinor
}

object CryptoResultCalculator {

    fun calculate(
        incomes: List<Income>,
        startDateInclusive: LocalDate,
        endDateExclusive: LocalDate
    ): CryptoResultTotals {
        require(startDateInclusive.isBefore(endDateExclusive)) {
            "Crypto result start date must be before end date"
        }

        var gains = 0L
        var losses = 0L
        incomes.asSequence()
            .filter { income ->
                income.source == IncomeSource.CRYPTO &&
                    income.entryKind == IncomeEntryKind.REALIZED_PNL &&
                    !income.occurredDate.isBefore(startDateInclusive) &&
                    income.occurredDate.isBefore(endDateExclusive)
            }
            .forEach { income ->
                when (income.pnlDirection) {
                    PnlDirection.GAIN -> gains = Math.addExact(gains, income.money.baseAmountMinor)
                    PnlDirection.LOSS -> losses = Math.addExact(losses, income.money.baseAmountMinor)
                    PnlDirection.BREAK_EVEN, null -> Unit
                }
            }

        return CryptoResultTotals(
            totalGainLkrMinor = gains,
            totalLossLkrMinor = losses,
            netLkrMinor = Math.subtractExact(gains, losses)
        )
    }
}
