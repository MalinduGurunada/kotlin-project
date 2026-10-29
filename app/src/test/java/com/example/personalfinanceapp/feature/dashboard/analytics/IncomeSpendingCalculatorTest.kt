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
            entryKind = IncomeEntryKind.RECEIPT,
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
        assertEquals("REALIZED NET GAIN", breakdown.cryptoYtdResult.pnlLabel)
    }
}
