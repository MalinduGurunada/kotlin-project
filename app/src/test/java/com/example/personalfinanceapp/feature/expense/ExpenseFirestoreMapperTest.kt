package com.example.personalfinanceapp.feature.expense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseFirestoreMapperTest {

    @Test
    fun reconstructsMissingBaseAmountFromLegitimateLegacyMoneyFields() {
        val fields = validFields().toMutableMap().apply {
            remove("baseAmountMinor")
            this["originalAmountDecimal"] = 1250.50
            this["rateLkrPerUnitDecimal"] = 1
        }

        val expense = ExpenseFirestoreMapper.fromFirestoreMap("legacy", fields)

        assertEquals(125_050L, expense.money.baseAmountMinor)
        assertEquals("1250.5", expense.money.originalAmountDecimal)
        assertEquals("1", expense.money.rateLkrPerUnitDecimal)
    }

    @Test
    fun rejectsMissingNegativeAndInconsistentAmounts() {
        val missing = validFields().toMutableMap().apply {
            remove("baseAmountMinor")
            remove("originalAmountDecimal")
        }
        val negative = validFields().toMutableMap().apply {
            this["baseAmountMinor"] = -1L
            this["originalAmountDecimal"] = "-0.01"
        }
        val inconsistent = validFields().toMutableMap().apply {
            this["baseAmountMinor"] = 99L
        }

        listOf(missing, negative, inconsistent).forEach { fields ->
            assertTrue(
                runCatching {
                    ExpenseFirestoreMapper.fromFirestoreMap("invalid", fields)
                }.exceptionOrNull() is ExpenseFirestoreMappingException
            )
        }
    }

    @Test
    fun rejectsMalformedNumericLegacyValues() {
        val malformed = validFields().toMutableMap().apply {
            remove("baseAmountMinor")
            this["originalAmountDecimal"] = "not-money"
        }

        assertTrue(
            runCatching {
                ExpenseFirestoreMapper.fromFirestoreMap("malformed", malformed)
            }.exceptionOrNull() is ExpenseFirestoreMappingException
        )
    }

    @Test
    fun ignoresInvalidAmountsWhenParseableDateIsOutsideRequestedPeriod() {
        val fields = validFields().toMutableMap().apply {
            this["occurredDate"] = "2026-10-31"
            this["originalAmountDecimal"] = "not-money"
        }

        val expense = ExpenseFirestoreMapper.fromFirestoreMapInRange(
            documentId = "outside",
            fields = fields,
            startDate = LocalDate.of(2026, 11, 1),
            endDateExclusive = LocalDate.of(2026, 12, 1)
        )

        assertEquals(null, expense)
    }

    @Test
    fun rejectsInvalidAmountsWhenDateIsInsideRequestedPeriod() {
        val fields = validFields().toMutableMap().apply {
            this["originalAmountDecimal"] = "not-money"
        }

        val failure = runCatching {
            ExpenseFirestoreMapper.fromFirestoreMapInRange(
                documentId = "inside",
                fields = fields,
                startDate = LocalDate.of(2026, 11, 1),
                endDateExclusive = LocalDate.of(2026, 12, 1)
            )
        }.exceptionOrNull()

        assertTrue(failure is ExpenseFirestoreMappingException)
    }

    private fun validFields(): Map<String, Any?> = mapOf(
        "categoryId" to "food",
        "categoryNameSnapshot" to "Food",
        "spendingClass" to "DISCRETIONARY",
        "paymentChannel" to "CASH",
        "originalAmountDecimal" to "1250.50",
        "originalCurrency" to "LKR",
        "rateLkrPerUnitDecimal" to "1",
        "baseAmountMinor" to 125_050L,
        "rateSource" to "LKR_FIXED",
        "rateDate" to "2026-11-04",
        "occurredDate" to "2026-11-04"
    )
}
