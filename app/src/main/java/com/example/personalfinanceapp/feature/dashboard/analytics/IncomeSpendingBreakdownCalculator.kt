package com.example.personalfinanceapp.feature.dashboard.analytics

import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.income.CryptoResultCalculator
import com.example.personalfinanceapp.feature.income.signedBaseAmountMinor
import com.example.personalfinanceapp.feature.category.effectiveSpendingClass
import java.math.BigInteger
import java.time.LocalDate

data class SourceBreakdownItem(
    val sourceName: String,
    val totalLkrMinor: Long,
    val percentage: Int
)

data class CategoryBreakdownItem(
    val categoryId: String,
    val categoryName: String,
    val totalLkrMinor: Long,
    val percentage: Int
)

enum class CryptoResultState(val label: String) {
    NO_ACTIVITY("NO ACTIVITY"),
    BREAK_EVEN("BREAK EVEN"),
    GAIN("REALIZED NET GAIN"),
    LOSS("REALIZED NET LOSS")
}

data class CryptoYtdResult(
    val totalGainLkrMinor: Long = 0L,
    val totalLossLkrMinor: Long = 0L,
    val netPnlLkrMinor: Long = 0L,
    val state: CryptoResultState = CryptoResultState.NO_ACTIVITY
) {
    val pnlLabel: String
        get() = state.label
}

data class MonthlyAnalyticsBreakdown(
    val incomeSourceBreakdown: List<SourceBreakdownItem> = emptyList(),
    val expenseCategoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val totalCommittedLkrMinor: Long = 0L,
    val committedPercent: Int = 0,
    val totalDiscretionaryLkrMinor: Long = 0L,
    val discretionaryPercent: Int = 0,
    val monthOverMonthOutflowChangePercent: Double = 0.0,
    val cryptoYtdResult: CryptoYtdResult = CryptoYtdResult()
)

object IncomeSpendingBreakdownCalculator {

    fun calculateBreakdown(
        currentMonthIncomes: List<Income>,
        currentMonthExpenses: List<Expense>,
        previousMonthExpenses: List<Expense> = emptyList(),
        yearToDateCryptoIncomes: List<Income> = emptyList(),
        categoryNamesById: Map<String, String> = emptyMap(),
        categorySpendingClassesById: Map<String, SpendingClass> = emptyMap()
    ): MonthlyAnalyticsBreakdown {

        // Income Source Breakdown
        val sourceTotals = currentMonthIncomes.groupBy { it.source }
            .map { (source, items) ->
                source to items.fold(0L) { total, income ->
                    Math.addExact(total, income.signedBaseAmountMinor())
                }
            }
        val totalIncomeActivity = sourceTotals.fold(BigInteger.ZERO) { total, (_, amount) ->
            total.add(BigInteger.valueOf(amount).abs())
        }
        val sourceBreakdown = if (totalIncomeActivity.signum() > 0) {
            sourceTotals.map { (source, sum) ->
                SourceBreakdownItem(
                    sourceName = source.name,
                    totalLkrMinor = sum,
                    percentage = percentageOf(BigInteger.valueOf(sum).abs(), totalIncomeActivity)
                )
            }.sortedByDescending { BigInteger.valueOf(it.totalLkrMinor).abs() }
        } else {
            emptyList()
        }

        // Expense Category Breakdown
        val totalExpenseMinor = currentMonthExpenses.fold(0L) { total, expense ->
            Math.addExact(total, expense.money.baseAmountMinor)
        }
        val categoryBreakdown = if (totalExpenseMinor > 0) {
            currentMonthExpenses.groupBy { it.categoryId }
                .map { (catId, items) ->
                    val sum = items.fold(0L) { total, expense ->
                        Math.addExact(total, expense.money.baseAmountMinor)
                    }
                    val catName = categoryNamesById[catId]
                        ?: items.firstOrNull()?.categoryNameSnapshot?.takeIf(String::isNotBlank)
                        ?: catId
                    val pct = percentageOf(
                        BigInteger.valueOf(sum),
                        BigInteger.valueOf(totalExpenseMinor)
                    )
                    CategoryBreakdownItem(catId, catName, sum, pct)
                }.sortedByDescending { it.totalLkrMinor }
        } else {
            emptyList()
        }

        // Committed vs Discretionary Spending Ratio
        val committedSum = currentMonthExpenses
            .filter {
                it.effectiveSpendingClass(categorySpendingClassesById) == SpendingClass.COMMITTED
            }
            .fold(0L) { total, expense -> Math.addExact(total, expense.money.baseAmountMinor) }
        val discretionarySum = currentMonthExpenses
            .filter {
                it.effectiveSpendingClass(categorySpendingClassesById) ==
                    SpendingClass.DISCRETIONARY
            }
            .fold(0L) { total, expense -> Math.addExact(total, expense.money.baseAmountMinor) }

        val committedPct = if (totalExpenseMinor > 0) {
            percentageOf(
                BigInteger.valueOf(committedSum),
                BigInteger.valueOf(totalExpenseMinor)
            )
        } else 0
        val discretionaryPct = if (totalExpenseMinor > 0) {
            100 - committedPct
        } else 0

        // Month-over-Month Comparison
        val prevExpenseMinor = previousMonthExpenses.fold(0L) { total, expense ->
            Math.addExact(total, expense.money.baseAmountMinor)
        }
        val momChangePct = if (prevExpenseMinor > 0) {
            (Math.subtractExact(totalExpenseMinor, prevExpenseMinor).toDouble() /
                prevExpenseMinor.toDouble()) * 100.0
        } else 0.0

        // Crypto Year-to-Date Results
        val cryptoActivity = yearToDateCryptoIncomes.filter {
            it.source == IncomeSource.CRYPTO && it.entryKind == IncomeEntryKind.REALIZED_PNL
        }
        val ytdBounds = cryptoActivity.map { it.occurredDate }
        val cryptoTotals = if (ytdBounds.isEmpty()) {
            com.example.personalfinanceapp.feature.income.CryptoResultTotals()
        } else {
            CryptoResultCalculator.calculate(
                incomes = cryptoActivity,
                startDateInclusive = ytdBounds.minOrNull()!!,
                endDateExclusive = ytdBounds.maxOrNull()!!.plusDays(1)
            )
        }
        val gainMinor = cryptoTotals.totalGainLkrMinor
        val lossMinor = cryptoTotals.totalLossLkrMinor
        val netCryptoPnl = cryptoTotals.netLkrMinor
        val cryptoState = when {
            cryptoActivity.isEmpty() -> CryptoResultState.NO_ACTIVITY
            netCryptoPnl > 0L -> CryptoResultState.GAIN
            netCryptoPnl < 0L -> CryptoResultState.LOSS
            else -> CryptoResultState.BREAK_EVEN
        }

        return MonthlyAnalyticsBreakdown(
            incomeSourceBreakdown = sourceBreakdown,
            expenseCategoryBreakdown = categoryBreakdown,
            totalCommittedLkrMinor = committedSum,
            committedPercent = committedPct,
            totalDiscretionaryLkrMinor = discretionarySum,
            discretionaryPercent = discretionaryPct,
            monthOverMonthOutflowChangePercent = momChangePct,
            cryptoYtdResult = CryptoYtdResult(
                totalGainLkrMinor = gainMinor,
                totalLossLkrMinor = lossMinor,
                netPnlLkrMinor = netCryptoPnl,
                state = cryptoState
            )
        )
    }

    private fun percentageOf(part: BigInteger, total: BigInteger): Int = if (total.signum() > 0) {
        part.multiply(BigInteger.valueOf(100L)).divide(total).toInt()
    } else {
        0
    }
}
