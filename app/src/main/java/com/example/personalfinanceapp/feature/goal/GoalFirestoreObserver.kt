package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GoalFirestoreObserver internal constructor(
    private val authRepository: AuthRepository,
    private val snapshotSource: GoalFirestoreSnapshotSource
) {

    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ) : this(authRepository, FirebaseGoalFirestoreSnapshotSource(firestore))

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeGoals(): Flow<List<Goal>> = observeIdentityChanges()
        .flatMapLatest { identity ->
            val uid = identity.uid
            when {
                uid == null -> flowOf(emptyList())
                identity.mustClearPreviousUser -> flow {
                    emit(emptyList())
                    emitAll(observeGoalsForOwner(uid))
                }
                else -> observeGoalsForOwner(uid)
            }
        }
        .buffer(capacity = 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeContributions(goalId: String): Flow<List<Contribution>> {
        GoalFirestoreMapper.validateContributionGoalId(goalId)
        return observeIdentityChanges()
            .flatMapLatest { identity ->
                val uid = identity.uid
                when {
                    uid == null -> flowOf(emptyList())
                    identity.mustClearPreviousUser -> flow {
                        emit(emptyList())
                        emitAll(observeContributionsForOwner(uid, goalId))
                    }
                    else -> observeContributionsForOwner(uid, goalId)
                }
            }
            .buffer(capacity = 0)
    }

    private fun observeIdentityChanges(): Flow<GoalIdentityChange> = flow {
        var lastAuthenticatedUid: UserId? = null
        var signedOutSinceAuthenticated = false
        var currentUid: UserId? = null

        authRepository.observeAuthState()
            .distinctUntilChanged()
            .collect { uid ->
                val mustClearPreviousUser = uid != null &&
                    lastAuthenticatedUid != null &&
                    (uid != lastAuthenticatedUid || signedOutSinceAuthenticated)

                if (uid == null) {
                    if (lastAuthenticatedUid != null) signedOutSinceAuthenticated = true
                } else {
                    lastAuthenticatedUid = uid
                    signedOutSinceAuthenticated = false
                }
                currentUid = uid
                emit(GoalIdentityChange(uid, mustClearPreviousUser))
            }

        if (currentUid != null) {
            emit(GoalIdentityChange(uid = null, mustClearPreviousUser = false))
        }
    }

    private fun observeGoalsForOwner(uid: UserId): Flow<List<Goal>> = callbackFlow {
        val registration = snapshotSource.listenGoals(uid) snapshot@{ result ->
            val documents = result.getOrElse { exception ->
                close(exception)
                return@snapshot
            }
            val goals = try {
                documents.map { document ->
                    GoalFirestoreMapper.goalFromFirestoreMap(document.id, document.fields)
                }
            } catch (exception: Exception) {
                close(exception)
                return@snapshot
            }
            trySend(goals)
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private fun observeContributionsForOwner(
        uid: UserId,
        goalId: String
    ): Flow<List<Contribution>> = callbackFlow {
        val registration = snapshotSource.listenContributions(uid, goalId) snapshot@{ result ->
            val documents = result.getOrElse { exception ->
                close(exception)
                return@snapshot
            }
            val contributions = try {
                documents.map { document ->
                    GoalFirestoreMapper.contributionFromFirestoreMap(
                        documentId = document.id,
                        expectedGoalId = goalId,
                        fields = document.fields
                    )
                }
            } catch (exception: Exception) {
                close(exception)
                return@snapshot
            }
            trySend(contributions)
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()
}

private data class GoalIdentityChange(
    val uid: UserId?,
    val mustClearPreviousUser: Boolean
)

internal data class GoalFirestoreDocument(
    val id: String,
    val fields: Map<String, Any?>
)

internal fun interface GoalFirestoreListenerRegistration {
    fun remove()
}

internal interface GoalFirestoreSnapshotSource {
    fun listenGoals(
        authenticatedUid: UserId,
        onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
    ): GoalFirestoreListenerRegistration

    fun listenContributions(
        authenticatedUid: UserId,
        goalId: String,
        onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
    ): GoalFirestoreListenerRegistration
}

private class FirebaseGoalFirestoreSnapshotSource(
    private val firestore: FirebaseFirestore
) : GoalFirestoreSnapshotSource {

    override fun listenGoals(
        authenticatedUid: UserId,
        onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
    ): GoalFirestoreListenerRegistration {
        val query = firestore.collection(USERS_COLLECTION)
            .document(authenticatedUid)
            .collection(GOALS_COLLECTION)
            .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
        return query.listenAsGoalDocuments(onSnapshot)
    }

    override fun listenContributions(
        authenticatedUid: UserId,
        goalId: String,
        onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
    ): GoalFirestoreListenerRegistration {
        val query = firestore.collection(USERS_COLLECTION)
            .document(authenticatedUid)
            .collection(GOALS_COLLECTION)
            .document(goalId)
            .collection(CONTRIBUTIONS_COLLECTION)
            .orderBy(OCCURRED_DATE_FIELD, Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
        return query.listenAsGoalDocuments(onSnapshot)
    }

    private fun Query.listenAsGoalDocuments(
        onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
    ): GoalFirestoreListenerRegistration {
        val listener = addSnapshotListener { snapshot, exception ->
            when {
                exception != null -> onSnapshot(Result.failure(exception))
                snapshot == null -> onSnapshot(
                    Result.failure(IllegalStateException("Goal listener returned no snapshot"))
                )
                else -> onSnapshot(
                    Result.success(
                        snapshot.documents.map { document ->
                            GoalFirestoreDocument(
                                id = document.id,
                                fields = document.data ?: emptyMap()
                            )
                        }
                    )
                )
            }
        }
        return GoalFirestoreListenerRegistration { listener.remove() }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val GOALS_COLLECTION = "goals"
        const val CONTRIBUTIONS_COLLECTION = "contributions"
        const val OCCURRED_DATE_FIELD = "occurredDate"
    }
}
