package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EffectiveCategoryClassificationTest {

    @Test
    fun currentCategoryClassificationOverridesStoredExpenseSnapshot() {
        val expense = expense(SpendingClass.DISCRETIONARY)

        assertEquals(
            SpendingClass.COMMITTED,
            expense.effectiveSpendingClass(mapOf("food" to SpendingClass.COMMITTED))
        )
        assertEquals(SpendingClass.DISCRETIONARY, expense.spendingClass)
    }

    @Test
    fun missingCategoryFallsBackToStoredExpenseSnapshot() {
        val expense = expense(SpendingClass.DISCRETIONARY)

        assertEquals(SpendingClass.DISCRETIONARY, expense.effectiveSpendingClass(emptyMap()))
    }

    @Test
    fun archivedCategoryStillProvidesItsCurrentClassification() {
        val categories = listOf(
            Category(
                id = "food",
                name = "Food",
                defaultSpendingClass = SpendingClass.COMMITTED,
                archived = true
            )
        )

        assertEquals(
            SpendingClass.COMMITTED,
            expense(SpendingClass.DISCRETIONARY)
                .effectiveSpendingClass(categories.spendingClassesById())
        )
    }

    private fun expense(spendingClass: SpendingClass) = Expense(
        id = "expense-1",
        categoryId = "food",
        categoryNameSnapshot = "Food",
        spendingClass = spendingClass,
        paymentChannel = PaymentChannel.CARD,
        money = Money(
            originalAmountDecimal = "100.00",
            originalCurrency = CurrencyCode.LKR,
            rateLkrPerUnitDecimal = "1.00",
            baseAmountMinor = 10_000L,
            rateSource = RateSource.LKR_FIXED,
            rateDate = LocalDate.of(2026, 11, 5)
        ),
        occurredDate = LocalDate.of(2026, 11, 5)
    )
}
