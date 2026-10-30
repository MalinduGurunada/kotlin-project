package com.example.personalfinanceapp.feature.history

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.expense.FakeExpenseRepository
import com.example.personalfinanceapp.feature.history.ui.TransactionHistoryViewModel
import com.example.personalfinanceapp.feature.income.FakeIncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeIncomeRepository: FakeIncomeRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: TransactionHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeIncomeRepository = FakeIncomeRepository()
        fakeExpenseRepository = FakeExpenseRepository()

        viewModel = TransactionHistoryViewModel(fakeIncomeRepository, fakeExpenseRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsFilteredTransactions() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ALL", state.selectedFilterType)
    }

    @Test
    fun prepareRepeatExpenseGeneratesNewDistinctId() {
        val original = Expense(
            id = "orig-exp-100",
            categoryId = "food",
            categoryNameSnapshot = "Food & Dining",
            spendingClass = SpendingClass.DISCRETIONARY,
            paymentChannel = PaymentChannel.CASH,
            money = Money("1500.00", CurrencyCode.LKR, "1.00", 150000L, RateSource.LKR_FIXED, LocalDate.now()),
            occurredDate = LocalDate.of(2026, 9, 10)
        )

        val repeated = viewModel.prepareRepeatExpense(original)
        assertNotEquals(original.id, repeated.id)
        assertEquals(original.categoryId, repeated.categoryId)
        assertEquals(original.money.originalAmountDecimal, repeated.money.originalAmountDecimal)
    }
}
