package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.expense.ui.expenseEditorInitialValues
import com.example.personalfinanceapp.feature.expense.ui.presentExpenseEditorError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseEditorPresentationTest {

    private val categories = listOf(
        Category("food", "Food", SpendingClass.DISCRETIONARY),
        Category("bills", "Bills", SpendingClass.COMMITTED)
    )

    @Test
    fun newExpenseStartsWithBlankAmountAndNote() {
        val today = LocalDate.of(2026, 10, 29)

        val values = expenseEditorInitialValues(null, categories, today)

        assertTrue(values.amountText.isEmpty())
        assertTrue(values.noteText.isEmpty())
        assertEquals("food", values.categoryId)
        assertEquals(today, values.occurredDate)
    }

    @Test
    fun existingExpenseHydratesStoredValues() {
        val date = LocalDate.of(2025, 2, 4)
        val expense = Expense(
            id = "expense-1",
            categoryId = "bills",
            categoryNameSnapshot = "Bills",
            spendingClass = SpendingClass.COMMITTED,
            paymentChannel = PaymentChannel.BANK,
            money = Money(
                originalAmountDecimal = "19.95",
                originalCurrency = CurrencyCode.USD,
                rateLkrPerUnitDecimal = "300.25",
                baseAmountMinor = 599_899L,
                rateSource = RateSource.MANUAL_REFERENCE,
                rateDate = date
            ),
            occurredDate = date,
            note = "Hosting"
        )

        val values = expenseEditorInitialValues(expense, categories)

        assertEquals("19.95", values.amountText)
        assertEquals(CurrencyCode.USD, values.currency)
        assertEquals("bills", values.categoryId)
        assertEquals(PaymentChannel.BANK, values.paymentChannel)
        assertEquals(SpendingClass.COMMITTED, values.spendingClass)
        assertEquals("Hosting", values.noteText)
        assertEquals(date, values.occurredDate)
    }

    @Test
    fun displayingSaveErrorPreservesEveryEnteredValue() {
        val entered = expenseEditorInitialValues(null, categories, LocalDate.of(2026, 10, 29)).copy(
            amountText = "42.75",
            currency = CurrencyCode.USD,
            categoryId = "bills",
            paymentChannel = PaymentChannel.CARD,
            spendingClass = SpendingClass.COMMITTED,
            noteText = "Keep this text"
        )

        val presentation = presentExpenseEditorError(entered, "Repository unavailable")

        assertEquals(entered, presentation.values)
        assertEquals("Repository unavailable", presentation.errorMessage)
    }
}
