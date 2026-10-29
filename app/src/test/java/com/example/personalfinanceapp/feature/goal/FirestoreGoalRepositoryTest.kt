package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreGoalRepositoryTest {

    @Test
    fun goalsAndContributionsPersistAfterRepositoryRecreation() = runTest {
        val auth = TestAuthRepository("user-a")
        val store = InMemoryGoalStore()
        val firstRepository = repository(auth, store)
        val goal = goal("goal-1", "Emergency fund")
        val contribution = contribution("contribution-1", goal.id, 125_050L)

        assertTrue(firstRepository.saveGoalForOwner("user-a", goal).isSuccess)
        assertTrue(
            firstRepository.addContributionForOwner("user-a", contribution).isSuccess
        )

        val recreatedRepository = repository(auth, store)
        assertEquals(listOf(goal), recreatedRepository.observeGoals().first())
        assertEquals(
            listOf(contribution),
            recreatedRepository.observeContributions(goal.id).first()
        )
    }

    @Test
    fun accountSwitchClearsOldDataCancelsListenerAndRejectsStaleCallback() = runTest {
        val auth = TestAuthRepository("user-a")
        val store = InMemoryGoalStore().apply {
            seedGoal("user-a", goal("same-id", "User A goal"))
            seedGoal("user-b", goal("same-id", "User B goal"))
        }
        val repository = repository(auth, store)
        val emissions = mutableListOf<List<Goal>>()
        val job = backgroundScope.launch { repository.observeGoals().collect(emissions::add) }
        runCurrent()

        val oldSubscription = store.goalSubscriptions.single()
        assertEquals("User A goal", emissions.last().single().name)

        auth.setUid("user-b")
        runCurrent()

        assertTrue(oldSubscription.removed)
        assertEquals("User B goal", emissions.last().single().name)
        val emissionCount = emissions.size
        oldSubscription.callback(
            Result.success(listOf(store.goalDocument(goal("same-id", "Stale A goal"))))
        )
        runCurrent()
        assertEquals(emissionCount, emissions.size)
        job.cancel()
    }

    @Test
    fun crossAccountAndPendingStaleWritesAreRejected() = runTest {
        val auth = TestAuthRepository("user-b")
        val store = InMemoryGoalStore()
        val repository = repository(auth, store)

        val directResult = repository.saveGoalForOwner(
            expectedOwnerId = "user-a",
            goal = goal("goal-1", "Wrong owner")
        )
        assertTrue(directResult.isFailure)
        assertTrue(store.goals.isEmpty())

        auth.setUid("user-a")
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        store.beforeWrite = {
            writeStarted.complete(Unit)
            releaseWrite.await()
        }
        val pending = async {
            repository.saveGoalForOwner("user-a", goal("goal-2", "Pending"))
        }
        writeStarted.await()
        auth.setUid("user-b")
        releaseWrite.complete(Unit)

        assertTrue(pending.await().isFailure)
        assertFalse(store.goals.containsKey("user-a" to "goal-2"))
        assertFalse(store.goals.containsKey("user-b" to "goal-2"))
    }

    @Test
    fun identicalGoalAndContributionIdsRemainIsolatedAcrossAccounts() = runTest {
        val auth = TestAuthRepository("user-a")
        val store = InMemoryGoalStore()
        val repository = repository(auth, store)
        val userAGoal = goal("same-goal", "A goal")
        val userAContribution = contribution("same-entry", userAGoal.id, 100L)
        assertTrue(repository.saveGoal(userAGoal).isSuccess)
        assertTrue(repository.addContribution(userAContribution).isSuccess)

        auth.setUid("user-b")
        val userBGoal = goal("same-goal", "B goal")
        val userBContribution = contribution("same-entry", userBGoal.id, 250L)
        assertTrue(repository.saveGoal(userBGoal).isSuccess)
        assertTrue(repository.addContribution(userBContribution).isSuccess)

        assertEquals("A goal", store.goalFor("user-a", "same-goal")?.name)
        assertEquals("B goal", store.goalFor("user-b", "same-goal")?.name)
        assertEquals(100L, store.contributionFor("user-a", "same-goal", "same-entry")?.amountMinor)
        assertEquals(250L, store.contributionFor("user-b", "same-goal", "same-entry")?.amountMinor)
    }

    @Test
    fun emptyOfflineAndObservationErrorsRemainDistinct() = runTest {
        val auth = TestAuthRepository("user-a")
        val store = InMemoryGoalStore()
        val repository = repository(auth, store)

        assertTrue(repository.observeGoals().first().isEmpty())

        val observationFailure = IllegalStateException("Permission denied")
        store.listenFailure = observationFailure
        val observedFailure = runCatching { repository.observeGoals().first() }.exceptionOrNull()
        assertTrue(observedFailure is IllegalStateException)
        assertEquals(observationFailure.message, observedFailure?.message)

        store.listenFailure = null
        val offlineFailure = IllegalStateException("Offline")
        store.writeFailure = offlineFailure
        val writeResult = repository.saveGoal(goal("goal-offline", "Offline goal"))
        assertSame(offlineFailure, writeResult.exceptionOrNull())
        assertNull(store.goalFor("user-a", "goal-offline"))
    }

    @Test
    fun contributionRulesArePreservedIncludingSingleExactReversal() = runTest {
        val auth = TestAuthRepository("user-a")
        val store = InMemoryGoalStore()
        val repository = repository(auth, store)
        val goal = goal("goal-1", "Emergency")
        val original = contribution("original", goal.id, 500L)
        val reversal = Contribution(
            id = "reversal",
            goalId = goal.id,
            amountMinor = -500L,
            kind = ContributionKind.REVERSAL,
            occurredDate = LocalDate.of(2026, 10, 22),
            reversesId = original.id
        )
        assertTrue(repository.saveGoal(goal).isSuccess)
        assertTrue(repository.addContribution(original).isSuccess)
        assertTrue(repository.addContribution(reversal).isSuccess)
        assertTrue(repository.addContribution(reversal).isSuccess)

        val duplicate = reversal.copy(id = "second-reversal")
        assertTrue(repository.addContribution(duplicate).isFailure)
        val stored = repository.observeContributions(goal.id).first()
        assertEquals(0L, stored.fold(0L) { sum, item -> Math.addExact(sum, item.amountMinor) })
    }

    private fun repository(
        auth: TestAuthRepository,
        store: InMemoryGoalStore
    ) = FirestoreGoalRepository(
        authRepository = auth,
        observer = GoalFirestoreObserver(auth, store),
        writeGateway = store
    )

    private fun goal(id: String, name: String) = Goal(
        id = id,
        name = name,
        targetAmountMinor = 1_000_000L,
        startDate = LocalDate.of(2026, 10, 1),
        targetDate = LocalDate.of(2027, 10, 1)
    )

    private fun contribution(id: String, goalId: String, amountMinor: Long) = Contribution(
        id = id,
        goalId = goalId,
        amountMinor = amountMinor,
        kind = ContributionKind.ALLOCATION,
        occurredDate = LocalDate.of(2026, 10, 21)
    )

    private class TestAuthRepository(initialUid: UserId?) : AuthRepository {
        private val state = MutableStateFlow(initialUid)
        override fun observeAuthState(): Flow<UserId?> = state
        override fun currentUserId(): UserId? = state.value
        fun setUid(uid: UserId?) { state.value = uid }
        override suspend fun login(email: String, password: String) = unsupportedUser()
        override suspend fun register(email: String, password: String) = unsupportedUser()
        override suspend fun sendPasswordReset(email: String) = unsupportedUnit()
        override suspend fun signOut() = unsupportedUnit()
        private fun unsupportedUser(): Result<UserId> = Result.failure(UnsupportedOperationException())
        private fun unsupportedUnit(): Result<Unit> = Result.failure(UnsupportedOperationException())
    }

    private class InMemoryGoalStore : GoalFirestoreSnapshotSource, GoalFirestoreWriteGateway {
        val goals = mutableMapOf<Pair<UserId, String>, Map<String, Any>>()
        val contributions = mutableMapOf<Triple<UserId, String, String>, Map<String, Any>>()
        private val reversalClaims = mutableSetOf<Triple<UserId, String, String>>()
        val goalSubscriptions = mutableListOf<GoalSubscription>()
        private val contributionSubscriptions = mutableListOf<ContributionSubscription>()
        var beforeWrite: suspend () -> Unit = {}
        var writeFailure: Throwable? = null
        var listenFailure: Throwable? = null

        override fun listenGoals(
            authenticatedUid: UserId,
            onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
        ): GoalFirestoreListenerRegistration {
            val subscription = GoalSubscription(authenticatedUid, onSnapshot)
            goalSubscriptions += subscription
            listenFailure?.let {
                onSnapshot(Result.failure(it))
            } ?: onSnapshot(Result.success(goalDocuments(authenticatedUid)))
            return GoalFirestoreListenerRegistration { subscription.removed = true }
        }

        override fun listenContributions(
            authenticatedUid: UserId,
            goalId: String,
            onSnapshot: (Result<List<GoalFirestoreDocument>>) -> Unit
        ): GoalFirestoreListenerRegistration {
            val subscription = ContributionSubscription(authenticatedUid, goalId, onSnapshot)
            contributionSubscriptions += subscription
            listenFailure?.let {
                onSnapshot(Result.failure(it))
            } ?: onSnapshot(Result.success(contributionDocuments(authenticatedUid, goalId)))
            return GoalFirestoreListenerRegistration { subscription.removed = true }
        }

        override suspend fun setGoal(
            authenticatedUid: UserId,
            goalId: String,
            fields: Map<String, Any>,
            ownerSessionIsCurrent: () -> Boolean
        ) {
            beforeWrite()
            check(ownerSessionIsCurrent()) { "Stale owner session" }
            writeFailure?.let { throw it }
            goals[authenticatedUid to goalId] = fields.toMap()
            notifyGoals(authenticatedUid)
        }

        override suspend fun setGoalArchived(
            authenticatedUid: UserId,
            goalId: String,
            archived: Boolean,
            ownerSessionIsCurrent: () -> Boolean
        ) {
            beforeWrite()
            check(ownerSessionIsCurrent()) { "Stale owner session" }
            writeFailure?.let { throw it }
            val key = authenticatedUid to goalId
            val existing = goals[key] ?: throw NoSuchElementException("Goal not found")
            goals[key] = existing.toMutableMap().apply { put("archived", archived) }
            notifyGoals(authenticatedUid)
        }

        override suspend fun addContribution(
            authenticatedUid: UserId,
            contribution: Contribution,
            fields: Map<String, Any>,
            ownerSessionIsCurrent: () -> Boolean
        ) {
            beforeWrite()
            check(ownerSessionIsCurrent()) { "Stale owner session" }
            writeFailure?.let { throw it }
            check(goals.containsKey(authenticatedUid to contribution.goalId)) { "Goal not found" }
            val key = Triple(authenticatedUid, contribution.goalId, contribution.id)
            contributions[key]?.let { existing ->
                if (existing == fields) return
                throw IllegalArgumentException("Contribution ID already exists")
            }
            if (contribution.kind == ContributionKind.REVERSAL) {
                val originalId = requireNotNull(contribution.reversesId)
                val original = contributionFor(authenticatedUid, contribution.goalId, originalId)
                    ?: throw IllegalArgumentException("Original contribution not found")
                require(original.kind != ContributionKind.REVERSAL)
                require(contribution.amountMinor == Math.negateExact(original.amountMinor))
                val claim = Triple(authenticatedUid, contribution.goalId, originalId)
                require(reversalClaims.add(claim)) { "Contribution has already been reversed" }
            }
            contributions[key] = fields.toMap()
            notifyContributions(authenticatedUid, contribution.goalId)
        }

        fun seedGoal(uid: UserId, goal: Goal) {
            goals[uid to goal.id] = GoalFirestoreMapper.toFirestoreMap(goal)
        }

        fun goalFor(uid: UserId, id: String): Goal? = goals[uid to id]?.let {
            GoalFirestoreMapper.goalFromFirestoreMap(id, it)
        }

        fun contributionFor(uid: UserId, goalId: String, id: String): Contribution? =
            contributions[Triple(uid, goalId, id)]?.let {
                GoalFirestoreMapper.contributionFromFirestoreMap(id, goalId, it)
            }

        fun goalDocument(goal: Goal) = GoalFirestoreDocument(
            goal.id,
            GoalFirestoreMapper.toFirestoreMap(goal)
        )

        private fun goalDocuments(uid: UserId) = goals
            .filterKeys { it.first == uid }
            .toSortedMap(compareBy { it.second })
            .map { (key, fields) -> GoalFirestoreDocument(key.second, fields) }

        private fun contributionDocuments(uid: UserId, goalId: String) = contributions
            .filterKeys { it.first == uid && it.second == goalId }
            .values
            .map { fields ->
                GoalFirestoreDocument(fields.getValue("id") as String, fields)
            }
            .sortedWith(compareBy({ it.fields.getValue("occurredDate") as String }, { it.id }))

        private fun notifyGoals(uid: UserId) {
            val documents = goalDocuments(uid)
            goalSubscriptions.filter { it.uid == uid && !it.removed }
                .forEach { it.callback(Result.success(documents)) }
        }

        private fun notifyContributions(uid: UserId, goalId: String) {
            val documents = contributionDocuments(uid, goalId)
            contributionSubscriptions
                .filter { it.uid == uid && it.goalId == goalId && !it.removed }
                .forEach { it.callback(Result.success(documents)) }
        }

        data class GoalSubscription(
            val uid: UserId,
            val callback: (Result<List<GoalFirestoreDocument>>) -> Unit,
            var removed: Boolean = false
        )

        private data class ContributionSubscription(
            val uid: UserId,
            val goalId: String,
            val callback: (Result<List<GoalFirestoreDocument>>) -> Unit,
            var removed: Boolean = false
        )
    }
}
