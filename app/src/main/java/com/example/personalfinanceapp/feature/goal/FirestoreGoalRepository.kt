package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

class FirestoreGoalRepository internal constructor(
    private val authRepository: AuthRepository,
    private val observer: GoalFirestoreObserver,
    private val writeGateway: GoalFirestoreWriteGateway
) : GoalRepository {

    @Inject
    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ) : this(
        authRepository = authRepository,
        observer = GoalFirestoreObserver(authRepository, firestore),
        writeGateway = FirebaseGoalFirestoreWriteGateway(firestore)
    )

    override fun observeGoals(): Flow<List<Goal>> = observer.observeGoals()

    override fun observeContributions(goalId: DocumentId): Flow<List<Contribution>> =
        observer.observeContributions(goalId)

    override suspend fun saveGoal(goal: Goal): Result<Unit> =
        saveGoalInternal(expectedOwnerId = null, goal = goal)

    override suspend fun saveGoalForOwner(
        expectedOwnerId: UserId,
        goal: Goal
    ): Result<Unit> = saveGoalInternal(expectedOwnerId, goal)

    private suspend fun saveGoalInternal(
        expectedOwnerId: UserId?,
        goal: Goal
    ): Result<Unit> = repositoryResult {
        requireExpectedOwner(expectedOwnerId)
        GoalFirestoreMapper.validateGoal(goal)
        val uid = requireAuthenticatedUid(expectedOwnerId)
        ensureOwnerIsStillAuthenticated(uid)
        writeGateway.setGoal(
            uid,
            goal.id,
            GoalFirestoreMapper.toFirestoreMap(goal),
            ownerSessionIsCurrent(uid)
        )
    }

    override suspend fun setGoalArchived(
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = setGoalArchivedInternal(null, id, archived)

    override suspend fun setGoalArchivedForOwner(
        expectedOwnerId: UserId,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = setGoalArchivedInternal(expectedOwnerId, id, archived)

    private suspend fun setGoalArchivedInternal(
        expectedOwnerId: UserId?,
        id: DocumentId,
        archived: Boolean
    ): Result<Unit> = repositoryResult {
        requireExpectedOwner(expectedOwnerId)
        GoalFirestoreMapper.validateContributionGoalId(id)
        val uid = requireAuthenticatedUid(expectedOwnerId)
        ensureOwnerIsStillAuthenticated(uid)
        writeGateway.setGoalArchived(uid, id, archived, ownerSessionIsCurrent(uid))
    }

    override suspend fun addContribution(contribution: Contribution): Result<Unit> =
        addContributionInternal(null, contribution)

    override suspend fun addContributionForOwner(
        expectedOwnerId: UserId,
        contribution: Contribution
    ): Result<Unit> = addContributionInternal(expectedOwnerId, contribution)

    private suspend fun addContributionInternal(
        expectedOwnerId: UserId?,
        contribution: Contribution
    ): Result<Unit> = repositoryResult {
        requireExpectedOwner(expectedOwnerId)
        GoalFirestoreMapper.validateContributionShape(contribution)
        val uid = requireAuthenticatedUid(expectedOwnerId)
        ensureOwnerIsStillAuthenticated(uid)
        writeGateway.addContribution(
            authenticatedUid = uid,
            contribution = contribution,
            fields = GoalFirestoreMapper.contributionToFirestoreMap(contribution),
            ownerSessionIsCurrent = ownerSessionIsCurrent(uid)
        )
    }

    private fun requireExpectedOwner(expectedOwnerId: UserId?) {
        if (expectedOwnerId != null) {
            require(expectedOwnerId.isNotBlank()) { "Expected goal owner cannot be empty" }
        }
    }

    private suspend fun requireAuthenticatedUid(expectedOwnerId: UserId?): UserId {
        val uid = authRepository.observeAuthState().first()
        check(!uid.isNullOrBlank()) { "User is not authenticated" }
        if (expectedOwnerId != null) {
            check(uid == expectedOwnerId) {
                "Authenticated user changed before goal data could be written"
            }
        }
        return uid
    }

    private suspend fun ensureOwnerIsStillAuthenticated(ownerId: UserId) {
        check(authRepository.observeAuthState().first() == ownerId) {
            "Authenticated user changed before goal data could be written"
        }
    }

    private fun ownerSessionIsCurrent(ownerId: UserId): () -> Boolean = {
        authRepository.currentUserId() == ownerId
    }

    private suspend inline fun repositoryResult(
        crossinline operation: suspend () -> Unit
    ): Result<Unit> = try {
        operation()
        Result.success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

internal interface GoalFirestoreWriteGateway {
    suspend fun setGoal(
        authenticatedUid: UserId,
        goalId: DocumentId,
        fields: Map<String, Any>,
        ownerSessionIsCurrent: () -> Boolean
    )

    suspend fun setGoalArchived(
        authenticatedUid: UserId,
        goalId: DocumentId,
        archived: Boolean,
        ownerSessionIsCurrent: () -> Boolean
    )

    suspend fun addContribution(
        authenticatedUid: UserId,
        contribution: Contribution,
        fields: Map<String, Any>,
        ownerSessionIsCurrent: () -> Boolean
    )
}

private class FirebaseGoalFirestoreWriteGateway(
    private val firestore: FirebaseFirestore
) : GoalFirestoreWriteGateway {

    override suspend fun setGoal(
        authenticatedUid: UserId,
        goalId: DocumentId,
        fields: Map<String, Any>,
        ownerSessionIsCurrent: () -> Boolean
    ) {
        check(ownerSessionIsCurrent()) {
            "Authenticated user changed before goal could be saved"
        }
        firestore.goalDocument(authenticatedUid, goalId)
            .set(fields)
            .awaitGoalCompletion()
    }

    override suspend fun setGoalArchived(
        authenticatedUid: UserId,
        goalId: DocumentId,
        archived: Boolean,
        ownerSessionIsCurrent: () -> Boolean
    ) {
        check(ownerSessionIsCurrent()) {
            "Authenticated user changed before goal could be archived"
        }
        val goalDocument = firestore.goalDocument(authenticatedUid, goalId)
        firestore.runTransaction { transaction ->
            check(ownerSessionIsCurrent()) {
                "Authenticated user changed before goal could be archived"
            }
            if (!transaction.get(goalDocument).exists()) {
                throw NoSuchElementException("Goal not found")
            }
            transaction.update(goalDocument, "archived", archived)
            Unit
        }.awaitGoalCompletion()
    }

    override suspend fun addContribution(
        authenticatedUid: UserId,
        contribution: Contribution,
        fields: Map<String, Any>,
        ownerSessionIsCurrent: () -> Boolean
    ) {
        check(ownerSessionIsCurrent()) {
            "Authenticated user changed before contribution could be saved"
        }
        val goalDocument = firestore.goalDocument(authenticatedUid, contribution.goalId)
        val contributionDocument = goalDocument.collection(CONTRIBUTIONS_COLLECTION)
            .document(contribution.id)

        firestore.runTransaction { transaction ->
            check(ownerSessionIsCurrent()) {
                "Authenticated user changed before contribution could be saved"
            }
            if (!transaction.get(goalDocument).exists()) {
                throw NoSuchElementException("Goal not found")
            }

            val existing = transaction.get(contributionDocument)
            if (existing.exists()) {
                val stored = GoalFirestoreMapper.contributionFromFirestoreMap(
                    documentId = existing.id,
                    expectedGoalId = contribution.goalId,
                    fields = existing.data ?: emptyMap()
                )
                if (stored != contribution) {
                    throw IllegalArgumentException("Contribution ID already exists")
                }
                return@runTransaction Unit
            }

            if (contribution.kind == ContributionKind.REVERSAL) {
                val originalId = requireNotNull(contribution.reversesId)
                val originalDocument = goalDocument.collection(CONTRIBUTIONS_COLLECTION)
                    .document(originalId)
                val originalSnapshot = transaction.get(originalDocument)
                if (!originalSnapshot.exists()) {
                    throw IllegalArgumentException("Reversed contribution does not exist")
                }
                val original = GoalFirestoreMapper.contributionFromFirestoreMap(
                    documentId = originalSnapshot.id,
                    expectedGoalId = contribution.goalId,
                    fields = originalSnapshot.data ?: emptyMap()
                )
                val expectedAmount = try {
                    Math.negateExact(original.amountMinor)
                } catch (exception: ArithmeticException) {
                    throw IllegalArgumentException("Original contribution cannot be reversed", exception)
                }
                if (original.kind == ContributionKind.REVERSAL ||
                    contribution.amountMinor != expectedAmount
                ) {
                    throw IllegalArgumentException("Invalid reversal contribution")
                }

                val claimDocument = goalDocument.collection(REVERSAL_CLAIMS_COLLECTION)
                    .document(original.id)
                if (transaction.get(claimDocument).exists()) {
                    throw IllegalArgumentException("Contribution has already been reversed")
                }
                transaction.set(
                    claimDocument,
                    mapOf(
                        "schemaVersion" to 1L,
                        "originalContributionId" to original.id,
                        "reversalContributionId" to contribution.id
                    )
                )
            }

            transaction.set(contributionDocument, fields)
            Unit
        }.awaitGoalCompletion()
    }
}

private fun FirebaseFirestore.goalDocument(uid: UserId, goalId: DocumentId) =
    collection(USERS_COLLECTION)
        .document(uid)
        .collection(GOALS_COLLECTION)
        .document(goalId)

private suspend fun Task<*>.awaitGoalCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { completedTask ->
            when {
                completedTask.isCanceled -> continuation.cancel(
                    CancellationException("Firebase task was cancelled")
                )
                completedTask.isSuccessful -> continuation.resume(Unit) { _, _, _ -> }
                else -> continuation.resumeWith(
                    Result.failure(
                        completedTask.exception
                            ?: IllegalStateException("Firebase task failed without an exception")
                    )
                )
            }
        }
    }
}

private const val USERS_COLLECTION = "users"
private const val GOALS_COLLECTION = "goals"
private const val CONTRIBUTIONS_COLLECTION = "contributions"
private const val REVERSAL_CLAIMS_COLLECTION = "reversalClaims"
