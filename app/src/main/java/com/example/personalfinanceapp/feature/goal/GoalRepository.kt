
package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    fun observeGoals(): Flow<List<Goal>>

    fun observeContributions(
        goalId: DocumentId
    ): Flow<List<Contribution>>

    suspend fun saveGoal(
        goal: Goal
    ): Result<Unit>

    suspend fun setGoalArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit>

    suspend fun addContribution(
        contribution: Contribution
    ): Result<Unit>
}
