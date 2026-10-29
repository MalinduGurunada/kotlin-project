package com.example.personalfinanceapp.feature.dashboard.analytics

import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.SpendingClass
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

data class CryptoYtdResult(
    val totalGainLkrMinor: Long = 0L,
    val totalLossLkrMinor: Long = 0L,
    val netPnlLkrMinor: Long = 0L,
    val pnlLabel: String = "REALIZED GAIN"
)

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
        yearToDateCryptoIncomes: List<Income> = emptyList()
    ): MonthlyAnalyticsBreakdown {

        // Income Source Breakdown
        val totalIncomeMinor = currentMonthIncomes.sumOf { it.money.baseAmountMinor }
        val sourceBreakdown = if (totalIncomeMinor > 0) {
            currentMonthIncomes.groupBy { it.source }
                .map { (source, items) ->
                    val sum = items.sumOf { it.money.baseAmountMinor }
                    val pct = ((sum.toDouble() / totalIncomeMinor) * 100).toInt()
                    SourceBreakdownItem(source.name, sum, pct)
                }.sortedByDescending { it.totalLkrMinor }
        } else {
            emptyList()
        }

        // Expense Category Breakdown
        val totalExpenseMinor = currentMonthExpenses.sumOf { it.money.baseAmountMinor }
        val categoryBreakdown = if (totalExpenseMinor > 0) {
            currentMonthExpenses.groupBy { it.categoryId }
                .map { (catId, items) ->
                    val sum = items.sumOf { it.money.baseAmountMinor }
                    val catName = items.firstOrNull()?.categoryNameSnapshot ?: catId
                    val pct = ((sum.toDouble() / totalExpenseMinor) * 100).toInt()
                    CategoryBreakdownItem(catId, catName, sum, pct)
                }.sortedByDescending { it.totalLkrMinor }
        } else {
            emptyList()
        }

        // Committed vs Discretionary Spending Ratio
        val committedSum = currentMonthExpenses
            .filter { it.spendingClass == SpendingClass.COMMITTED }
            .sumOf { it.money.baseAmountMinor }
        val discretionarySum = currentMonthExpenses
            .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .sumOf { it.money.baseAmountMinor }

        val committedPct = if (totalExpenseMinor > 0) {
            ((committedSum.toDouble() / totalExpenseMinor) * 100).toInt()
        } else 0
        val discretionaryPct = if (totalExpenseMinor > 0) {
            100 - committedPct
        } else 0

        // Month-over-Month Comparison
        val prevExpenseMinor = previousMonthExpenses.sumOf { it.money.baseAmountMinor }
        val momChangePct = if (prevExpenseMinor > 0) {
            ((totalExpenseMinor - prevExpenseMinor).toDouble() / prevExpenseMinor) * 100.0
        } else 0.0

        // Crypto Year-to-Date Results
        val cryptoIncomes = yearToDateCryptoIncomes.filter { it.source == IncomeSource.CRYPTO }
        val gainMinor = cryptoIncomes
            .filter { it.pnlDirection == PnlDirection.GAIN }
            .sumOf { it.money.baseAmountMinor }
        val lossMinor = cryptoIncomes
            .filter { it.pnlDirection == PnlDirection.LOSS }
            .sumOf { it.money.baseAmountMinor }
        val netCryptoPnl = gainMinor - lossMinor
        val cryptoLabel = if (netCryptoPnl >= 0) "REALIZED NET GAIN" else "REALIZED NET LOSS"

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
                pnlLabel = cryptoLabel
            )
        )
    }
}
