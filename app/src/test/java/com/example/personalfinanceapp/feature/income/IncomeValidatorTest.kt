package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IncomeValidatorTest {

    private val occurredDate = LocalDate.of(2026, 10, 20)

    private fun createIncome(
        id: String = "income-001",
        source: IncomeSource = IncomeSource.SALARY,
        entryKind: IncomeEntryKind = IncomeEntryKind.RECEIPT,
        pnlDirection: PnlDirection? = null,
        useUsd: Boolean = false,
        usdRateSource: RateSource = RateSource.MANUAL_REFERENCE
    ): Income {
        val money = if (useUsd) {
            MoneyConverter.fromUsd(
                amountInput = "125.50",
                rateLkrPerUsdInput = "305.25",
                rateSource = usdRateSource,
                rateDate = occurredDate.minusDays(1)
            ).getOrThrow()
        } else {
            MoneyConverter.fromLkr(
                amountInput = "1500.50",
                transactionDate = occurredDate
            ).getOrThrow()
        }

        return Income(
            id = id,
            source = source,
            entryKind = entryKind,
            money = money,
            occurredDate = occurredDate,
            pnlDirection = pnlDirection
        )
    }

    private fun assertFailureMessage(
        income: Income,
        expectedMessage: String
    ) {
        val result = IncomeValidator.validate(income)

        assertTrue(result.isFailure)
        assertEquals(expectedMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun acceptsSalaryReceipt() {
        assertTrue(IncomeValidator.validate(createIncome()).isSuccess)
    }

    @Test
    fun acceptsFreelanceReceipt() {
        val income = createIncome(source = IncomeSource.FREELANCE)

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsAdsenseReceipt() {
        val income = createIncome(source = IncomeSource.ADSENSE)

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsCryptoRealizedGain() {
        val income = createIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.GAIN
        )

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsCryptoRealizedLossWithPositiveMagnitude() {
        val income = createIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.LOSS
        )

        assertTrue(IncomeValidator.validate(income).isSuccess)
        assertTrue(income.money.baseAmountMinor > 0L)
    }

    @Test
    fun acceptsValidLkrSnapshot() {
        val income = createIncome()

        assertEquals(CurrencyCode.LKR, income.money.originalCurrency)
        assertEquals(income.occurredDate, income.money.rateDate)
        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun rejectsLkrRateDateBeforeTransactionDate() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateDate = validIncome.occurredDate.minusDays(1)
                )
            )
        }

        assertFailureMessage(
            income,
            "LKR rate date must match the transaction date"
        )
    }

    @Test
    fun acceptsHistoricalLkrWithMatchingRateDate() {
        val historicalDate = LocalDate.of(2020, 1, 15)
        val historicalMoney = MoneyConverter.fromLkr(
            amountInput = "1500.50",
            transactionDate = historicalDate
        ).getOrThrow()
        val income = createIncome().copy(
            money = historicalMoney,
            occurredDate = historicalDate
        )

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsNumericallyEquivalentLkrRate() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(rateLkrPerUnitDecimal = "1.0")
            )
        }

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsValidUsdSnapshot() {
        val income = createIncome(useUsd = true)

        assertEquals(CurrencyCode.USD, income.money.originalCurrency)
        assertNotEquals(income.occurredDate, income.money.rateDate)
        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun acceptsBothManualUsdRateSources() {
        val manualTransferIncome = createIncome(
            useUsd = true,
            usdRateSource = RateSource.MANUAL_TRANSFER
        )
        val manualReferenceIncome = createIncome(
            useUsd = true,
            usdRateSource = RateSource.MANUAL_REFERENCE
        )

        assertTrue(IncomeValidator.validate(manualTransferIncome).isSuccess)
        assertTrue(IncomeValidator.validate(manualReferenceIncome).isSuccess)
    }

    @Test
    fun acceptsOptionalMetadataFields() {
        val income = createIncome(
            source = IncomeSource.FREELANCE,
            useUsd = true
        ).copy(
            projectReference = "website-redesign",
            receivableId = "receivable-001",
            assetSymbol = "USD",
            note = "Final milestone"
        )

        assertTrue(IncomeValidator.validate(income).isSuccess)
    }

    @Test
    fun rejectsBlankIncomeId() {
        assertFailureMessage(
            income = createIncome(id = "   "),
            expectedMessage = "Income ID cannot be empty"
        )
    }

    @Test
    fun rejectsZeroMinorUnitValue() {
        val income = createIncome().copy(
            money = createIncome().money.copy(baseAmountMinor = 0L)
        )

        assertFailureMessage(
            income,
            "Income amount must be greater than zero"
        )
    }

    @Test
    fun rejectsNegativeMinorUnitValue() {
        val income = createIncome().copy(
            money = createIncome().money.copy(baseAmountMinor = -1L)
        )

        assertFailureMessage(
            income,
            "Income amount must be greater than zero"
        )
    }

    @Test
    fun rejectsInvalidOriginalAmount() {
        val income = createIncome().copy(
            money = createIncome().money.copy(
                originalAmountDecimal = "1500.999"
            )
        )

        assertFailureMessage(
            income,
            "Enter a valid amount with up to 2 decimal places"
        )
    }

    @Test
    fun rejectsZeroOriginalAmount() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(originalAmountDecimal = "0")
            )
        }

        assertFailureMessage(income, "Amount must be greater than zero")
    }

    @Test
    fun rejectsInvalidExchangeRate() {
        val income = createIncome(useUsd = true).let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateLkrPerUnitDecimal = "not-a-rate"
                )
            )
        }

        assertFailureMessage(
            income,
            "Enter a valid exchange rate with up to 6 decimal places"
        )
    }

    @Test
    fun rejectsZeroExchangeRate() {
        val income = createIncome(useUsd = true).let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(rateLkrPerUnitDecimal = "0")
            )
        }

        assertFailureMessage(
            income,
            "Exchange rate must be greater than zero"
        )
    }

    @Test
    fun rejectsMalformedLkrExchangeRate() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateLkrPerUnitDecimal = "not-a-rate"
                )
            )
        }

        assertFailureMessage(income, "Enter a valid exchange rate")
    }

    @Test
    fun rejectsIncorrectLkrRate() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateLkrPerUnitDecimal = "2"
                )
            )
        }

        assertFailureMessage(income, "LKR exchange rate must be 1")
    }

    @Test
    fun rejectsManualRateSourceForLkr() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateSource = RateSource.MANUAL_REFERENCE
                )
            )
        }

        assertFailureMessage(
            income,
            "LKR requires the fixed exchange rate source"
        )
    }

    @Test
    fun rejectsFixedRateSourceForUsd() {
        val income = createIncome(useUsd = true).let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    rateSource = RateSource.LKR_FIXED
                )
            )
        }

        assertFailureMessage(
            income,
            "USD requires a manual exchange rate source"
        )
    }

    @Test
    fun rejectsStoredAmountInconsistentWithCalculation() {
        val income = createIncome(useUsd = true).let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    baseAmountMinor = validIncome.money.baseAmountMinor + 1L
                )
            )
        }

        assertFailureMessage(
            income,
            "Stored LKR minor-unit amount does not match the calculated amount"
        )
    }

    @Test
    fun rejectsCryptoWithoutPnlDirection() {
        val income = createIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL
        )

        assertFailureMessage(
            income,
            "Select whether the crypto transaction is a gain or loss"
        )
    }

    @Test
    fun rejectsCryptoReceiptEntryKind() {
        val income = createIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.RECEIPT,
            pnlDirection = PnlDirection.GAIN
        )

        assertFailureMessage(
            income,
            "Crypto income must be recorded as realized profit or loss"
        )
    }

    @Test
    fun rejectsNonCryptoRealizedPnlEntryKind() {
        val income = createIncome(
            source = IncomeSource.SALARY,
            entryKind = IncomeEntryKind.REALIZED_PNL
        )

        assertFailureMessage(
            income,
            "Regular income must be recorded as a receipt"
        )
    }

    @Test
    fun rejectsNonCryptoPnlDirection() {
        val income = createIncome(pnlDirection = PnlDirection.LOSS)

        assertFailureMessage(
            income,
            "Profit or loss direction is only used for crypto transactions"
        )
    }

    @Test
    fun rejectsConversionOverflow() {
        val income = createIncome().let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    originalAmountDecimal = "999999999999999999999999",
                    baseAmountMinor = 1L
                )
            )
        }

        assertTrue(IncomeValidator.validate(income).isFailure)
    }

    @Test
    fun rejectsConversionThatRoundsToZeroMinorUnits() {
        val income = createIncome(useUsd = true).let { validIncome ->
            validIncome.copy(
                money = validIncome.money.copy(
                    originalAmountDecimal = "0.01",
                    rateLkrPerUnitDecimal = "0.01",
                    baseAmountMinor = 1L
                )
            )
        }

        assertFailureMessage(
            income,
            "Converted amount is too small to represent in LKR cents"
        )
    }
}
