package com.example.personalfinanceapp.feature.dashboard.analytics

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IncomeSpendingCalculatorTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun createExpense(
        id: String,
        categoryId: String,
        amountMinor: Long,
        spendingClass: SpendingClass
    ): Expense {
        return Expense(
            id = id,
            categoryId = categoryId,
            categoryNameSnapshot = categoryId.replaceFirstChar { it.uppercase() },
            spendingClass = spendingClass,
            paymentChannel = PaymentChannel.CASH,
            money = Money(
                originalAmountDecimal = (amountMinor / 100).toString(),
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1.00",
                baseAmountMinor = amountMinor,
                rateSource = RateSource.LKR_FIXED,
                rateDate = today
            ),
            occurredDate = today
        )
    }

    private fun createIncome(
        id: String,
        source: IncomeSource,
        amountMinor: Long,
        pnlDirection: PnlDirection? = null
    ): Income {
        return Income(
            id = id,
            source = source,
            entryKind = if (source == IncomeSource.CRYPTO) {
                IncomeEntryKind.REALIZED_PNL
            } else {
                IncomeEntryKind.RECEIPT
            },
            money = Money(
                originalAmountDecimal = (amountMinor / 100).toString(),
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1.00",
                baseAmountMinor = amountMinor,
                rateSource = RateSource.LKR_FIXED,
                rateDate = today
            ),
            occurredDate = today,
            pnlDirection = pnlDirection
        )
    }

    @Test
    fun calculateEmptyDataReturnsZeroDefaults() {
        val breakdown = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = emptyList(),
            currentMonthExpenses = emptyList()
        )

        assertTrue(breakdown.incomeSourceBreakdown.isEmpty())
        assertTrue(breakdown.expenseCategoryBreakdown.isEmpty())
        assertEquals(0, breakdown.committedPercent)
        assertEquals(0, breakdown.discretionaryPercent)
        assertEquals(0.0, breakdown.monthOverMonthOutflowChangePercent, 0.01)
    }

    @Test
    fun calculateCommittedVsDiscretionaryRatios() {
        val expenses = listOf(
            createExpense("e1", "bills", 60_000_00L, SpendingClass.COMMITTED),
            createExpense("e2", "food", 40_000_00L, SpendingClass.DISCRETIONARY)
        )

        val breakdown = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = emptyList(),
            currentMonthExpenses = expenses
        )

        assertEquals(60, breakdown.committedPercent)
        assertEquals(40, breakdown.discretionaryPercent)
    }

    @Test
    fun currentCategoryClassificationOverridesHistoricalExpenseSnapshot() {
        val expense = createExpense(
            "historical-food",
            "food",
            40_000_00L,
            SpendingClass.DISCRETIONARY
        )

        val breakdown = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = emptyList(),
            currentMonthExpenses = listOf(expense),
            categorySpendingClassesById = mapOf("food" to SpendingClass.COMMITTED)
        )

        assertEquals(40_000_00L, breakdown.totalCommittedLkrMinor)
        assertEquals(0L, breakdown.totalDiscretionaryLkrMinor)
        assertEquals(100, breakdown.committedPercent)
    }

    @Test
    fun calculateCryptoYtdNetGain() {
        val cryptoIncomes = listOf(
            createIncome("c1", IncomeSource.CRYPTO, 150_000_00L, PnlDirection.GAIN),
            createIncome("c2", IncomeSource.CRYPTO, 50_000_00L, PnlDirection.LOSS)
        )

        val breakdown = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = emptyList(),
            currentMonthExpenses = emptyList(),
            yearToDateCryptoIncomes = cryptoIncomes
        )

        assertEquals(100_000_00L, breakdown.cryptoYtdResult.netPnlLkrMinor)
        assertEquals(CryptoResultState.GAIN, breakdown.cryptoYtdResult.state)
        assertEquals("REALIZED NET GAIN", breakdown.cryptoYtdResult.pnlLabel)
    }

    @Test
    fun cryptoSummaryDistinguishesNoActivityBreakEvenGainAndLoss() {
        fun result(vararg incomes: Income) = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = emptyList(),
            currentMonthExpenses = emptyList(),
            yearToDateCryptoIncomes = incomes.toList()
        ).cryptoYtdResult

        assertEquals(CryptoResultState.NO_ACTIVITY, result().state)
        assertEquals(
            CryptoResultState.BREAK_EVEN,
            result(
                createIncome("gain", IncomeSource.CRYPTO, 100_00L, PnlDirection.GAIN),
                createIncome("loss", IncomeSource.CRYPTO, 100_00L, PnlDirection.LOSS)
            ).state
        )
        assertEquals(
            CryptoResultState.GAIN,
            result(createIncome("gain-only", IncomeSource.CRYPTO, 1_00L, PnlDirection.GAIN)).state
        )
        assertEquals(
            CryptoResultState.LOSS,
            result(createIncome("loss-only", IncomeSource.CRYPTO, 1_00L, PnlDirection.LOSS)).state
        )
    }

    @Test
    fun sourceBreakdownIncludesRealisedLossMagnitudeAndUsesCurrentCategoryName() {
        val salary = createIncome("salary", IncomeSource.SALARY, 300_00L)
        val cryptoLoss = createIncome(
            "crypto-loss",
            IncomeSource.CRYPTO,
            100_00L,
            PnlDirection.LOSS
        )
        val expense = createExpense(
            id = "expense",
            categoryId = "food",
            amountMinor = 50_00L,
            spendingClass = SpendingClass.DISCRETIONARY
        ).copy(categoryNameSnapshot = "Old food name")

        val breakdown = IncomeSpendingBreakdownCalculator.calculateBreakdown(
            currentMonthIncomes = listOf(salary, cryptoLoss),
            currentMonthExpenses = listOf(expense),
            categoryNamesById = mapOf("food" to "Food & Dining")
        )

        assertEquals(75, breakdown.incomeSourceBreakdown[0].percentage)
        assertEquals(IncomeSource.CRYPTO.name, breakdown.incomeSourceBreakdown[1].sourceName)
        assertEquals(-100_00L, breakdown.incomeSourceBreakdown[1].totalLkrMinor)
        assertEquals(25, breakdown.incomeSourceBreakdown[1].percentage)
        assertEquals("Food & Dining", breakdown.expenseCategoryBreakdown.single().categoryName)
    }
}
