package com.example.personalfinanceapp.feature.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class DateRange(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate
)

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val dateRange: DateRange = DateRange(
        startDate = LocalDate.now().withDayOfMonth(1),
        endDateExclusive = LocalDate.now().withDayOfMonth(1).plusMonths(1)
    ),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalLkrMinor: Long = 0L
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val _dateRange = MutableStateFlow(
        DateRange(
            startDate = now.withDayOfMonth(1),
            endDateExclusive = now.withDayOfMonth(1).plusMonths(1)
        )
    )

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ExpenseUiState> = combine(
        _dateRange.flatMapLatest { range ->
            expenseRepository.observeExpenses(range.startDate, range.endDateExclusive)
        },
        categoryRepository.observeCategories(),
        _dateRange,
        _isLoading,
        _errorMessage
    ) { expenses, categories, dateRange, isLoading, errorMessage ->
        val totalLkr = expenses.sumOf { it.money.baseAmountMinor }
        ExpenseUiState(
            expenses = expenses,
            categories = categories,
            dateRange = dateRange,
            isLoading = isLoading,
            errorMessage = errorMessage,
            totalLkrMinor = totalLkr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExpenseUiState()
    )

    fun setDateRange(startDate: LocalDate, endDateExclusive: LocalDate) {
        require(startDate.isBefore(endDateExclusive)) { "Start date must be before end date" }
        _dateRange.value = DateRange(startDate, endDateExclusive)
    }

    fun saveExpense(
        id: String?,
        categoryId: String,
        amountDecimal: String,
        currency: CurrencyCode,
        paymentChannel: PaymentChannel,
        spendingClass: SpendingClass,
        occurredDate: LocalDate,
        note: String?
    ) {
        val amount = amountDecimal.trim().toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _errorMessage.value = "Amount must be a positive number"
            return
        }

        if (categoryId.isBlank()) {
            _errorMessage.value = "Please select a category"
            return
        }

        val categories = uiState.value.categories
        val category = categories.find { it.id == categoryId }
        val categoryNameSnapshot = category?.name ?: "General"

        val rateLkr = if (currency == CurrencyCode.USD) "300.00" else "1.00"
        val rateBigDecimal = rateLkr.toBigDecimal()
        val baseLkrDecimal = amount.multiply(rateBigDecimal)
        val baseAmountMinor = baseLkrDecimal.multiply(BigDecimal("100")).toLong()

        val expenseId = if (!id.isNullOrBlank()) id else UUID.randomUUID().toString()

        val expense = Expense(
            id = expenseId,
            categoryId = categoryId,
            categoryNameSnapshot = categoryNameSnapshot,
            spendingClass = spendingClass,
            paymentChannel = paymentChannel,
            money = Money(
                originalAmountDecimal = amount.toPlainString(),
                originalCurrency = currency,
                rateLkrPerUnitDecimal = rateLkr,
                baseAmountMinor = baseAmountMinor,
                rateSource = if (currency == CurrencyCode.LKR) RateSource.LKR_FIXED else RateSource.MANUAL_REFERENCE,
                rateDate = occurredDate
            ),
            occurredDate = occurredDate,
            note = note?.trim()?.ifBlank { null }
        )

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = expenseRepository.saveExpense(expense)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save expense"
            }
        }
    }

    fun deleteExpense(id: DocumentId) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = expenseRepository.deleteExpense(id)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete expense"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
