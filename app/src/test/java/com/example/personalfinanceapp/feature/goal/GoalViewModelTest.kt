package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.goal.ui.GoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
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
        assertFalse(goalUi.isCompleted)
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

    @Test
    fun multipleGoalsCalculateIndependentProgressSimultaneously() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val emergencyFund = Goal(
            id = "goal-emergency",
            name = "Emergency Buffer",
            targetAmountMinor = 100_000_00L,
            startDate = LocalDate.now().minusMonths(1),
            targetDate = LocalDate.now().plusMonths(5)
        )
        val macbookFund = Goal(
            id = "goal-macbook",
            name = "MacBook Pro M3",
            targetAmountMinor = 500_000_00L,
            startDate = LocalDate.now().minusMonths(2),
            targetDate = LocalDate.now().plusMonths(4)
        )

        fakeGoalRepository.saveGoal(emergencyFund)
        fakeGoalRepository.saveGoal(macbookFund)

        // 50,000 of 100,000 -> 50%
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-emerg-1",
                goalId = "goal-emergency",
                amountMinor = 50_000_00L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )

        // 400,000 of 500,000 -> 80%
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-mac-1",
                goalId = "goal-macbook",
                amountMinor = 400_000_00L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.goals.size)

        val emergUi = state.goals.find { it.id == "goal-emergency" }
        assertNotNull(emergUi)
        assertEquals(50_000L, emergUi!!.savedAmountLkr)
        assertEquals(50_000L, emergUi.remainingLkr)
        assertEquals(50, emergUi.progressPercent)
        assertFalse(emergUi.isCompleted)

        val macUi = state.goals.find { it.id == "goal-macbook" }
        assertNotNull(macUi)
        assertEquals(400_000L, macUi!!.savedAmountLkr)
        assertEquals(100_000L, macUi.remainingLkr)
        assertEquals(80, macUi.progressPercent)
        assertFalse(macUi.isCompleted)

        // Select the second goal and verify both still maintain independent, correct progress
        viewModel.selectGoal("goal-macbook")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals("goal-macbook", updatedState.selectedGoalDetail?.id)
        assertEquals(1, updatedState.contributions.size)
        assertEquals("c-mac-1", updatedState.contributions.first().id)

        // First goal progress must still be 50% even though second goal is selected
        val emergStill50 = updatedState.goals.find { it.id == "goal-emergency" }
        assertEquals(50, emergStill50?.progressPercent)
        assertEquals(50_000L, emergStill50?.savedAmountLkr)
    }

    @Test
    fun minorUnitCalculationsPreservePrecisionAndAvoidPrematureRounding() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        // 150 minor units (1.50 LKR) target
        val smallGoal = Goal(
            id = "small-goal",
            name = "Micro Savings",
            targetAmountMinor = 150L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        fakeGoalRepository.saveGoal(smallGoal)

        // 100 minor units (1.00 LKR) -> 66% (100 / 150 * 100)
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-micro-1",
                goalId = "small-goal",
                amountMinor = 100L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val goalUi = state.goals.first()
        assertEquals(66, goalUi.progressPercent)
        assertFalse(goalUi.isCompleted)

        // Complete the remaining 50 minor units -> 100%
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-micro-2",
                goalId = "small-goal",
                amountMinor = 50L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val completedState = viewModel.uiState.value
        val completedGoalUi = completedState.goals.first()
        assertEquals(100, completedGoalUi.progressPercent)
        assertTrue(completedGoalUi.isCompleted)
    }

    @Test
    fun saveGoalValidatesInputsAndCreatesGoal() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        // Blank name
        viewModel.saveGoal(null, "   ", 100_000L, LocalDate.now(), LocalDate.now().plusMonths(1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Goal name cannot be blank", viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)

        // Zero target
        viewModel.saveGoal(null, "Valid Name", 0L, LocalDate.now(), LocalDate.now().plusMonths(1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Target amount must be greater than zero", viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Target date before start date
        viewModel.saveGoal(null, "Valid Name", 100_000L, LocalDate.now(), LocalDate.now().minusDays(1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Target date cannot be before start date", viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Valid creation
        val targetDate = LocalDate.now().plusMonths(6)
        viewModel.saveGoal(null, "Vacation Fund", 250_000L, LocalDate.now(), targetDate)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.goals.size)
        assertEquals("Vacation Fund", state.goals.first().name)
        assertEquals(250_000L, state.goals.first().targetAmountLkr)
    }

    @Test
    fun addContributionValidatesAmountAndAppendsLedgerEntry() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val goal = Goal(
            id = "goal-travel",
            name = "Travel",
            targetAmountMinor = 300_000_00L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(12)
        )
        fakeGoalRepository.saveGoal(goal)
        testDispatcher.scheduler.advanceUntilIdle()

        // Invalid non-positive amount
        viewModel.addContribution("goal-travel", 0L, "Note")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Contribution amount must be positive", viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Valid contribution
        viewModel.addContribution("goal-travel", 75_000L, "First deposit")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(75_000L, state.goals.first().savedAmountLkr)
        assertEquals(25, state.goals.first().progressPercent)
        assertEquals(1, state.contributions.size)
        assertEquals("First deposit", state.contributions.first().note)
    }

    @Test
    fun unauthenticatedStateClearsGoalsAndDetail() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGoalRepository.saveGoal(
            Goal(
                id = "goal-1",
                name = "Goal 1",
                targetAmountMinor = 100_000_00L,
                startDate = LocalDate.now(),
                targetDate = LocalDate.now().plusMonths(2)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.goals.size)

        fakeAuthRepository.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.goals.isEmpty())
        assertNull(state.selectedGoalDetail)
        assertTrue(state.contributions.isEmpty())
    }

    @Test
    fun extremelyLargeContributionsAvoidSilentOverflowAndCapAtLongMax() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val goal = Goal(
            id = "goal-overflow",
            name = "Overflow Test",
            targetAmountMinor = 500_000_00L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        fakeGoalRepository.saveGoal(goal)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-1",
                goalId = "goal-overflow",
                amountMinor = Long.MAX_VALUE - 1000L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-2",
                goalId = "goal-overflow",
                amountMinor = 5000L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val goalUi = state.goals.first()
        
        assertEquals(Long.MAX_VALUE / 100, goalUi.savedAmountLkr)
        assertEquals(0L, goalUi.remainingLkr)
        assertEquals(100, goalUi.progressPercent)
        assertTrue(goalUi.isCompleted)
    }

    @Test
    fun intermediateLargeContributionsAreCalculatedCorrectlyWithBigInteger() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val goal = Goal(
            id = "goal-intermediate",
            name = "Intermediate Test",
            targetAmountMinor = 500_000_00L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        fakeGoalRepository.saveGoal(goal)
        testDispatcher.scheduler.advanceUntilIdle()

        // Sum goes up to MAX_VALUE - 1000
        fakeGoalRepository.addContribution(
            Contribution(
                id = "c-1",
                goalId = "goal-intermediate",
                amountMinor = Long.MAX_VALUE - 1000L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        
        // Sum goes mathematically to MAX_VALUE + 4000 (overflows standard Long)
        // Since fakeGoalRepository enforces business rules, we bypass it for this intermediate saturation test.
        val corruptedRepo = object : GoalRepository {
            override fun observeGoals() = flowOf(listOf(goal))
            override fun observeContributions(goalId: DocumentId) = flowOf(
                listOf(
                    Contribution(
                        id = "c-1",
                        goalId = "goal-intermediate",
                        amountMinor = Long.MAX_VALUE - 1000L,
                        kind = ContributionKind.ALLOCATION,
                        occurredDate = LocalDate.now()
                    ),
                    Contribution(
                        id = "c-2",
                        goalId = "goal-intermediate",
                        amountMinor = 5000L,
                        kind = ContributionKind.ALLOCATION,
                        occurredDate = LocalDate.now()
                    ),
                    Contribution(
                        id = "c-3",
                        goalId = "goal-intermediate",
                        amountMinor = -10_000L,
                        kind = ContributionKind.REVERSAL,
                        occurredDate = LocalDate.now()
                    )
                )
            )
            override suspend fun saveGoal(goal: Goal) = Result.success(Unit)
            override suspend fun setGoalArchived(id: DocumentId, archived: Boolean) = Result.success(Unit)
            override suspend fun addContribution(contribution: Contribution) = Result.success(Unit)
        }

        val testViewModel = GoalViewModel(fakeAuthRepository, corruptedRepo)
        
        backgroundScope.launch { testViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = testViewModel.uiState.value
        val goalUi = state.goals.first()
        
        val expected = (BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(6000L))).divide(
            BigInteger.valueOf(100L)).longValueExact()
        assertEquals(expected, goalUi.savedAmountLkr)
    }

    @Test
    fun extremelyLargeReversalsAvoidSilentOverflowAndCapAtLongMin() = runTest {
        // Create a custom repository that emits corrupted data directly to force underflow
        val corruptedRepo = object : GoalRepository {
            override fun observeGoals() = flowOf(
                listOf(
                    Goal(
                        id = "goal-underflow",
                        name = "Underflow Test",
                        targetAmountMinor = 500_000_00L,
                        startDate = LocalDate.now(),
                        targetDate = LocalDate.now().plusMonths(1)
                    )
                )
            )

            override fun observeContributions(goalId: DocumentId) =
                flowOf(
                    listOf(
                        Contribution(
                            id = "c-1",
                            goalId = "goal-underflow",
                            amountMinor = Long.MIN_VALUE + 1000L,
                            kind = ContributionKind.REVERSAL,
                            occurredDate = LocalDate.now()
                        ),
                        Contribution(
                            id = "c-2",
                            goalId = "goal-underflow",
                            amountMinor = -5000L,
                            kind = ContributionKind.REVERSAL,
                            occurredDate = LocalDate.now()
                        )
                    )
                )

            override suspend fun saveGoal(goal: Goal) = Result.success(Unit)
            override suspend fun setGoalArchived(id: DocumentId, archived: Boolean) = Result.success(Unit)
            override suspend fun addContribution(contribution: Contribution) = Result.success(Unit)
        }

        val testViewModel = GoalViewModel(fakeAuthRepository, corruptedRepo)
        
        backgroundScope.launch { testViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = testViewModel.uiState.value
        val goalUi = state.goals.first()
        
        assertEquals(Long.MIN_VALUE / 100, goalUi.savedAmountLkr)
        assertEquals(Long.MAX_VALUE / 100, goalUi.remainingLkr) 
        assertEquals(0, goalUi.progressPercent)
        assertFalse(goalUi.isCompleted)
    }

    @Test
    fun rupeeInputsRejectMinorUnitOverflowBeforeWriting() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveGoal(
            id = "too-large",
            name = "Too large",
            targetAmountLkr = Long.MAX_VALUE,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Goal target is too large", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.goals.isEmpty())

        viewModel.clearError()
        viewModel.addContribution("missing-goal", Long.MAX_VALUE, null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Contribution amount is too large", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun pendingGoalSaveRetainsInitiatingOwnerAcrossAccountSwitch() = runTest {
        var capturedOwner: String? = null
        var writeExecuted = false
        val ownerCheckingRepository = object : GoalRepository by fakeGoalRepository {
            override suspend fun saveGoalForOwner(
                expectedOwnerId: String,
                goal: Goal
            ): Result<Unit> {
                capturedOwner = expectedOwnerId
                if (fakeAuthRepository.currentUserId() != expectedOwnerId) {
                    return Result.failure(
                        IllegalStateException("Authenticated user changed before goal data could be written")
                    )
                }
                writeExecuted = true
                return fakeGoalRepository.saveGoal(goal)
            }
        }
        val accountBoundViewModel = GoalViewModel(fakeAuthRepository, ownerCheckingRepository)
        backgroundScope.launch { accountBoundViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        accountBoundViewModel.saveGoal(
            id = "pending-a",
            name = "Account A goal",
            targetAmountLkr = 1_000L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        fakeAuthRepository.simulateAccountSwitch("user-2", "two@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("user-1", capturedOwner)
        assertFalse(writeExecuted)
        assertEquals(
            "Authenticated user changed before goal data could be written",
            accountBoundViewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun pendingContributionRetainsInitiatingOwnerAcrossAccountSwitch() = runTest {
        val goal = Goal(
            id = "goal-a",
            name = "Account A goal",
            targetAmountMinor = 100_000L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(1)
        )
        fakeGoalRepository.saveGoal(goal).getOrThrow()
        var capturedOwner: String? = null
        var writeExecuted = false
        val ownerCheckingRepository = object : GoalRepository by fakeGoalRepository {
            override suspend fun addContributionForOwner(
                expectedOwnerId: String,
                contribution: Contribution
            ): Result<Unit> {
                capturedOwner = expectedOwnerId
                if (fakeAuthRepository.currentUserId() != expectedOwnerId) {
                    return Result.failure(
                        IllegalStateException("Authenticated user changed before goal data could be written")
                    )
                }
                writeExecuted = true
                return fakeGoalRepository.addContribution(contribution)
            }
        }
        val accountBoundViewModel = GoalViewModel(fakeAuthRepository, ownerCheckingRepository)
        backgroundScope.launch { accountBoundViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        accountBoundViewModel.addContribution(goal.id, 100L, null)
        fakeAuthRepository.simulateAccountSwitch("user-2", "two@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("user-1", capturedOwner)
        assertFalse(writeExecuted)
        assertEquals(
            "Authenticated user changed before goal data could be written",
            accountBoundViewModel.uiState.value.errorMessage
        )
    }
}
