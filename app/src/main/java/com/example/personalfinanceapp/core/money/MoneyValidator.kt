
package com.example.personalfinanceapp.core.money

import java.math.BigDecimal

object MoneyValidator {

    private val amountPattern =
        Regex("""\d+(\.\d{1,2})?""")

    fun parsePositiveAmount(
        input: String
    ): Result<BigDecimal> {

        val cleanedInput = input.trim()

        if (cleanedInput.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Amount cannot be empty"
                )
            )
        }

        if (!amountPattern.matches(cleanedInput)) {
            return Result.failure(
                IllegalArgumentException(
                    "Enter a valid amount with up to 2 decimal places"
                )
            )
        }

        val amount = cleanedInput.toBigDecimalOrNull()

        if (amount == null || amount <= BigDecimal.ZERO) {
            return Result.failure(
                IllegalArgumentException(
                    "Amount must be greater than zero"
                )
            )
        }

        return Result.success(amount)
    }
}
