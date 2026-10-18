package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.category.FakeCategoryRepository
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepository = FakeExpenseRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        viewModel = ExpenseViewModel(fakeExpenseRepository, fakeCategoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsCategoriesAndEmptyExpenses() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.categories.size)
        assertTrue(state.expenses.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
    }

    @Test
    fun saveExpenseWithInvalidAmountShowsError() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = null,
            categoryId = "food",
            amountDecimal = "-50.00",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = LocalDate.now(),
            note = "Invalid test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Amount must be a positive number", state.errorMessage)
    }

    @Test
    fun saveExpenseWithBlankCategoryShowsError() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = null,
            categoryId = "",
            amountDecimal = "1500.00",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = LocalDate.now(),
            note = "Blank category"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Please select a category", state.errorMessage)
    }

    @Test
    fun saveExpenseSuccessfullyUpdatesStateAndTotal() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now().withDayOfMonth(10)

        viewModel.saveExpense(
            id = "exp-1",
            categoryId = "food",
            amountDecimal = "1500.00",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = today,
            note = "Lunch"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.expenses.size)
        assertEquals("exp-1", state.expenses.first().id)
        assertEquals(150_000L, state.totalLkrMinor)
        assertNull(state.errorMessage)
    }

    @Test
    fun deleteExpenseSuccessfullyRemovesItem() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now().withDayOfMonth(10)

        viewModel.saveExpense(
            id = "exp-1",
            categoryId = "food",
            amountDecimal = "1500.00",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = today,
            note = "Lunch"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteExpense("exp-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.expenses.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
    }
}
