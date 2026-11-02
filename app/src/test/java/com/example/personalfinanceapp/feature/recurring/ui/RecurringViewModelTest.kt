package com.example.personalfinanceapp.feature.recurring.ui

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringStatus
import com.example.personalfinanceapp.feature.category.FakeCategoryRepository
import com.example.personalfinanceapp.feature.recurring.FakeRecurringRepository
import com.example.personalfinanceapp.feature.recurring.RecurringReminderCoordinator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(
        Instant.parse("2026-10-30T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recreationResynchronizesPendingRemindersWithoutChangingTheirIdentity() = runTest {
        val repository = FakeRecurringRepository()
        repository.saveRecurringExpense(recurringExpense())

        val firstCoordinator = RecordingReminderCoordinator()
        val firstViewModel = viewModel(repository, firstCoordinator)
        backgroundScope.launch { firstViewModel.uiState.collect() }
        dispatcher.scheduler.advanceUntilIdle()

        val secondCoordinator = RecordingReminderCoordinator()
        val recreatedViewModel = viewModel(repository, secondCoordinator)
        backgroundScope.launch { recreatedViewModel.uiState.collect() }
        dispatcher.scheduler.advanceUntilIdle()

        val firstDates = firstCoordinator.syncCalls.single().dueDates
        val recreatedDates = secondCoordinator.syncCalls.single().dueDates
        assertEquals(listOf(LocalDate.of(2026, 10, 31)), firstDates)
        assertEquals(firstDates, recreatedDates)
    }

    @Test
    fun savingOneOccurrenceSynchronizesItOnlyOnce() = runTest {
        val repository = FakeRecurringRepository()
        val coordinator = RecordingReminderCoordinator()
        val viewModel = viewModel(repository, coordinator)
        backgroundScope.launch { viewModel.uiState.collect() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openCreate()
        viewModel.updateTitle("Electricity")
        viewModel.updateCategory("bills")
        viewModel.updateAmount("12500.00")
        viewModel.updateDueDate("2026-10-31")
        viewModel.updateReminderEnabled(true)
        viewModel.updateReminderLeadDays(0L)
        viewModel.saveForm()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, coordinator.syncCalls.size)
        assertEquals(
            listOf(LocalDate.of(2026, 10, 31)),
            coordinator.syncCalls.single().dueDates
        )
    }

    @Test
    fun confirmingPaymentCancelsTheOccurrenceReminder() = runTest {
        val repository = FakeRecurringRepository()
        repository.saveRecurringExpense(recurringExpense())
        val coordinator = RecordingReminderCoordinator()
        val viewModel = viewModel(repository, coordinator)
        backgroundScope.launch { viewModel.uiState.collect() }
        dispatcher.scheduler.advanceUntilIdle()

        val dueItem = viewModel.uiState.value.dueItems.single {
            it.dueDate == LocalDate.of(2026, 10, 31)
        }
        viewModel.openPayment(dueItem)
        viewModel.confirmPayment()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("electricity" to LocalDate.of(2026, 10, 31)),
            coordinator.cancelledOccurrences
        )
        assertTrue(viewModel.paymentState.value == null)
    }

    @Test
    fun cancellingCommitmentClearsFutureReminderDates() = runTest {
        val repository = FakeRecurringRepository()
        val recurring = recurringExpense()
        repository.saveRecurringExpense(recurring)
        val coordinator = RecordingReminderCoordinator()
        val viewModel = viewModel(repository, coordinator)
        backgroundScope.launch { viewModel.uiState.collect() }
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatus(recurring, RecurringStatus.CANCELLED)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            coordinator.syncCalls.any {
                it.recurringExpense.status == RecurringStatus.CANCELLED &&
                    it.dueDates.isEmpty()
            }
        )
    }

    private fun viewModel(
        repository: FakeRecurringRepository,
        coordinator: RecordingReminderCoordinator
    ) = RecurringViewModel(
        recurringRepository = repository,
        categoryRepository = FakeCategoryRepository(),
        reminderScheduler = coordinator,
        clock = clock
    )

    private fun recurringExpense() = RecurringExpense(
        id = "electricity",
        title = "Electricity",
        categoryId = "bills",
        expectedAmountMinor = 12_500_00L,
        frequency = RecurrenceFrequency.MONTHLY,
        anchorDate = LocalDate.of(2026, 10, 31),
        startDate = LocalDate.of(2026, 10, 31),
        reminderEnabled = true,
        reminderLeadDays = 0L
    )

    private class RecordingReminderCoordinator : RecurringReminderCoordinator {
        val syncCalls = mutableListOf<SyncCall>()
        val cancelledOccurrences = mutableListOf<Pair<String, LocalDate>>()

        override suspend fun sync(
            recurringExpense: RecurringExpense,
            dueDates: List<LocalDate>
        ): Result<Unit> {
            syncCalls += SyncCall(recurringExpense, dueDates)
            return Result.success(Unit)
        }

        override suspend fun cancelOccurrence(
            recurringExpenseId: String,
            dueDate: LocalDate
        ): Result<Unit> {
            cancelledOccurrences += recurringExpenseId to dueDate
            return Result.success(Unit)
        }
    }

    private data class SyncCall(
        val recurringExpense: RecurringExpense,
        val dueDates: List<LocalDate>
    )
}
