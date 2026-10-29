package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class IncomePresentationTest {

    @Test
    fun `LKR minor units format exactly without floating point`() {
        assertEquals("LKR 0.00", formatLkrMinorAmount(0L))
        assertEquals("LKR -123.45", formatLkrMinorAmount(-12_345L))
        assertEquals(
            "LKR 92,233,720,368,547,758.07",
            formatLkrMinorAmount(Long.MAX_VALUE)
        )
        assertEquals(
            "LKR -92,233,720,368,547,758.08",
            formatLkrMinorAmount(Long.MIN_VALUE)
        )
    }

    @Test
    fun `original amount preserves currency and formats stored decimal`() {
        assertEquals(
            "USD 125,000.50",
            formatOriginalAmount(money("125000.50", CurrencyCode.USD))
        )
        assertEquals(
            "LKR 1.00",
            formatOriginalAmount(money("1", CurrencyCode.LKR))
        )
    }

    @Test
    fun `income source labels distinguish every supported source and crypto direction`() {
        assertEquals("Salary", incomeSourceLabel(income(IncomeSource.SALARY)))
        assertEquals("Freelance", incomeSourceLabel(income(IncomeSource.FREELANCE)))
        assertEquals("AdSense", incomeSourceLabel(income(IncomeSource.ADSENSE)))
        assertEquals(
            "Crypto Gain",
            incomeSourceLabel(income(IncomeSource.CRYPTO, PnlDirection.GAIN))
        )
        assertEquals(
            "Crypto Loss",
            incomeSourceLabel(income(IncomeSource.CRYPTO, PnlDirection.LOSS))
        )
    }

    @Test
    fun `whole month and transaction date use readable labels`() {
        assertEquals(
            "October 2026",
            formatIncomeDateRange(
                IncomeDateRange(
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 11, 1)
                )
            )
        )
        assertEquals(
            "Oct 24, 2026",
            formatTransactionDate(income(IncomeSource.SALARY))
        )
    }

    @Test
    fun `month shifting handles year and leap boundaries`() {
        val december = IncomeDateRange(
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2027, 1, 1)
        )
        val februaryLeapYear = IncomeDateRange(
            LocalDate.of(2028, 2, 1),
            LocalDate.of(2028, 3, 1)
        )

        assertEquals(
            IncomeDateRange(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)),
            shiftIncomeMonth(december, 1L)
        )
        assertEquals(
            IncomeDateRange(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 4, 1)),
            shiftIncomeMonth(februaryLeapYear, 1L)
        )
    }

    @Test
    fun `month shifting rejects date overflow`() {
        val finalRepresentableMonthRange = IncomeDateRange(
            LocalDate.of(999_999_999, 11, 1),
            LocalDate.of(999_999_999, 12, 1)
        )

        assertNull(shiftIncomeMonth(finalRepresentableMonthRange, 1L))
    }

    private fun income(
        source: IncomeSource,
        direction: PnlDirection? = null
    ): Income = Income(
        id = "income-id",
        source = source,
        entryKind = if (source == IncomeSource.CRYPTO) {
            IncomeEntryKind.REALIZED_PNL
        } else {
            IncomeEntryKind.RECEIPT
        },
        money = money("1.00", CurrencyCode.LKR),
        occurredDate = LocalDate.of(2026, 10, 24),
        pnlDirection = direction
    )

    private fun money(
        originalAmountDecimal: String,
        currency: CurrencyCode
    ): Money = Money(
        originalAmountDecimal = originalAmountDecimal,
        originalCurrency = currency,
        rateLkrPerUnitDecimal = "1",
        baseAmountMinor = 100L,
        rateSource = RateSource.LKR_FIXED,
        rateDate = LocalDate.of(2026, 10, 24)
    )
}

