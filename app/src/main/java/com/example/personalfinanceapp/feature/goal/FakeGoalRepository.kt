
package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeGoalRepository : GoalRepository {

    private val goals = MutableStateFlow<List<Goal>>(emptyList())

    private val contributions =
        MutableStateFlow<List<Contribution>>(emptyList())

    private val mutex = Mutex()

    override fun observeGoals(): Flow<List<Goal>> {
        return goals.asStateFlow()
    }

    override fun observeContributions(
        goalId: DocumentId
    ): Flow<List<Contribution>> {
        return contributions.map { allContributions ->
            allContributions
                .filter { it.goalId == goalId }
                .sortedWith(
                    compareBy<Contribution> { it.occurredDate }
                        .thenBy { it.id }
                )
        }.distinctUntilChanged()
    }

    override suspend fun saveGoal(
        goal: Goal
    ): Result<Unit> = mutex.withLock {

        if (goal.id.isBlank() || goal.name.isBlank()) {
            return@withLock Result.failure(
                IllegalArgumentException("Goal ID and name cannot be blank")
            )
        }

        if (goal.targetAmountMinor <= 0L) {
            return@withLock Result.failure(
                IllegalArgumentException("Goal target must be positive")
            )
        }

        if (goal.targetDate.isBefore(goal.startDate)) {
            return@withLock Result.failure(
                IllegalArgumentException("Target date cannot be before start date")
            )
        }

        val current = goals.value
        val existingIndex = current.indexOfFirst { it.id == goal.id }

        goals.value = if (existingIndex >= 0) {
            current.toMutableList().apply {
                set(existingIndex, goal)
            }
        } else {
            current + goal
        }

        Result.success(Unit)
    }

    override suspend fun setGoalArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = mutex.withLock {

        val current = goals.value

        if (current.none { it.id == id }) {
            return@withLock Result.failure(
                NoSuchElementException("Goal not found")
            )
        }

        goals.value = current.map { goal ->
            if (goal.id == id) {
                goal.copy(archived = archived)
            } else {
                goal
            }
        }

        Result.success(Unit)
    }

    override suspend fun addContribution(
        contribution: Contribution
    ): Result<Unit> = mutex.withLock {

        if (contribution.id.isBlank()) {
            return@withLock Result.failure(
                IllegalArgumentException("Contribution ID cannot be blank")
            )
        }

        if (goals.value.none { it.id == contribution.goalId }) {
            return@withLock Result.failure(
                NoSuchElementException("Goal not found")
            )
        }

        val current = contributions.value
        val existing = current.find { it.id == contribution.id }

        if (existing != null) {
            return@withLock if (existing == contribution) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalArgumentException("Contribution ID already exists")
                )
            }
        }

        if (contribution.kind == ContributionKind.REVERSAL) {
            val original = current.find {
                it.id == contribution.reversesId &&
                        it.goalId == contribution.goalId &&
                        it.kind != ContributionKind.REVERSAL
            }

            if (
                original == null ||
                contribution.amountMinor != -original.amountMinor ||
                current.any {
                    it.kind == ContributionKind.REVERSAL &&
                            it.reversesId == original.id
                }
            ) {
                return@withLock Result.failure(
                    IllegalArgumentException("Invalid or duplicate reversal")
                )
            }
        } else {
            if (
                contribution.amountMinor <= 0L ||
                contribution.reversesId != null
            ) {
                return@withLock Result.failure(
                    IllegalArgumentException("Invalid contribution amount")
                )
            }
        }

        contributions.value = current + contribution

        Result.success(Unit)
    }
}
