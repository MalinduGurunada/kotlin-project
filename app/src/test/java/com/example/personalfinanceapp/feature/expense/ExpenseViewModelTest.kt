package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.category.FakeCategoryRepository
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateProvider
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import com.example.personalfinanceapp.feature.expense.ui.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeExchangeRateRepository: TestExchangeRateRepository
    private lateinit var viewModel: ExpenseViewModel

    private class TestExchangeRateRepository : ExchangeRateRepository {
        var requestCount: Int = 0
        var result: Result<ExchangeRateQuote> = Result.success(
            ExchangeRateQuote(
                baseCurrency = CurrencyCode.USD,
                targetCurrency = CurrencyCode.LKR,
                rate = BigDecimal("305.50"),
                provider = ExchangeRateProvider.EXCHANGE_RATE_API_OPEN,
                applicableDate = LocalDate.now(),
                providerUpdatedAt = Instant.now(),
                nextProviderUpdateAt = Instant.now().plusSeconds(86400),
                fetchedAt = Instant.now()
            )
        )

        override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> {
            requestCount += 1
            return result
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepository = FakeExpenseRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeExchangeRateRepository = TestExchangeRateRepository()
        viewModel = ExpenseViewModel(
            expenseRepository = fakeExpenseRepository,
            categoryRepository = fakeCategoryRepository,
            exchangeRateRepository = fakeExchangeRateRepository
        )
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
        assertEquals(0L, state.committedLkrMinor)
        assertEquals(0L, state.discretionaryLkrMinor)
    }

    @Test
    fun saveExpenseWithBlankAmountShowsError() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = null,
            categoryId = "food",
            amountDecimal = "",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = LocalDate.now(),
            note = "Empty test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Amount cannot be blank", state.errorMessage)
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
        assertNotNull(state.errorMessage)
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
    fun saveExpenseLkrSuccessfullyUpdatesStateAndTotal() = runTest {
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
        assertEquals(150_000L, state.discretionaryLkrMinor)
        assertEquals(0L, state.committedLkrMinor)
        assertEquals(100, state.discretionaryPercentage)
        assertEquals(0, state.committedPercentage)
        assertNull(state.errorMessage)
    }

    @Test
    fun saveExpenseUsdUsesExchangeRateRepository() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now().withDayOfMonth(10)

        // 100 USD @ 305.50 LKR/USD = 30,550.00 LKR = 3,055,000 cents
        viewModel.saveExpense(
            id = "exp-usd-1",
            categoryId = "bills",
            amountDecimal = "100.00",
            currency = CurrencyCode.USD,
            paymentChannel = PaymentChannel.CARD,
            spendingClass = SpendingClass.COMMITTED,
            occurredDate = today,
            note = "Cloud Server"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.expenses.size)
        val savedExpense = state.expenses.first()
        assertEquals("exp-usd-1", savedExpense.id)
        assertEquals(3_055_000L, savedExpense.money.baseAmountMinor)
        assertEquals("305.50", savedExpense.money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, savedExpense.money.rateSource)
        assertEquals(3_055_000L, state.totalLkrMinor)
        assertEquals(3_055_000L, state.committedLkrMinor)
        assertEquals(100, state.committedPercentage)
        assertNull(state.errorMessage)
    }

    @Test
    fun saveExpenseUsdWithCustomRatePreservesSnapshot() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now().withDayOfMonth(10)

        // 50 USD @ 298.25 LKR/USD = 14,912.50 LKR = 1,491,250 cents
        viewModel.saveExpense(
            id = "exp-usd-custom",
            categoryId = "bills",
            amountDecimal = "50.00",
            currency = CurrencyCode.USD,
            paymentChannel = PaymentChannel.CARD,
            spendingClass = SpendingClass.COMMITTED,
            occurredDate = today,
            note = "Custom rate expense",
            customRateLkrPerUsd = "298.25"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.expenses.size)
        val savedExpense = state.expenses.first()
        assertEquals(1_491_250L, savedExpense.money.baseAmountMinor)
        assertEquals("298.25", savedExpense.money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_REFERENCE, savedExpense.money.rateSource)
        assertNull(state.errorMessage)
    }

    @Test
    fun saveExpenseUsdWhenRateFailsSetsErrorMessage() = runTest {
        fakeExchangeRateRepository.result = Result.failure(IllegalStateException("Network offline"))

        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = "exp-fail",
            categoryId = "food",
            amountDecimal = "20.00",
            currency = CurrencyCode.USD,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = LocalDate.now(),
            note = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.expenses.isEmpty())
        assertEquals("Network offline", state.errorMessage)
    }

    @Test
    fun editingUsdMetadataPreservesExactMoneySnapshotAndStableId() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()
        val historicalDate = LocalDate.of(2025, 8, 14)
        val occurredDate = LocalDate.now().withDayOfMonth(10)
        val existing = Expense(
            id = "historical-usd",
            categoryId = "bills",
            categoryNameSnapshot = "Bills",
            spendingClass = SpendingClass.COMMITTED,
            paymentChannel = PaymentChannel.CARD,
            money = Money(
                originalAmountDecimal = "12.345",
                originalCurrency = CurrencyCode.USD,
                rateLkrPerUnitDecimal = "301.123456",
                baseAmountMinor = 371_737L,
                rateSource = RateSource.MANUAL_TRANSFER,
                rateDate = historicalDate
            ),
            occurredDate = occurredDate,
            note = "Original"
        )
        fakeExpenseRepository.saveExpense(existing)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = existing.id,
            categoryId = existing.categoryId,
            amountDecimal = "12.3450",
            currency = CurrencyCode.USD,
            paymentChannel = PaymentChannel.BANK,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = occurredDate.plusDays(1),
            note = "Metadata changed",
            existingExpense = existing
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = viewModel.uiState.value.expenses.single()
        assertEquals(existing.id, saved.id)
        assertEquals(existing.money, saved.money)
        assertEquals("Metadata changed", saved.note)
        assertEquals(PaymentChannel.BANK, saved.paymentChannel)
        assertEquals(0, fakeExchangeRateRepository.requestCount)
    }

    @Test
    fun editingUsdAmountRecalculatesWithHistoricalSnapshotNotLatestQuote() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()
        val historicalDate = LocalDate.of(2025, 1, 15)
        val existing = Expense(
            id = "changed-usd",
            categoryId = "bills",
            categoryNameSnapshot = "Bills",
            spendingClass = SpendingClass.COMMITTED,
            paymentChannel = PaymentChannel.CARD,
            money = Money(
                originalAmountDecimal = "10.00",
                originalCurrency = CurrencyCode.USD,
                rateLkrPerUnitDecimal = "300.125",
                baseAmountMinor = 300_125L,
                rateSource = RateSource.MANUAL_REFERENCE,
                rateDate = historicalDate
            ),
            occurredDate = LocalDate.now().withDayOfMonth(10)
        )
        fakeExpenseRepository.saveExpense(existing)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = existing.id,
            categoryId = existing.categoryId,
            amountDecimal = "20.00",
            currency = CurrencyCode.USD,
            paymentChannel = existing.paymentChannel,
            spendingClass = existing.spendingClass,
            occurredDate = existing.occurredDate,
            note = existing.note,
            existingExpense = existing
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val money = viewModel.uiState.value.expenses.single().money
        assertEquals("20.00", money.originalAmountDecimal)
        assertEquals("300.125", money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_REFERENCE, money.rateSource)
        assertEquals(historicalDate, money.rateDate)
        assertEquals(600_250L, money.baseAmountMinor)
        assertEquals(0, fakeExchangeRateRepository.requestCount)
    }

    @Test
    fun editingUsdAmountOfflineReusesHistoricalRateSourceAndDate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()
        val historicalDate = LocalDate.of(2024, 3, 2)
        val occurredDate = LocalDate.now().withDayOfMonth(10)
        val existing = Expense(
            id = "offline-usd",
            categoryId = "bills",
            categoryNameSnapshot = "Bills",
            spendingClass = SpendingClass.COMMITTED,
            paymentChannel = PaymentChannel.CARD,
            money = Money(
                originalAmountDecimal = "10.00",
                originalCurrency = CurrencyCode.USD,
                rateLkrPerUnitDecimal = "299.999999",
                baseAmountMinor = 300_000L,
                rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
                rateDate = historicalDate
            ),
            occurredDate = occurredDate
        )
        fakeExpenseRepository.saveExpense(existing)
        fakeExchangeRateRepository.result = Result.failure(IllegalStateException("Offline"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveExpense(
            id = existing.id,
            categoryId = existing.categoryId,
            amountDecimal = "10.01",
            currency = CurrencyCode.USD,
            paymentChannel = existing.paymentChannel,
            spendingClass = existing.spendingClass,
            occurredDate = existing.occurredDate,
            note = existing.note,
            existingExpense = existing
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = viewModel.uiState.value.expenses.single()
        assertEquals("299.999999", saved.money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, saved.money.rateSource)
        assertEquals(historicalDate, saved.money.rateDate)
        assertEquals(300_300L, saved.money.baseAmountMinor)
        assertEquals(0, fakeExchangeRateRepository.requestCount)
        assertNull(viewModel.saveUiState.value.errorMessage)
    }

    @Test
    fun repositoryFailureIsPublishedToEditorWithoutSuccessSignal() = runTest {
        val failingRepository = object : ExpenseRepository {
            override fun observeExpenses(
                startDate: LocalDate,
                endDateExclusive: LocalDate
            ): Flow<List<Expense>> = flowOf(emptyList())

            override suspend fun saveExpense(expense: Expense): Result<Unit> =
                Result.failure(IllegalStateException("Repository unavailable"))

            override suspend fun deleteExpense(id: String): Result<Unit> = Result.success(Unit)
        }
        val failingViewModel = ExpenseViewModel(
            expenseRepository = failingRepository,
            categoryRepository = fakeCategoryRepository,
            exchangeRateRepository = fakeExchangeRateRepository
        )
        backgroundScope.launch { failingViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        failingViewModel.saveExpense(
            id = null,
            categoryId = "food",
            amountDecimal = "42.75",
            currency = CurrencyCode.LKR,
            paymentChannel = PaymentChannel.CASH,
            spendingClass = SpendingClass.DISCRETIONARY,
            occurredDate = LocalDate.now(),
            note = "Entered note"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Repository unavailable", failingViewModel.saveUiState.value.errorMessage)
        assertNull(failingViewModel.saveUiState.value.savedExpenseId)
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
