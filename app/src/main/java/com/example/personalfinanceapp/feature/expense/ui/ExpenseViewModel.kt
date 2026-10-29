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
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
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
    val totalLkrMinor: Long = 0L,
    val committedLkrMinor: Long = 0L,
    val discretionaryLkrMinor: Long = 0L,
    val committedPercentage: Int = 0,
    val discretionaryPercentage: Int = 0
)

data class ExpenseSaveUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedExpenseId: DocumentId? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository
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
    private val _saveUiState = MutableStateFlow(ExpenseSaveUiState())
    val saveUiState: StateFlow<ExpenseSaveUiState> = _saveUiState.asStateFlow()

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
        val committedLkr = expenses
            .filter { it.spendingClass == SpendingClass.COMMITTED }
            .sumOf { it.money.baseAmountMinor }
        val discretionaryLkr = expenses
            .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .sumOf { it.money.baseAmountMinor }
        val committedPct = if (totalLkr > 0L) {
            ((committedLkr.toDouble() / totalLkr) * 100).toInt()
        } else 0
        val discretionaryPct = if (totalLkr > 0L) {
            100 - committedPct
        } else 0

        ExpenseUiState(
            expenses = expenses,
            categories = categories,
            dateRange = dateRange,
            isLoading = isLoading,
            errorMessage = errorMessage,
            totalLkrMinor = totalLkr,
            committedLkrMinor = committedLkr,
            discretionaryLkrMinor = discretionaryLkr,
            committedPercentage = committedPct,
            discretionaryPercentage = discretionaryPct
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
        note: String?,
        customRateLkrPerUsd: String? = null,
        existingExpense: Expense? = null
    ) {
        _saveUiState.value = ExpenseSaveUiState()
        val trimmedAmount = amountDecimal.trim()
        if (trimmedAmount.isBlank()) {
            publishSaveError("Amount cannot be blank")
            return
        }

        if (categoryId.isBlank()) {
            publishSaveError("Please select a category")
            return
        }

        val storedExpense = existingExpense ?: id
            ?.takeIf { it.isNotBlank() }
            ?.let { expenseId -> uiState.value.expenses.firstOrNull { it.id == expenseId } }

        if (existingExpense != null && !id.isNullOrBlank() && existingExpense.id != id) {
            publishSaveError("The expense being edited no longer matches the loaded record")
            return
        }

        val categoryNameSnapshot = uiState.value.categories
            .firstOrNull { it.id == categoryId }
            ?.name
            ?: storedExpense
                ?.takeIf { it.categoryId == categoryId }
                ?.categoryNameSnapshot
            ?: run {
                publishSaveError("The selected category is unavailable")
                return
            }

        _isLoading.value = true
        _errorMessage.value = null
        _saveUiState.value = ExpenseSaveUiState(isSaving = true)

        viewModelScope.launch {
            val moneyResult: Result<Money> = when {
                storedExpense != null &&
                    currency == storedExpense.money.originalCurrency &&
                    customRateLkrPerUsd.isNullOrBlank() &&
                    amountsAreEqual(trimmedAmount, storedExpense.money.originalAmountDecimal) -> {
                    Result.success(storedExpense.money)
                }
                storedExpense?.money?.originalCurrency == CurrencyCode.USD &&
                    currency == CurrencyCode.USD &&
                    customRateLkrPerUsd.isNullOrBlank() -> {
                    MoneyConverter.fromUsd(
                        amountInput = trimmedAmount,
                        rateLkrPerUsdInput = storedExpense.money.rateLkrPerUnitDecimal,
                        rateSource = storedExpense.money.rateSource,
                        rateDate = storedExpense.money.rateDate
                    )
                }
                currency == CurrencyCode.LKR -> {
                    MoneyConverter.fromLkr(
                        amountInput = trimmedAmount,
                        transactionDate = occurredDate
                    )
                }
                else -> {
                    createUsdMoney(
                        amount = trimmedAmount,
                        occurredDate = occurredDate,
                        customRateLkrPerUsd = customRateLkrPerUsd
                    )
                }
            }

            if (moneyResult.isFailure) {
                publishSaveError(
                    moneyResult.exceptionOrNull()?.localizedMessage
                        ?: "Invalid amount or currency conversion"
                )
                return@launch
            }

            val money = moneyResult.getOrThrow()
            val expenseId = storedExpense?.id
                ?: id?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

            val expense = Expense(
                id = expenseId,
                categoryId = categoryId,
                categoryNameSnapshot = categoryNameSnapshot,
                spendingClass = spendingClass,
                paymentChannel = paymentChannel,
                money = money,
                occurredDate = occurredDate,
                note = note?.trim()?.ifBlank { null },
                channelLabel = storedExpense?.channelLabel
            )

            val result = expenseRepository.saveExpense(expense)
            _isLoading.value = false
            if (result.isFailure) {
                publishSaveError(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to save expense"
                )
            } else {
                _saveUiState.value = ExpenseSaveUiState(savedExpenseId = expenseId)
            }
        }
    }

    private suspend fun createUsdMoney(
        amount: String,
        occurredDate: LocalDate,
        customRateLkrPerUsd: String?
    ): Result<Money> {
        val rateStringToUse: String
        val rateSourceToUse: RateSource
        val rateDateToUse: LocalDate

        if (!customRateLkrPerUsd.isNullOrBlank()) {
            rateStringToUse = customRateLkrPerUsd.trim()
            rateSourceToUse = RateSource.MANUAL_REFERENCE
            rateDateToUse = occurredDate
        } else {
            val quoteResult = exchangeRateRepository.getLatestUsdToLkr()
            if (quoteResult.isFailure) {
                return Result.failure(
                    quoteResult.exceptionOrNull()
                        ?: IllegalStateException("Exchange rate is currently unavailable")
                )
            }
            val quote = quoteResult.getOrThrow()
            rateStringToUse = quote.rate.toPlainString()
            rateSourceToUse = RateSource.EXCHANGE_RATE_API_REFERENCE
            rateDateToUse = quote.applicableDate
        }

        return MoneyConverter.fromUsd(
            amountInput = amount,
            rateLkrPerUsdInput = rateStringToUse,
            rateSource = rateSourceToUse,
            rateDate = rateDateToUse
        )
    }

    private fun amountsAreEqual(first: String, second: String): Boolean {
        val firstAmount = first.toBigDecimalOrNull() ?: return false
        val secondAmount = second.toBigDecimalOrNull() ?: return false
        return firstAmount.compareTo(secondAmount) == 0
    }

    private fun publishSaveError(message: String) {
        _isLoading.value = false
        _errorMessage.value = message
        _saveUiState.value = ExpenseSaveUiState(errorMessage = message)
    }

    fun consumeSaveResult() {
        _saveUiState.value = ExpenseSaveUiState()
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
