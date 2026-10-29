
package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.UserId
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeGoals(): Flow<List<Goal>>

    fun observeContributions(
        goalId: DocumentId
    ): Flow<List<Contribution>>

    suspend fun saveGoal(
        goal: Goal
    ): Result<Unit>

    suspend fun saveGoalForOwner(
        expectedOwnerId: UserId,
        goal: Goal
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Owner-bound goal writes are not supported")
    )

    suspend fun setGoalArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>

    suspend fun setGoalArchivedForOwner(
        expectedOwnerId: UserId,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Owner-bound goal writes are not supported")
    )

    suspend fun addContribution(
        contribution: Contribution
    ): Result<Unit>

    suspend fun addContributionForOwner(
        expectedOwnerId: UserId,
        contribution: Contribution
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Owner-bound contribution writes are not supported")
    )
}
