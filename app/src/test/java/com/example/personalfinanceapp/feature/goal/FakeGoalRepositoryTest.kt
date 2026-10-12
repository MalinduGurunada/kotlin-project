
package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeGoalRepositoryTest {

    private fun createGoal(
        id: String = "goal-001"
    ): Goal {
        return Goal(
            id = id,
            name = "Buy Laptop",
            targetAmountMinor = 10_000_000L,
            startDate = LocalDate.of(2026, 10, 1),
            targetDate = LocalDate.of(2027, 10, 1)
        )
    }

    private fun createContribution(
        id: String = "contribution-001",
        goalId: String = "goal-001"
    ): Contribution {
        return Contribution(
            id = id,
            goalId = goalId,
            amountMinor = 500_000L,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.of(2026, 10, 12)
        )
    }

    @Test
    fun initialGoalListIsEmpty() = runTest {
        val repository = FakeGoalRepository()

        val goals = repository.observeGoals().first()

        assertTrue(goals.isEmpty())
    }

    @Test
    fun saveGoalSuccessfully() = runTest {
        val repository = FakeGoalRepository()

        val result = repository.saveGoal(createGoal())

        assertTrue(result.isSuccess)

        val goals = repository.observeGoals().first()

        assertEquals(1, goals.size)
        assertEquals("Buy Laptop", goals.first().name)
    }

    @Test
    fun updateGoalWithoutCreatingDuplicate() = runTest {
        val repository = FakeGoalRepository()

        val goal = createGoal()

        repository.saveGoal(goal)

        val updatedGoal = goal.copy(
            name = "New Laptop"
        )

        val result = repository.saveGoal(updatedGoal)

        assertTrue(result.isSuccess)

        val goals = repository.observeGoals().first()

        assertEquals(1, goals.size)
        assertEquals("New Laptop", goals.first().name)
    }

    @Test
    fun rejectInvalidGoalTarget() = runTest {
        val repository = FakeGoalRepository()

        val invalidGoal = createGoal().copy(
            targetAmountMinor = 0L
        )

        val result = repository.saveGoal(invalidGoal)

        assertTrue(result.isFailure)
        assertTrue(repository.observeGoals().first().isEmpty())
    }

    @Test
    fun rejectTargetDateBeforeStartDate() = runTest {
        val repository = FakeGoalRepository()

        val invalidGoal = createGoal().copy(
            targetDate = LocalDate.of(2026, 9, 1)
        )

        val result = repository.saveGoal(invalidGoal)

        assertTrue(result.isFailure)
    }

    @Test
    fun archiveGoalSuccessfully() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        val result = repository.setGoalArchived(
            id = "goal-001",
            archived = true
        )

        assertTrue(result.isSuccess)

        val goals = repository.observeGoals().first()

        assertTrue(goals.first().archived)
        assertEquals(1, goals.size)
    }

    @Test
    fun addContributionSuccessfully() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        val result = repository.addContribution(
            createContribution()
        )

        assertTrue(result.isSuccess)

        val contributions = repository.observeContributions(
            "goal-001"
        ).first()

        assertEquals(1, contributions.size)
        assertEquals(500_000L, contributions.first().amountMinor)
    }

    @Test
    fun rejectContributionForMissingGoal() = runTest {
        val repository = FakeGoalRepository()

        val contribution = createContribution(
            goalId = "missing-goal"
        )

        val result = repository.addContribution(contribution)

        assertTrue(result.isFailure)
    }

    @Test
    fun duplicateContributionDoesNotCreateAnotherRecord() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        val contribution = createContribution()

        val firstResult = repository.addContribution(contribution)

        val secondResult = repository.addContribution(contribution)

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)

        val contributions = repository.observeContributions(
            "goal-001"
        ).first()

        assertEquals(1, contributions.size)
    }

    @Test
    fun rejectChangingExistingContributionWithSameId() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        val contribution = createContribution()

        repository.addContribution(contribution)

        val changedContribution = contribution.copy(
            amountMinor = 900_000L
        )

        val result = repository.addContribution(
            changedContribution
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun reverseContributionSuccessfully() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        repository.addContribution(createContribution())

        val reversal = Contribution(
            id = "reversal-001",
            goalId = "goal-001",
            amountMinor = -500_000L,
            kind = ContributionKind.REVERSAL,
            occurredDate = LocalDate.of(2026, 10, 13),
            reversesId = "contribution-001"
        )

        val result = repository.addContribution(reversal)

        assertTrue(result.isSuccess)

        val contributions = repository.observeContributions(
            "goal-001"
        ).first()

        assertEquals(2, contributions.size)

        val total = contributions.sumOf { it.amountMinor }

        assertEquals(0L, total)
    }

    @Test
    fun rejectDuplicateReversal() = runTest {
        val repository = FakeGoalRepository()

        repository.saveGoal(createGoal())

        repository.addContribution(createContribution())

        val reversal = Contribution(
            id = "reversal-001",
            goalId = "goal-001",
            amountMinor = -500_000L,
            kind = ContributionKind.REVERSAL,
            occurredDate = LocalDate.of(2026, 10, 13),
            reversesId = "contribution-001"
        )

        repository.addContribution(reversal)

        val duplicateReversal = reversal.copy(
            id = "reversal-002"
        )

        val result = repository.addContribution(
            duplicateReversal
        )

        assertTrue(result.isFailure)

        val contributions = repository.observeContributions(
            "goal-001"
        ).first()

        assertEquals(2, contributions.size)
    }
}
