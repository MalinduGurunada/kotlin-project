
package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class FakeExpenseRepository : ExpenseRepository {

    private val expenses = MutableStateFlow<List<Expense>>(emptyList())

    private val mutex = Mutex()

    override fun observeExpenses(
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Flow<List<Expense>> {

        require(startDate.isBefore(endDateExclusive)) {
            "Start date must be before end date"
        }

        return expenses.map { allExpenses ->

            allExpenses.filter { expense ->
                !expense.occurredDate.isBefore(startDate) &&
                        expense.occurredDate.isBefore(endDateExclusive)
            }.sortedWith(
                compareBy<Expense> { it.occurredDate }
                    .thenBy { it.id }
            )

        }.distinctUntilChanged()
    }

    override suspend fun saveExpense(
        expense: Expense
    ): Result<Unit> = mutex.withLock {

        if (expense.id.isBlank()) {
            return@withLock Result.failure(
                IllegalArgumentException("Expense ID cannot be blank")
            )
        }

        val current = expenses.value

        val existingIndex = current.indexOfFirst {
            it.id == expense.id
        }

        expenses.value = if (existingIndex >= 0) {

            current.toMutableList().apply {
                set(existingIndex, expense)
            }

        } else {

            current + expense
        }

        Result.success(Unit)
    }

    override suspend fun deleteExpense(
        id: DocumentId
    ): Result<Unit> = mutex.withLock {

        val current = expenses.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException("Expense not found")
            )
        }

        expenses.value = current.filter {
            it.id != id
        }

        Result.success(Unit)
    }
}
