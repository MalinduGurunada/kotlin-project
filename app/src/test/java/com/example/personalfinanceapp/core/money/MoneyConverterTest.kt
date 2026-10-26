
package com.example.personalfinanceapp.core.money

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.RateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoneyConverterTest {

    private val transactionDate =
        LocalDate.of(2026, 10, 16)

    @Test
    fun convertsWholeLkrAmountToMinorUnits() {
        val result = MoneyConverter.fromLkr(
            amountInput = "1500",
            transactionDate = transactionDate
        )

        assertTrue(result.isSuccess)

        val money = result.getOrThrow()

        assertEquals(150000L, money.baseAmountMinor)
        assertEquals(CurrencyCode.LKR, money.originalCurrency)
        assertEquals("1", money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.LKR_FIXED, money.rateSource)
        assertEquals(transactionDate, money.rateDate)
    }

    @Test
    fun convertsDecimalLkrAmountToMinorUnits() {
        val result = MoneyConverter.fromLkr(
            amountInput = "1500.50",
            transactionDate = transactionDate
        )

        val money = result.getOrThrow()

        assertEquals(150050L, money.baseAmountMinor)
        assertEquals("1500.50", money.originalAmountDecimal)
    }

    @Test
    fun trimsLkrAmountInput() {
        val result = MoneyConverter.fromLkr(
            amountInput = " 250.50 ",
            transactionDate = transactionDate
        )

        val money = result.getOrThrow()

        assertEquals("250.50", money.originalAmountDecimal)
        assertEquals(25050L, money.baseAmountMinor)
    }

    @Test
    fun rejectsInvalidLkrAmounts() {
        val invalidAmounts = listOf(
            "",
            "abc",
            "0",
            "-100",
            "12.345"
        )

        invalidAmounts.forEach { amount ->
            val result = MoneyConverter.fromLkr(
                amountInput = amount,
                transactionDate = transactionDate
            )

            assertTrue(
                "Expected rejection for: $amount",
                result.isFailure
            )
        }
    }

    @Test
    fun convertsUsdToLkrMinorUnits() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100.00",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isSuccess)

        val money = result.getOrThrow()

        assertEquals(3000000L, money.baseAmountMinor)
        assertEquals("100.00", money.originalAmountDecimal)
        assertEquals(CurrencyCode.USD, money.originalCurrency)
        assertEquals("300", money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_TRANSFER, money.rateSource)
    }

    @Test
    fun roundsUsdConversionUsingHalfUp() {
        val result = MoneyConverter.fromUsd(
            amountInput = "1.00",
            rateLkrPerUsdInput = "100.005",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = transactionDate
        )

        val money = result.getOrThrow()

        // LKR 100.005 rounds to LKR 100.01.
        assertEquals(10001L, money.baseAmountMinor)
    }

    @Test
    fun roundsBelowMinorUnitMidpointDownAndMidpointUp() {
        val belowMidpoint = MoneyConverter.fromUsd(
            amountInput = "1.00",
            rateLkrPerUsdInput = "100.004999",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = transactionDate
        ).getOrThrow()
        val atMidpoint = MoneyConverter.fromUsd(
            amountInput = "1.00",
            rateLkrPerUsdInput = "100.005",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = transactionDate
        ).getOrThrow()

        // Exact BigDecimal minor values are 10,000.4999 and 10,000.5 respectively.
        assertEquals(10000L, belowMidpoint.baseAmountMinor)
        assertEquals(10001L, atMidpoint.baseAmountMinor)
    }

    @Test
    fun roundsHalfCentUpCorrectly() {
        val result = MoneyConverter.fromUsd(
            amountInput = "125.50",
            rateLkrPerUsdInput = "305.25",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        // LKR 38,308.875 -> 3,830,887.5 cents
        // HALF_UP rounds to 3,830,888 cents.
        assertEquals(
            3830888L,
            result.getOrThrow().baseAmountMinor
        )
    }

    @Test
    fun preservesHistoricalExchangeRate() {
        val oldDate = LocalDate.of(2026, 10, 1)
        val newDate = LocalDate.of(2026, 10, 16)

        val oldMoney = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = oldDate
        ).getOrThrow()

        val newMoney = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "310",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = newDate
        ).getOrThrow()

        assertEquals(3000000L, oldMoney.baseAmountMinor)
        assertEquals("300", oldMoney.rateLkrPerUnitDecimal)
        assertEquals(oldDate, oldMoney.rateDate)

        assertEquals(3100000L, newMoney.baseAmountMinor)
        assertEquals("310", newMoney.rateLkrPerUnitDecimal)
        assertEquals(newDate, newMoney.rateDate)
    }

    @Test
    fun acceptsManualReferenceRateSource() {
        val result = MoneyConverter.fromUsd(
            amountInput = "10",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = transactionDate
        )

        assertTrue(result.isSuccess)
        assertEquals(
            RateSource.MANUAL_REFERENCE,
            result.getOrThrow().rateSource
        )
    }

    @Test
    fun convertsAutomaticUsdRateAndPreservesItsSnapshotMetadata() {
        val applicableDate = LocalDate.of(2026, 10, 19)

        val result = MoneyConverter.fromUsd(
            amountInput = "125.50",
            rateLkrPerUsdInput = "305.25",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = applicableDate
        )

        assertTrue(result.isSuccess)

        val money = result.getOrThrow()

        assertEquals(3830888L, money.baseAmountMinor)
        assertEquals("125.50", money.originalAmountDecimal)
        assertEquals(CurrencyCode.USD, money.originalCurrency)
        assertEquals("305.25", money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, money.rateSource)
        assertEquals(applicableDate, money.rateDate)
    }

    @Test
    fun rejectsInvalidAutomaticExchangeRates() {
        val invalidRates = listOf(
            "",
            "0",
            "-300",
            "300.1234567"
        )

        invalidRates.forEach { rate ->
            val result = MoneyConverter.fromUsd(
                amountInput = "100",
                rateLkrPerUsdInput = rate,
                rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
                rateDate = transactionDate
            )

            assertTrue("Expected rejection for: $rate", result.isFailure)
        }
    }

    @Test
    fun rejectsFixedRateSourceForUsd() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.LKR_FIXED,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsEmptyExchangeRate() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsZeroExchangeRate() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "0",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsNegativeExchangeRate() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "-300",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsExchangeRateWithTooManyDecimals() {
        val result = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "300.1234567",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsInvalidUsdAmount() {
        val result = MoneyConverter.fromUsd(
            amountInput = "abc",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsOverflowInsteadOfStoringWrongValue() {
        val result = MoneyConverter.fromLkr(
            amountInput = "999999999999999999999999",
            transactionDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsAutomaticUsdOverflowInsteadOfStoringWrongValue() {
        val result = MoneyConverter.fromUsd(
            amountInput = "999999999999999999999999",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsUsdConversionThatRoundsToZeroMinorUnits() {
        val result = MoneyConverter.fromUsd(
            amountInput = "0.01",
            rateLkrPerUsdInput = "0.01",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Converted amount is too small to represent in LKR cents",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun rejectsAutomaticUsdConversionThatRoundsToZeroMinorUnits() {
        val result = MoneyConverter.fromUsd(
            amountInput = "0.01",
            rateLkrPerUsdInput = "0.01",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = transactionDate
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Converted amount is too small to represent in LKR cents",
            result.exceptionOrNull()?.message
        )
    }
}
