
package com.example.personalfinanceapp.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyValidatorTest {

    @Test
    fun acceptsWholeNumber() {
        val result = MoneyValidator.parsePositiveAmount("1500")

        assertTrue(result.isSuccess)
        assertEquals(
            BigDecimal("1500"),
            result.getOrThrow()
        )
    }

    @Test
    fun acceptsTwoDecimalPlaces() {
        val result = MoneyValidator.parsePositiveAmount("1500.50")

        assertTrue(result.isSuccess)
        assertEquals(
            BigDecimal("1500.50"),
            result.getOrThrow()
        )
    }

    @Test
    fun acceptsOneDecimalPlace() {
        val result = MoneyValidator.parsePositiveAmount("25.5")

        assertTrue(result.isSuccess)
        assertEquals(
            BigDecimal("25.5"),
            result.getOrThrow()
        )
    }

    @Test
    fun trimsExtraSpaces() {
        val result = MoneyValidator.parsePositiveAmount(" 1500.50 ")

        assertTrue(result.isSuccess)
        assertEquals(
            BigDecimal("1500.50"),
            result.getOrThrow()
        )
    }

    @Test
    fun rejectsEmptyAmount() {
        val result = MoneyValidator.parsePositiveAmount("")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsWhitespaceOnly() {
        val result = MoneyValidator.parsePositiveAmount("   ")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsZero() {
        val result = MoneyValidator.parsePositiveAmount("0")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsNegativeAmount() {
        val result = MoneyValidator.parsePositiveAmount("-100")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsAlphabeticInput() {
        val result = MoneyValidator.parsePositiveAmount("abc")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsMoreThanTwoDecimalPlaces() {
        val result = MoneyValidator.parsePositiveAmount("1500.999")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsCommaSeparatedInput() {
        val result = MoneyValidator.parsePositiveAmount("1,500.50")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsScientificNotation() {
        val result = MoneyValidator.parsePositiveAmount("1e3")

        assertTrue(result.isFailure)
    }
}
