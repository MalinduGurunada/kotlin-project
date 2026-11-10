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
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.category.spendingClassesById
import com.example.personalfinanceapp.feature.category.withEffectiveSpendingClass
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigInteger
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
    val totalsAvailable: Boolean = true,
    val totalLkrMinor: Long = 0L,
    val committedLkrMinor: Long = 0L,
    val discretionaryLkrMinor: Long = 0L,
    val committedPercentage: Int = 0,
    val discretionaryPercentage: Int = 0
)

internal data class ExpenseTotals(
    val totalMinor: Long,
    val committedMinor: Long,
    val discretionaryMinor: Long,
    val committedPercentage: Int,
    val discretionaryPercentage: Int
)

private sealed interface ExpenseObservation {
    data object Pending : ExpenseObservation
    data class Available(val expenses: List<Expense>) : ExpenseObservation
    data class Failed(val message: String) : ExpenseObservation
}

internal fun calculateExpenseTotals(expenses: List<Expense>): ExpenseTotals {
    expenses.forEach { expense ->
        require(expense.money.baseAmountMinor > 0L) { "Expense amount must be positive" }
    }
    val totalMinor = expenses.fold(0L) { total, expense ->
        Math.addExact(total, expense.money.baseAmountMinor)
    }
    val committedMinor = expenses
        .filter { it.spendingClass == SpendingClass.COMMITTED }
        .fold(0L) { total, expense -> Math.addExact(total, expense.money.baseAmountMinor) }
    val discretionaryMinor = expenses
        .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
        .fold(0L) { total, expense -> Math.addExact(total, expense.money.baseAmountMinor) }
    Math.addExact(committedMinor, discretionaryMinor)
    val committedPercentage = if (totalMinor > 0L) {
        BigInteger.valueOf(committedMinor)
            .multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(totalMinor))
            .toInt()
    } else 0
    return ExpenseTotals(
        totalMinor = totalMinor,
        committedMinor = committedMinor,
        discretionaryMinor = discretionaryMinor,
        committedPercentage = committedPercentage,
        discretionaryPercentage = if (totalMinor > 0L) 100 - committedPercentage else 0
    )
}

data class ExpenseSaveUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedExpenseId: DocumentId? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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
                .map<List<Expense>, ExpenseObservation>(ExpenseObservation::Available)
                .onStart { emit(ExpenseObservation.Pending) }
                .catch { exception ->
                    emit(
                        ExpenseObservation.Failed(
                            exception.localizedMessage
                                ?: "Expense data is currently unavailable"
                        )
                    )
                }
        },
        categoryRepository.observeCategories(),
        _dateRange,
        _isLoading,
        _errorMessage
    ) { observation, categories, dateRange, isLoading, errorMessage ->
        if (observation !is ExpenseObservation.Available) {
            return@combine ExpenseUiState(
                expenses = emptyList(),
                categories = categories,
                dateRange = dateRange,
                isLoading = isLoading,
                errorMessage = (observation as? ExpenseObservation.Failed)?.message
                    ?: errorMessage,
                totalsAvailable = false
            )
        }

        val categorySpendingClasses = categories.spendingClassesById()
        val effectiveExpenses = observation.expenses.map { expense ->
            expense.withEffectiveSpendingClass(categorySpendingClasses)
        }
        val totalsResult = runCatching { calculateExpenseTotals(effectiveExpenses) }
        val totals = totalsResult.getOrNull()
        val totalsError = totalsResult.exceptionOrNull()?.let { exception ->
            if (exception is ArithmeticException) {
                "Expense totals are too large to calculate safely"
            } else {
                "Expense data contains an invalid amount"
            }
        }

        ExpenseUiState(
            expenses = effectiveExpenses,
            categories = categories,
            dateRange = dateRange,
            isLoading = isLoading,
            errorMessage = totalsError ?: errorMessage,
            totalsAvailable = totals != null,
            totalLkrMinor = totals?.totalMinor ?: 0L,
            committedLkrMinor = totals?.committedMinor ?: 0L,
            discretionaryLkrMinor = totals?.discretionaryMinor ?: 0L,
            committedPercentage = totals?.committedPercentage ?: 0,
            discretionaryPercentage = totals?.discretionaryPercentage ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExpenseUiState(totalsAvailable = false)
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
        val ownerId = authRepository.currentUserId()
        if (ownerId.isNullOrBlank()) {
            publishSaveError("User is not authenticated")
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
                channelLabel = storedExpense?.channelLabel,
                recurringExpenseId = storedExpense?.recurringExpenseId,
                recurringOccurrenceId = storedExpense?.recurringOccurrenceId
            )

            val result = expenseRepository.saveExpenseForOwner(ownerId, expense)
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
        val expense = uiState.value.expenses.firstOrNull { it.id == id }
        if (expense?.recurringOccurrenceId != null) {
            _errorMessage.value =
                "Confirmed recurring payments cannot be deleted. Edit the expense instead."
            return
        }
        val ownerId = authRepository.currentUserId()
        if (ownerId.isNullOrBlank()) {
            _errorMessage.value = "User is not authenticated"
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = expenseRepository.deleteExpenseForOwner(ownerId, id)
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
