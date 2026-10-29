package com.example.personalfinanceapp.feature.dashboard

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.dashboard.ui.ExpenseActivityFilter
import com.example.personalfinanceapp.feature.dashboard.ui.filterExpenses
import com.example.personalfinanceapp.feature.dashboard.ui.groupExpensesByDate
import com.example.personalfinanceapp.feature.dashboard.ui.summarizeExpenses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActivityPresentationTest {

    private val newerDate = LocalDate.of(2026, 10, 20)
    private val olderDate = LocalDate.of(2026, 10, 19)
    private val expenses = listOf(
        expense("committed-new", "bills", SpendingClass.COMMITTED, 100_00L, newerDate),
        expense("flexible-new", "food", SpendingClass.DISCRETIONARY, 25_50L, newerDate),
        expense("committed-old", "bills", SpendingClass.COMMITTED, 50_25L, olderDate)
    )

    @Test
    fun filtersReturnOnlyMatchingSupportedRecordsAndRecalculateTotals() {
        val committed = filterExpenses(expenses, ExpenseActivityFilter.COMMITTED)
        val food = filterExpenses(expenses, ExpenseActivityFilter.CATEGORY, "food")

        assertEquals(listOf("committed-new", "committed-old"), committed.map { it.id })
        assertEquals(listOf("flexible-new"), food.map { it.id })
        assertTrue(filterExpenses(expenses, ExpenseActivityFilter.CATEGORY, null).isEmpty())

        val summary = summarizeExpenses(committed)
        assertEquals(150_25L, summary.totalLkrMinor)
        assertEquals(150_25L, summary.committedLkrMinor)
        assertEquals(0L, summary.discretionaryLkrMinor)
        assertEquals(100, summary.committedPercentage)
        assertEquals(0, summary.discretionaryPercentage)
    }

    @Test
    fun dateGroupingIsDescendingAndDailyTotalsUseOnlyGroupRecords() {
        val groups = groupExpensesByDate(expenses)

        assertEquals(listOf(newerDate, olderDate), groups.map { it.date })
        assertEquals(125_50L, groups[0].totalLkrMinor)
        assertEquals(50_25L, groups[1].totalLkrMinor)
        assertEquals(listOf("committed-new", "flexible-new"), groups[0].expenses.map { it.id })
    }

    private fun expense(
        id: String,
        categoryId: String,
        spendingClass: SpendingClass,
        amountMinor: Long,
        date: LocalDate
    ) = Expense(
        id = id,
        categoryId = categoryId,
        categoryNameSnapshot = categoryId,
        spendingClass = spendingClass,
        paymentChannel = PaymentChannel.CASH,
        money = Money(
            originalAmountDecimal = (amountMinor.toBigDecimal() / 100.toBigDecimal()).toPlainString(),
            originalCurrency = CurrencyCode.LKR,
            rateLkrPerUnitDecimal = "1",
            baseAmountMinor = amountMinor,
            rateSource = RateSource.LKR_FIXED,
            rateDate = date
        ),
        occurredDate = date
    )
}
