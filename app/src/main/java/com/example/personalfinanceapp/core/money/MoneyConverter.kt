
package com.example.personalfinanceapp.core.money

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.RateSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object MoneyConverter {

    private val ratePattern =
        Regex("""\d+(\.\d{1,6})?""")

    private val oneHundred = BigDecimal("100")

    fun fromLkr(
        amountInput: String,
        transactionDate: LocalDate
    ): Result<Money> {

        val amount = MoneyValidator
            .parsePositiveAmount(amountInput)
            .getOrElse {
                return Result.failure(it)
            }

        return buildMoney(
            amount = amount,
            currency = CurrencyCode.LKR,
            rate = BigDecimal.ONE,
            rateSource = RateSource.LKR_FIXED,
            rateDate = transactionDate
        )
    }

    fun fromUsd(
        amountInput: String,
        rateLkrPerUsdInput: String,
        rateSource: RateSource,
        rateDate: LocalDate
    ): Result<Money> {

        if (
            rateSource != RateSource.MANUAL_TRANSFER &&
            rateSource != RateSource.MANUAL_REFERENCE
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "USD requires a manual exchange rate source"
                )
            )
        }

        val amount = MoneyValidator
            .parsePositiveAmount(amountInput)
            .getOrElse {
                return Result.failure(it)
            }

        val rateText = rateLkrPerUsdInput.trim()

        if (!ratePattern.matches(rateText)) {
            return Result.failure(
                IllegalArgumentException(
                    "Enter a valid exchange rate with up to 6 decimal places"
                )
            )
        }

        val rate = rateText.toBigDecimalOrNull()

        if (rate == null || rate <= BigDecimal.ZERO) {
            return Result.failure(
                IllegalArgumentException(
                    "Exchange rate must be greater than zero"
                )
            )
        }

        return buildMoney(
            amount = amount,
            currency = CurrencyCode.USD,
            rate = rate,
            rateSource = rateSource,
            rateDate = rateDate
        )
    }

    private fun buildMoney(
        amount: BigDecimal,
        currency: CurrencyCode,
        rate: BigDecimal,
        rateSource: RateSource,
        rateDate: LocalDate
    ): Result<Money> {

        return runCatching {

            val baseAmountMinor = amount
                .multiply(rate)
                .multiply(oneHundred)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            Money(
                originalAmountDecimal = amount.toPlainString(),
                originalCurrency = currency,
                rateLkrPerUnitDecimal = rate.toPlainString(),
                baseAmountMinor = baseAmountMinor,
                rateSource = rateSource,
                rateDate = rateDate
            )
        }
    }
}
