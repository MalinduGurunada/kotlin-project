package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import java.math.BigDecimal

object IncomeValidator {

    fun validate(income: Income): Result<Unit> {
        return runCatching {
            require(income.id.isNotBlank()) {
                "Income ID cannot be empty"
            }

            val money = income.money

            require(money.baseAmountMinor > 0L) {
                "Income amount must be greater than zero"
            }

            val recalculatedMoney = when (money.originalCurrency) {
                CurrencyCode.LKR -> {
                    require(money.rateSource == RateSource.LKR_FIXED) {
                        "LKR requires the fixed exchange rate source"
                    }

                    require(money.rateDate == income.occurredDate) {
                        "LKR rate date must match the transaction date"
                    }

                    val rate = money.rateLkrPerUnitDecimal
                        .trim()
                        .toBigDecimalOrNull()

                    require(rate != null) {
                        "Enter a valid exchange rate"
                    }

                    require(rate > BigDecimal.ZERO) {
                        "Exchange rate must be greater than zero"
                    }

                    require(rate.compareTo(BigDecimal.ONE) == 0) {
                        "LKR exchange rate must be 1"
                    }

                    MoneyConverter.fromLkr(
                        amountInput = money.originalAmountDecimal,
                        transactionDate = money.rateDate
                    ).getOrElse { throw it }
                }

                CurrencyCode.USD -> MoneyConverter.fromUsd(
                    amountInput = money.originalAmountDecimal,
                    rateLkrPerUsdInput = money.rateLkrPerUnitDecimal,
                    rateSource = money.rateSource,
                    rateDate = money.rateDate
                ).getOrElse { throw it }
            }

            require(
                money.baseAmountMinor == recalculatedMoney.baseAmountMinor
            ) {
                "Stored LKR minor-unit amount does not match the calculated amount"
            }

            if (income.source == IncomeSource.CRYPTO) {
                require(
                    income.entryKind == IncomeEntryKind.REALIZED_PNL
                ) {
                    "Crypto income must be recorded as realized profit or loss"
                }

                require(income.pnlDirection != null) {
                    "Select whether the crypto transaction is a gain or loss"
                }
            } else {
                require(
                    income.entryKind == IncomeEntryKind.RECEIPT
                ) {
                    "Regular income must be recorded as a receipt"
                }

                require(income.pnlDirection == null) {
                    "Profit or loss direction is only used for crypto transactions"
                }
            }

            if (income.receivableId != null) {
                require(income.receivableId.isNotBlank()) { "Receivable link cannot be blank" }
                require(
                    income.source == IncomeSource.FREELANCE &&
                        income.entryKind == IncomeEntryKind.RECEIPT
                ) { "Only freelance receipts can be linked to a receivable" }
            }
        }
    }
}
