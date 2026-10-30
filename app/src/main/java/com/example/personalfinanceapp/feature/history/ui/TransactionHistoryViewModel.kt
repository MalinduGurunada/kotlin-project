package com.example.personalfinanceapp.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class UnifiedTransactionItem(
    val id: String,
    val title: String,
    val categoryOrSource: String,
    val amountDecimalText: String,
    val baseAmountMinor: Long,
    val isExpense: Boolean,
    val occurredDate: LocalDate,
    val paymentChannelOrKind: String
)

data class TransactionHistoryUiState(
    val selectedFilterType: String = "ALL",
    val transactions: List<UnifiedTransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val startDate = now.withDayOfMonth(1).minusMonths(6)
    private val endDateExclusive = now.withDayOfMonth(1).plusMonths(1)

    private val _filterType = MutableStateFlow("ALL")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        _filterType.flatMapLatest { filter ->
            combine(
                incomeRepository.observeIncomes(startDate, endDateExclusive),
                expenseRepository.observeExpenses(startDate, endDateExclusive)
            ) { incomes, expenses ->
                mapUnifiedTransactions(incomes, expenses, filter)
            }
        },
        _filterType
    ) { items, filter ->
        TransactionHistoryUiState(
            selectedFilterType = filter,
            transactions = items,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionHistoryUiState(isLoading = true)
    )

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun prepareRepeatExpense(originalExpense: Expense): Expense {
        return originalExpense.copy(
            id = UUID.randomUUID().toString(),
            occurredDate = LocalDate.now()
        )
    }

    private fun mapUnifiedTransactions(
        incomes: List<Income>,
        expenses: List<Expense>,
        filter: String
    ): List<UnifiedTransactionItem> {
        val items = mutableListOf<UnifiedTransactionItem>()

        if (filter == "ALL" || filter == "EXPENSE") {
            expenses.forEach { exp ->
                items.add(
                    UnifiedTransactionItem(
                        id = exp.id,
                        title = exp.categoryNameSnapshot,
                        categoryOrSource = exp.spendingClass.name,
                        amountDecimalText = "-LKR ${exp.money.originalAmountDecimal}",
                        baseAmountMinor = exp.money.baseAmountMinor,
                        isExpense = true,
                        occurredDate = exp.occurredDate,
                        paymentChannelOrKind = exp.paymentChannel.name
                    )
                )
            }
        }

        if (filter == "ALL" || filter == "INCOME") {
            incomes.forEach { inc ->
                items.add(
                    UnifiedTransactionItem(
                        id = inc.id,
                        title = "${inc.source.name} Receipt",
                        categoryOrSource = inc.entryKind.name,
                        amountDecimalText = "+LKR ${inc.money.originalAmountDecimal}",
                        baseAmountMinor = inc.money.baseAmountMinor,
                        isExpense = false,
                        occurredDate = inc.occurredDate,
                        paymentChannelOrKind = inc.source.name
                    )
                )
            }
        }

        return items.sortedByDescending { it.occurredDate }
    }
}
