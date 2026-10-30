package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.goal.ui.GoalViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var viewModel: GoalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository(initialUserId = "user-1")
        fakeGoalRepository = FakeGoalRepository()

        viewModel = GoalViewModel(fakeAuthRepository, fakeGoalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveGoalAndCalculate70PercentProgress() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val macbookGoal = Goal(
            id = "macbook-m3",
            name = "MacBook Pro M3 Fund",
            targetAmountMinor = 550_000_00L,
            startDate = LocalDate.now().minusMonths(3),
            targetDate = LocalDate.now().plusMonths(3)
        )
        fakeGoalRepository.saveGoal(macbookGoal)

        val contribution = Contribution(
            id = "c-1",
            goalId = "macbook-m3",
            amountMinor = 385_000_00L,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now()
        )
        fakeGoalRepository.addContribution(contribution)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.goals.size)

        val goalUi = state.goals.first()
        assertEquals("MacBook Pro M3 Fund", goalUi.name)
        assertEquals(385_000L, goalUi.savedAmountLkr)
        assertEquals(165_000L, goalUi.remainingLkr)
        assertEquals(70, goalUi.progressPercent)
    }

    @Test
    fun reverseContributionRecalculatesSavedTotal() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val macbookGoal = Goal(
            id = "macbook-m3",
            name = "MacBook Pro M3 Fund",
            targetAmountMinor = 550_000_00L,
            startDate = LocalDate.now().minusMonths(3),
            targetDate = LocalDate.now().plusMonths(3)
        )
        fakeGoalRepository.saveGoal(macbookGoal)

        val contribution = Contribution(
            id = "c-1",
            goalId = "macbook-m3",
            amountMinor = 100_000_00L,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now()
        )
        fakeGoalRepository.addContribution(contribution)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reverseContribution(contribution)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val goalUi = state.goals.first()
        assertEquals(0L, goalUi.savedAmountLkr)
        assertEquals(0, goalUi.progressPercent)
    }
}
