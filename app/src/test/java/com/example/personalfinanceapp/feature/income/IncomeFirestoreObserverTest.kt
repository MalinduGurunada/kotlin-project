package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeFirestoreObserverTest {

    private val startDate = LocalDate.of(2026, 10, 1)
    private val endDateExclusive = LocalDate.of(2026, 11, 1)

    private fun lkrIncome(
        id: String,
        date: LocalDate = LocalDate.of(2026, 10, 21),
        note: String? = null
    ): Income {
        return Income(
            id = id,
            source = IncomeSource.SALARY,
            entryKind = IncomeEntryKind.RECEIPT,
            money = MoneyConverter.fromLkr(id.length.toString(), date).getOrThrow(),
            occurredDate = date,
            note = note
        )
    }

    private fun usdIncome(): Income {
        val occurredDate = LocalDate.of(2026, 10, 21)
        return Income(
            id = "income-usd",
            source = IncomeSource.FREELANCE,
            entryKind = IncomeEntryKind.RECEIPT,
            money = MoneyConverter.fromUsd(
                amountInput = "125.00",
                rateLkrPerUsdInput = "310.123456",
                rateSource = RateSource.MANUAL_TRANSFER,
                rateDate = LocalDate.of(2026, 10, 18)
            ).getOrThrow(),
            occurredDate = occurredDate,
            projectReference = "historical-project",
            note = "Historical transfer rate"
        )
    }

    private fun document(income: Income): IncomeFirestoreDocument {
        return IncomeFirestoreDocument(
            id = income.id,
            fields = IncomeFirestoreMapper.toFirestoreMap(income)
        )
    }

    private fun observer(
        authRepository: AuthRepository,
        source: TestIncomeFirestoreSnapshotSource
    ): IncomeFirestoreObserver {
        return IncomeFirestoreObserver(authRepository, source)
    }

    @Test
    fun rejectsInvalidDateRangeBeforeObservingAuthentication() {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val observer = observer(authRepository, source)

        listOf(startDate, startDate.minusDays(1)).forEach { invalidEnd ->
            assertThrows(IllegalArgumentException::class.java) {
                observer.observeIncomes(startDate, invalidEnd)
            }
        }

        assertEquals(0, authRepository.observerCount)
        assertTrue(source.subscriptions.isEmpty())
    }

    @Test
    fun signedOutStateEmitsEmptyWithoutRegisteringFirestoreListener() = runTest {
        val authRepository = TestAuthRepository(null)
        val source = TestIncomeFirestoreSnapshotSource()

        val incomes = observer(authRepository, source)
            .observeIncomes(startDate, endDateExclusive)
            .first()

        assertTrue(incomes.isEmpty())
        assertTrue(source.subscriptions.isEmpty())
    }

    @Test
    fun initialAuthenticatedObservationWaitsForRealSnapshot() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val firstSnapshot = async(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .first()
        }

        runCurrent()
        assertFalse(firstSnapshot.isCompleted)

        val subscription = source.singleSubscription("user-a")
        source.emitSuccess(subscription, emptyList())

        assertEquals(emptyList<Income>(), firstSnapshot.await())
        runCurrent()
        assertTrue(subscription.removed)
    }

    @Test
    fun ownerBoundSnapshotAssociatesRecordsWithTheProducingAuthenticationBranch() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val firstSnapshot = async(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomeSnapshots(startDate, endDateExclusive)
                .first()
        }

        runCurrent()
        val income = lkrIncome("income-a")
        source.emitSuccess(
            source.singleSubscription("user-a"),
            listOf(document(income))
        )

        val snapshot = firstSnapshot.await()
        assertEquals("user-a", snapshot.ownerId)
        assertEquals(listOf(income), snapshot.incomes)
    }

    @Test
    fun ownerBoundSnapshotClearsBeforePublishingNewAccountsRecords() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = mutableListOf<IncomeObservationSnapshot>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomeSnapshots(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val incomeA = lkrIncome("shared-id", note = "user-a")
        source.emitSuccess(
            source.singleSubscription("user-a"),
            listOf(document(incomeA))
        )
        runCurrent()

        authRepository.emit("user-b")
        runCurrent()
        val incomeB = lkrIncome("shared-id", note = "user-b")
        source.emitSuccess(
            source.singleSubscription("user-b"),
            listOf(document(incomeB))
        )
        runCurrent()

        assertEquals(listOf("user-a", null, "user-b"), emissions.map { it.ownerId })
        assertEquals(
            listOf(listOf(incomeA), emptyList(), listOf(incomeB)),
            emissions.map { it.incomes }
        )
        collection.cancel()
    }

    @Test
    fun authenticatedUidBuildsExpectedQueryAndDuplicateUidDoesNotRestart() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = mutableListOf<List<Income>>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        assertEquals("2026-10-01", subscription.query.startDateInclusiveIso)
        assertEquals("2026-11-01", subscription.query.endDateExclusiveIso)
        assertEquals(
            listOf(
                IncomeFirestoreOrder.OCCURRED_DATE_ASCENDING,
                IncomeFirestoreOrder.DOCUMENT_ID_ASCENDING
            ),
            subscription.query.ordering
        )

        val income = lkrIncome("income-a")
        source.emitSuccess(subscription, listOf(document(income)))
        runCurrent()

        authRepository.emit("user-a")
        runCurrent()
        assertEquals(1, source.subscriptions.size)
        assertEquals(listOf(listOf(income)), emissions)

        collection.cancel()
        runCurrent()
        assertTrue(subscription.removed)
    }

    @Test
    fun accountSwitchClearsOldDataRemovesListenerAndRejectsLateCallbacks() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = mutableListOf<List<Income>>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val userA = source.singleSubscription("user-a")
        val incomeA = lkrIncome("income-a")
        source.emitSuccess(userA, listOf(document(incomeA)))
        runCurrent()
        assertEquals(listOf(listOf(incomeA)), emissions)

        authRepository.emit("user-b")
        runCurrent()
        val userB = source.singleSubscription("user-b")
        assertTrue(userA.removed)
        assertEquals(listOf(listOf(incomeA), emptyList()), emissions)

        source.emitSuccess(userA, listOf(document(lkrIncome("late-user-a"))))
        runCurrent()
        assertEquals(listOf(listOf(incomeA), emptyList()), emissions)

        val incomeB = lkrIncome("income-b")
        source.emitSuccess(userB, listOf(document(incomeB)))
        runCurrent()
        assertEquals(listOf(listOf(incomeA), emptyList(), listOf(incomeB)), emissions)

        collection.cancel()
    }

    @Test
    fun slowCollectorDropsQueuedSnapshotsWhenAuthenticatedUidChanges() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val firstIncomeA = lkrIncome("income-a-first")
        val secondIncomeA = lkrIncome("income-a-second")
        val thirdIncomeA = lkrIncome("income-a-third")
        val incomeB = lkrIncome("income-b")
        val emissions = mutableListOf<List<Income>>()
        val collectorBlocked = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val collection = backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect { incomes ->
                    emissions += incomes
                    if (incomes == listOf(firstIncomeA)) {
                        collectorBlocked.complete(Unit)
                        releaseCollector.await()
                    }
                }
        }

        runCurrent()
        val userA = source.singleSubscription("user-a")
        source.emitSuccess(userA, listOf(document(firstIncomeA)))
        runCurrent()
        assertTrue(collectorBlocked.isCompleted)

        source.emitSuccess(userA, listOf(document(secondIncomeA)))
        source.emitSuccess(userA, listOf(document(thirdIncomeA)))
        authRepository.emit("user-b")
        runCurrent()

        assertTrue(userA.removed)
        assertEquals(listOf(listOf(firstIncomeA)), emissions)

        releaseCollector.complete(Unit)
        runCurrent()
        val userB = source.singleSubscription("user-b")
        assertEquals(listOf(listOf(firstIncomeA), emptyList()), emissions)

        source.emitSuccess(userB, listOf(document(incomeB)))
        runCurrent()
        assertEquals(
            listOf(listOf(firstIncomeA), emptyList(), listOf(incomeB)),
            emissions
        )

        collection.cancel()
    }

    @Test
    fun rapidSignOutAndAccountSwitchCannotLoseResetOrDeliverQueuedOldSnapshots() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val firstIncomeA = lkrIncome("income-a-first")
        val queuedIncomesA = (1..70).map { index ->
            lkrIncome("income-a-queued-$index")
        }
        val incomeB = lkrIncome("income-b")
        val emissions = mutableListOf<List<Income>>()
        val collectorBlocked = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val collection = backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect { incomes ->
                    emissions += incomes
                    if (incomes == listOf(firstIncomeA)) {
                        collectorBlocked.complete(Unit)
                        releaseCollector.await()
                    }
                }
        }

        runCurrent()
        val userA = source.singleSubscription("user-a")
        source.emitSuccess(userA, listOf(document(firstIncomeA)))
        runCurrent()
        assertTrue(collectorBlocked.isCompleted)

        queuedIncomesA.forEach { income ->
            source.emitSuccess(userA, listOf(document(income)))
        }
        runCurrent()

        val rapidTransition = launch {
            authRepository.emit(null)
            authRepository.emit("user-b")
        }
        runCurrent()

        assertTrue(rapidTransition.isCompleted)
        assertTrue(userA.removed)
        assertEquals(listOf(listOf(firstIncomeA)), emissions)

        releaseCollector.complete(Unit)
        runCurrent()
        val userB = source.singleSubscription("user-b")
        assertEquals(listOf(listOf(firstIncomeA), emptyList()), emissions)

        source.emitSuccess(userB, listOf(document(incomeB)))
        runCurrent()
        assertEquals(
            listOf(listOf(firstIncomeA), emptyList(), listOf(incomeB)),
            emissions
        )

        collection.cancel()
    }

    @Test
    fun rapidSignOutAndSameUserReauthenticationRejectsPreviousListenerData() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val firstIncome = lkrIncome("income-a-first-session")
        val queuedIncomes = (1..70).map { index ->
            lkrIncome("income-a-old-session-$index")
        }
        val newSessionIncome = lkrIncome("income-a-new-session")
        val emissions = mutableListOf<List<Income>>()
        val collectorBlocked = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val collection = backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect { incomes ->
                    emissions += incomes
                    if (incomes == listOf(firstIncome)) {
                        collectorBlocked.complete(Unit)
                        releaseCollector.await()
                    }
                }
        }

        runCurrent()
        val previousSession = source.singleSubscription("user-a")
        source.emitSuccess(previousSession, listOf(document(firstIncome)))
        runCurrent()
        assertTrue(collectorBlocked.isCompleted)

        queuedIncomes.forEach { income ->
            source.emitSuccess(previousSession, listOf(document(income)))
        }
        runCurrent()

        val rapidTransition = launch {
            authRepository.emit(null)
            authRepository.emit("user-a")
        }
        runCurrent()

        assertTrue(rapidTransition.isCompleted)
        assertTrue(previousSession.removed)
        assertEquals(listOf(listOf(firstIncome)), emissions)

        releaseCollector.complete(Unit)
        runCurrent()
        val currentSession = source.subscriptions.single { subscription ->
            subscription.query.authenticatedUid == "user-a" &&
                subscription !== previousSession
        }
        assertEquals(listOf(listOf(firstIncome), emptyList()), emissions)

        source.emitSuccess(
            previousSession,
            listOf(document(lkrIncome("late-old-session-income")))
        )
        source.emitSuccess(currentSession, listOf(document(newSessionIncome)))
        runCurrent()
        assertEquals(
            listOf(listOf(firstIncome), emptyList(), listOf(newSessionIncome)),
            emissions
        )

        collection.cancel()
    }

    @Test
    fun signOutClearsDataRemovesListenerAndRejectsLateCallbacks() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = mutableListOf<List<Income>>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        val income = lkrIncome("income-a")
        source.emitSuccess(subscription, listOf(document(income)))
        runCurrent()

        authRepository.emit(null)
        runCurrent()
        assertTrue(subscription.removed)
        assertEquals(listOf(listOf(income), emptyList()), emissions)
        assertEquals(1, source.subscriptions.size)

        source.emitSuccess(subscription, listOf(document(lkrIncome("late-income"))))
        runCurrent()
        assertEquals(listOf(listOf(income), emptyList()), emissions)

        collection.cancel()
    }

    @Test
    fun appliesInclusiveExclusiveDateWindowAndDeterministicOrdering() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .first()
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        source.emitServerDataset(
            subscription,
            listOf(
                document(lkrIncome("income-out-before", startDate.minusDays(1))),
                document(lkrIncome("income-b", startDate)),
                document(lkrIncome("income-a", startDate)),
                document(lkrIncome("income-c", endDateExclusive.minusDays(1))),
                document(lkrIncome("income-out-end", endDateExclusive))
            )
        )

        assertEquals(
            listOf("income-a", "income-b", "income-c"),
            result.await().map { it.id }
        )
    }

    @Test
    fun realTimeSnapshotsExposeEmptyAddUpdateAndDelete() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = async(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .take(4)
                .toList()
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        val original = lkrIncome("income-a", note = "Original")
        val updated = original.copy(note = "Updated")
        source.emitSuccess(subscription, emptyList())
        source.emitSuccess(subscription, listOf(document(original)))
        source.emitSuccess(subscription, listOf(document(updated)))
        source.emitSuccess(subscription, emptyList())

        assertEquals(
            listOf(emptyList(), listOf(original), listOf(updated), emptyList()),
            emissions.await()
        )
        runCurrent()
        assertTrue(subscription.removed)
    }

    @Test
    fun strictMapperPreservesHistoricalMoneySnapshotAndMetadata() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .first()
        }

        runCurrent()
        val income = usdIncome()
        source.emitSuccess(source.singleSubscription("user-a"), listOf(document(income)))

        val reconstructed = result.await().single()
        assertEquals(income, reconstructed)
        assertEquals(income.money, reconstructed.money)
        assertEquals("310.123456", reconstructed.money.rateLkrPerUnitDecimal)
        assertEquals(LocalDate.of(2026, 10, 18), reconstructed.money.rateDate)
    }

    @Test
    fun malformedAndUnknownSchemaDocumentsFailWithoutEmptyFallback() = runTest {
        val valid = document(lkrIncome("income-a"))
        val invalidDocuments = listOf(
            valid.copy(fields = valid.fields - "money") to "money",
            valid.copy(fields = valid.fields + ("schemaVersion" to 2L)) to "schemaVersion"
        )

        invalidDocuments.forEach { (invalidDocument, expectedField) ->
            val authRepository = TestAuthRepository("user-a")
            val source = TestIncomeFirestoreSnapshotSource()
            val emissions = mutableListOf<List<Income>>()
            val failure = async(UnconfinedTestDispatcher(testScheduler)) {
                try {
                    observer(authRepository, source)
                        .observeIncomes(startDate, endDateExclusive)
                        .collect(emissions::add)
                    null
                } catch (exception: Throwable) {
                    exception
                }
            }

            runCurrent()
            val subscription = source.singleSubscription("user-a")
            source.emitSuccess(subscription, listOf(invalidDocument))
            val exception = failure.await()

            assertTrue(exception is IncomeFirestoreMappingException)
            assertEquals(expectedField, (exception as IncomeFirestoreMappingException).fieldPath)
            assertTrue(emissions.isEmpty())
            runCurrent()
            assertTrue(subscription.removed)
        }
    }

    @Test
    fun firestorePermissionAndUnavailableErrorsRemainVisible() = runTest {
        val errors = listOf(
            TestFirestoreFailure(TestFirestoreFailureCode.PERMISSION_DENIED),
            TestFirestoreFailure(TestFirestoreFailureCode.UNAVAILABLE)
        )

        errors.forEach { expectedError ->
            val authRepository = TestAuthRepository("user-a")
            val source = TestIncomeFirestoreSnapshotSource()
            val emissions = mutableListOf<List<Income>>()
            val failure = async(UnconfinedTestDispatcher(testScheduler)) {
                try {
                    observer(authRepository, source)
                        .observeIncomes(startDate, endDateExclusive)
                        .collect(emissions::add)
                    null
                } catch (exception: Throwable) {
                    exception
                }
            }

            runCurrent()
            val subscription = source.singleSubscription("user-a")
            source.emitFailure(subscription, expectedError)

            assertSame(expectedError, failure.await())
            assertTrue(emissions.isEmpty())
            runCurrent()
            assertTrue(subscription.removed)
        }
    }

    @Test
    fun synchronousListenerRegistrationFailureIsPropagated() = runTest {
        val expectedError = IllegalStateException("Listener registration failed")
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource().apply {
            registrationFailure = expectedError
        }

        val actualError = try {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .first()
            null
        } catch (exception: Throwable) {
            exception
        }

        assertEquals(expectedError::class, actualError?.let { it::class })
        assertEquals(expectedError.message, actualError?.message)
        assertTrue(source.subscriptions.isEmpty())
    }

    @Test
    fun collectorCancellationRemovesListenerAndLateCallbackCannotEmit() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val emissions = mutableListOf<List<Income>>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        collection.cancel(CancellationException("Collector stopped"))
        runCurrent()
        assertTrue(subscription.removed)

        source.emitSuccess(subscription, listOf(document(lkrIncome("late-income"))))
        runCurrent()
        assertTrue(emissions.isEmpty())
    }

    @Test
    fun normalAuthFlowCompletionClearsDataAndRemovesListener() = runTest {
        val authRepository = CompletingTestAuthRepository("user-a")
        val source = TestIncomeFirestoreSnapshotSource()
        val income = lkrIncome("income-a")
        val emissions = mutableListOf<List<Income>>()
        val collection = async(StandardTestDispatcher(testScheduler)) {
            observer(authRepository, source)
                .observeIncomes(startDate, endDateExclusive)
                .collect(emissions::add)
        }

        runCurrent()
        val subscription = source.singleSubscription("user-a")
        source.emitSuccess(subscription, listOf(document(income)))
        runCurrent()
        assertEquals(listOf(listOf(income)), emissions)

        authRepository.complete()
        runCurrent()

        collection.await()
        assertEquals(listOf(listOf(income), emptyList()), emissions)
        assertTrue(subscription.removed)
    }

    private class TestAuthRepository(initialUid: UserId?) : AuthRepository {
        private val authStates = MutableSharedFlow<UserId?>(replay = 1)

        var observerCount = 0
            private set

        init {
            check(authStates.tryEmit(initialUid))
        }

        override fun observeAuthState(): Flow<UserId?> {
            observerCount += 1
            return authStates
        }

        suspend fun emit(uid: UserId?) {
            authStates.emit(uid)
        }

        override suspend fun login(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun register(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun signOut(): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }
    }

    private class CompletingTestAuthRepository(
        private val initialUid: UserId?
    ) : AuthRepository {
        private val completion = CompletableDeferred<Unit>()

        override fun observeAuthState(): Flow<UserId?> = flow {
            emit(initialUid)
            completion.await()
        }

        fun complete() {
            completion.complete(Unit)
        }

        override suspend fun login(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun register(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }

        override suspend fun signOut(): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by observer tests"))
        }
    }

    private class TestIncomeFirestoreSnapshotSource : IncomeFirestoreSnapshotSource {
        val subscriptions = mutableListOf<Subscription>()
        var registrationFailure: Throwable? = null

        override fun listen(
            query: IncomeFirestoreQuery,
            onSnapshot: (Result<List<IncomeFirestoreDocument>>) -> Unit
        ): IncomeFirestoreListenerRegistration {
            registrationFailure?.let { throw it }

            val subscription = Subscription(query, onSnapshot)
            subscriptions += subscription
            return IncomeFirestoreListenerRegistration {
                subscription.removed = true
            }
        }

        fun singleSubscription(uid: UserId): Subscription {
            return subscriptions.single { it.query.authenticatedUid == uid }
        }

        fun emitSuccess(
            subscription: Subscription,
            documents: List<IncomeFirestoreDocument>
        ) {
            subscription.callback(Result.success(documents))
        }

        fun emitFailure(subscription: Subscription, exception: Throwable) {
            subscription.callback(Result.failure(exception))
        }

        fun emitServerDataset(
            subscription: Subscription,
            documents: List<IncomeFirestoreDocument>
        ) {
            val query = subscription.query
            val matchingDocuments = documents
                .filter { document ->
                    val occurredDate = document.fields["occurredDate"] as String
                    occurredDate >= query.startDateInclusiveIso &&
                        occurredDate < query.endDateExclusiveIso
                }
                .sortedWith(
                    compareBy<IncomeFirestoreDocument> {
                        it.fields["occurredDate"] as String
                    }.thenBy { it.id }
                )
            emitSuccess(subscription, matchingDocuments)
        }

        class Subscription(
            val query: IncomeFirestoreQuery,
            val callback: (Result<List<IncomeFirestoreDocument>>) -> Unit,
            var removed: Boolean = false
        )
    }

    private enum class TestFirestoreFailureCode {
        PERMISSION_DENIED,
        UNAVAILABLE
    }

    private class TestFirestoreFailure(
        val code: TestFirestoreFailureCode
    ) : Exception("Firestore listener failed: $code")
}
