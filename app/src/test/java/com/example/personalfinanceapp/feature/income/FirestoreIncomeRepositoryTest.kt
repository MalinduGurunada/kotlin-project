package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreIncomeRepositoryTest {

    private val startDate = LocalDate.of(2026, 10, 1)
    private val endDateExclusive = LocalDate.of(2026, 11, 1)

    @Test
    fun observeIncomesDelegatesToUidScopedObserver() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val snapshotSource = RecordingSnapshotSource()
        val repository = repository(authRepository, RecordingWriteGateway(), snapshotSource)
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeIncomes(startDate, endDateExclusive).first()
        }

        runCurrent()
        val subscription = snapshotSource.subscriptions.single()
        assertEquals("user-a", subscription.query.authenticatedUid)
        assertEquals("2026-10-01", subscription.query.startDateInclusiveIso)
        assertEquals("2026-11-01", subscription.query.endDateExclusiveIso)

        val income = lkrIncome("income-a")
        subscription.callback(Result.success(listOf(document(income))))

        assertEquals(listOf(income), result.await())
        runCurrent()
        assertTrue(subscription.removed)
    }

    @Test
    fun ownerBoundSaveUsesExpectedUserStableIdAndCanonicalPayload() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val income = lkrIncome(
            id = "stable-income-id",
            projectReference = "project-42",
            receivableId = "receivable-7",
            note = "October salary"
        )

        val result = repository.saveIncomeForOwner("user-a", income)

        assertSuccess(result)
        val call = gateway.setCalls.single()
        assertEquals("user-a", call.authenticatedUid)
        assertEquals("stable-income-id", call.incomeId)
        assertEquals(IncomeFirestoreMapper.toFirestoreMap(income), call.fields)
    }

    @Test
    fun ownerChangeBeforeAuthenticationCaptureRejectsWithoutWritingToNewUser() = runTest {
        val captureStarted = CompletableDeferred<Unit>()
        val releaseCapture = CompletableDeferred<Unit>()
        var uidAtCapture: UserId? = "user-a"
        val authRepository = TestAuthRepository(
            authFlow = flow {
                captureStarted.complete(Unit)
                releaseCapture.await()
                emit(uidAtCapture)
            }
        )
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val income = lkrIncome("stable-income-id")
        val pendingSave = async {
            repository.saveIncomeForOwner("user-a", income)
        }

        captureStarted.await()
        uidAtCapture = "user-b"
        releaseCapture.complete(Unit)
        val result = pendingSave.await()

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(gateway.setCalls.isEmpty())
        assertFalse(gateway.documents.containsKey("user-b" to income.id))
    }

    @Test
    fun signedOutOwnerBoundSaveRejectsWithoutPersistenceAttempt() = runTest {
        val gateway = RecordingWriteGateway()
        val repository = repository(TestAuthRepository(null), gateway)

        val result = repository.saveIncomeForOwner("user-a", lkrIncome("income-a"))

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(gateway.setCalls.isEmpty())
        assertTrue(gateway.documents.isEmpty())
    }

    @Test
    fun ownerValidationFailureCanRetryWithTheSameStableId() = runTest {
        val authRepository = TestAuthRepository("user-b")
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val income = lkrIncome("stable-retry-id")

        val rejected = repository.saveIncomeForOwner("user-a", income)
        authRepository.setUid("user-a")
        val retried = repository.saveIncomeForOwner("user-a", income)

        assertTrue(rejected.exceptionOrNull() is IllegalStateException)
        assertSuccess(retried)
        assertEquals(listOf("stable-retry-id"), gateway.setCalls.map { it.incomeId })
        assertTrue(gateway.documents.containsKey("user-a" to "stable-retry-id"))
        assertFalse(gateway.documents.containsKey("user-b" to "stable-retry-id"))
    }

    @Test
    fun signedOutSaveAndDeleteFailWithoutPersistenceAttempt() = runTest {
        val authRepository = TestAuthRepository(null)
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)

        val saveResult = repository.saveIncome(lkrIncome("income-a"))
        val deleteResult = repository.deleteIncome("income-a")

        assertTrue(saveResult.exceptionOrNull() is IllegalStateException)
        assertTrue(deleteResult.exceptionOrNull() is IllegalStateException)
        assertTrue(gateway.setCalls.isEmpty())
        assertTrue(gateway.deleteCalls.isEmpty())
    }

    @Test
    fun differentAuthenticatedUsersWriteOnlyToTheirCapturedCollections() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)

        assertSuccess(repository.saveIncome(lkrIncome("income-a")))
        authRepository.setUid("user-b")
        assertSuccess(repository.saveIncome(lkrIncome("income-b")))

        assertEquals(
            listOf("user-a" to "income-a", "user-b" to "income-b"),
            gateway.setCalls.map { it.authenticatedUid to it.incomeId }
        )
    }

    @Test
    fun authenticationChangeCannotRedirectPendingSave() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        val releaseWrite = CompletableDeferred<Unit>()
        gateway.beforeSet = { releaseWrite.await() }
        val repository = repository(authRepository, gateway)
        val pendingSave = async {
            repository.saveIncomeForOwner("user-a", lkrIncome("income-a"))
        }

        runCurrent()
        assertEquals("user-a", gateway.setCalls.single().authenticatedUid)
        assertFalse(pendingSave.isCompleted)

        authRepository.setUid("user-b")
        releaseWrite.complete(Unit)

        assertSuccess(pendingSave.await())
        assertEquals("user-a", gateway.setCalls.single().authenticatedUid)
        assertTrue(gateway.documents.containsKey("user-a" to "income-a"))
        assertFalse(gateway.documents.containsKey("user-b" to "income-a"))
    }

    @Test
    fun authenticationChangeCannotRedirectPendingDelete() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        gateway.seed("user-a", lkrIncome("income-a"))
        gateway.seed("user-b", lkrIncome("income-a"))
        val releaseDelete = CompletableDeferred<Unit>()
        gateway.beforeDelete = { releaseDelete.await() }
        val repository = repository(authRepository, gateway)
        val pendingDelete = async {
            repository.deleteIncome("income-a")
        }

        runCurrent()
        assertEquals(listOf("user-a" to "income-a"), gateway.deleteCalls)
        assertFalse(pendingDelete.isCompleted)

        authRepository.setUid("user-b")
        releaseDelete.complete(Unit)

        assertSuccess(pendingDelete.await())
        assertFalse(gateway.documents.containsKey("user-a" to "income-a"))
        assertTrue(gateway.documents.containsKey("user-b" to "income-a"))
    }

    @Test
    fun cancellationDuringAuthenticationLookupPropagatesAndCleansUp() = runTest {
        val authStarted = CompletableDeferred<Unit>()
        var authCollectionCancelled = false
        val authRepository = TestAuthRepository(
            authFlow = flow {
                authStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    authCollectionCancelled = true
                }
            }
        )
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val pendingSave = async {
            repository.saveIncome(lkrIncome("income-a"))
        }

        authStarted.await()
        pendingSave.cancel()
        val failure = runCatching { pendingSave.await() }.exceptionOrNull()
        runCurrent()

        assertTrue(failure is CancellationException)
        assertTrue(authCollectionCancelled)
        assertTrue(gateway.setCalls.isEmpty())
    }

    @Test
    fun invalidIncomeAndInvalidSaveIdFailBeforeGateway() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val invalidAmount = lkrIncome("income-invalid").copy(
            money = lkrIncome("income-invalid").money.copy(baseAmountMinor = 0L)
        )

        val invalidIncomeResult = repository.saveIncome(invalidAmount)
        val invalidIdResult = repository.saveIncome(lkrIncome("folder/income"))

        assertTrue(invalidIncomeResult.exceptionOrNull() is IllegalArgumentException)
        assertTrue(invalidIdResult.exceptionOrNull() is IncomeFirestoreMappingException)
        assertTrue(gateway.setCalls.isEmpty())
    }

    @Test
    fun retryAndUpdateReuseStableIdAndReplaceEntireSnapshot() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        val repository = repository(authRepository, gateway)
        val original = lkrIncome(
            id = "income-a",
            projectReference = "old-project",
            receivableId = "receivable-a",
            note = "old-note"
        )
        val updated = original.copy(
            projectReference = null,
            receivableId = null,
            note = null
        )

        assertSuccess(repository.saveIncome(original))
        assertSuccess(repository.saveIncome(original))
        assertSuccess(repository.saveIncome(updated))

        assertEquals(listOf("income-a", "income-a", "income-a"), gateway.setCalls.map { it.incomeId })
        assertEquals(1, gateway.documents.size)
        val stored = gateway.documents.getValue("user-a" to "income-a")
        assertFalse(stored.containsKey("projectReference"))
        assertFalse(stored.containsKey("receivableId"))
        assertFalse(stored.containsKey("note"))
        assertEquals(IncomeFirestoreMapper.toFirestoreMap(updated), stored)
    }

    @Test
    fun savePreservesExactHistoricalMoneySnapshotAndMetadata() = runTest {
        val gateway = RecordingWriteGateway()
        val repository = repository(TestAuthRepository("user-a"), gateway)
        val income = usdIncome(
            id = "income-usd",
            source = IncomeSource.FREELANCE,
            projectReference = "historical-project",
            note = "manual transfer rate"
        )

        assertSuccess(repository.saveIncome(income))

        val fields = gateway.setCalls.single().fields
        @Suppress("UNCHECKED_CAST")
        val money = fields.getValue("money") as Map<String, Any>
        assertEquals("125.00", money["originalAmountDecimal"])
        assertEquals("USD", money["originalCurrency"])
        assertEquals("310.123456", money["rateLkrPerUnitDecimal"])
        assertEquals(income.money.baseAmountMinor, money["baseAmountMinor"])
        assertEquals("MANUAL_TRANSFER", money["rateSource"])
        assertEquals("2026-10-18", money["rateDate"])
        assertEquals("historical-project", fields["projectReference"])
        assertEquals("manual transfer rate", fields["note"])
    }

    @Test
    fun cryptoFieldsAndEveryIncomeSourceRemainSupported() = runTest {
        val gateway = RecordingWriteGateway()
        val repository = repository(TestAuthRepository("user-a"), gateway)
        val incomes = listOf(
            lkrIncome("salary", IncomeSource.SALARY),
            lkrIncome("freelance", IncomeSource.FREELANCE),
            lkrIncome("adsense", IncomeSource.ADSENSE),
            usdIncome(
                id = "crypto",
                source = IncomeSource.CRYPTO,
                entryKind = IncomeEntryKind.REALIZED_PNL,
                pnlDirection = PnlDirection.LOSS,
                assetSymbol = "BTC"
            )
        )

        incomes.forEach { income ->
            assertSuccess(repository.saveIncome(income))
        }

        assertEquals(IncomeSource.entries.map { it.name }.toSet(), gateway.setCalls.map {
            it.fields.getValue("source")
        }.toSet())
        val cryptoFields = gateway.documents.getValue("user-a" to "crypto")
        assertEquals("REALIZED_PNL", cryptoFields["entryKind"])
        assertEquals("LOSS", cryptoFields["pnlDirection"])
        assertEquals("BTC", cryptoFields["assetSymbol"])
    }

    @Test
    fun authenticatedDeleteRemovesOnlyCapturedUsersExistingDocument() = runTest {
        val authRepository = TestAuthRepository("user-a")
        val gateway = RecordingWriteGateway()
        gateway.seed("user-a", lkrIncome("income-a"))
        gateway.seed("user-b", lkrIncome("income-a"))
        val repository = repository(authRepository, gateway)

        val result = repository.deleteIncome("income-a")

        assertSuccess(result)
        assertEquals(listOf("user-a" to "income-a"), gateway.deleteCalls)
        assertFalse(gateway.documents.containsKey("user-a" to "income-a"))
        assertTrue(gateway.documents.containsKey("user-b" to "income-a"))
    }

    @Test
    fun deleteMissingIncomeReturnsNoSuchElementFailure() = runTest {
        val gateway = RecordingWriteGateway()
        val repository = repository(TestAuthRepository("user-a"), gateway)

        val result = repository.deleteIncome("missing-income")

        assertTrue(result.exceptionOrNull() is NoSuchElementException)
        assertEquals(1, gateway.deleteCalls.size)
    }

    @Test
    fun invalidDeleteIdsFailBeforeGatewayPathConstruction() = runTest {
        val gateway = RecordingWriteGateway()
        val repository = repository(TestAuthRepository("user-a"), gateway)
        val invalidIds = listOf("", ".", "..", "folder/income", "__reserved__", "a".repeat(1_501))

        invalidIds.forEach { invalidId ->
            val result = repository.deleteIncome(invalidId)
            assertTrue("Expected invalid ID failure for '$invalidId'", result.exceptionOrNull() is IllegalArgumentException)
        }

        assertTrue(gateway.deleteCalls.isEmpty())
    }

    @Test
    fun transactionAndOfflineDeleteFailuresPropagateWithoutRetry() = runTest {
        val failures = listOf(
            TestFirestoreFailure(TestFailureCode.TRANSACTION_ABORTED),
            TestFirestoreFailure(TestFailureCode.OFFLINE)
        )

        failures.forEach { expectedFailure ->
            val gateway = RecordingWriteGateway().apply {
                deleteFailure = expectedFailure
                seed("user-a", lkrIncome("income-a"))
            }
            val repository = repository(TestAuthRepository("user-a"), gateway)

            val result = repository.deleteIncome("income-a")

            assertSame(expectedFailure, result.exceptionOrNull())
            assertEquals(1, gateway.deleteCalls.size)
            assertTrue(gateway.documents.containsKey("user-a" to "income-a"))
        }
    }

    @Test
    fun firestoreSaveFailuresPropagateWithoutAutomaticRetry() = runTest {
        val failures = listOf(
            TestFirestoreFailure(TestFailureCode.PERMISSION_DENIED),
            TestFirestoreFailure(TestFailureCode.UNAVAILABLE)
        )

        failures.forEach { expectedFailure ->
            val gateway = RecordingWriteGateway().apply {
                setFailure = expectedFailure
            }
            val repository = repository(TestAuthRepository("user-a"), gateway)

            val result = repository.saveIncome(lkrIncome("income-a"))

            assertSame(expectedFailure, result.exceptionOrNull())
            assertEquals(1, gateway.setCalls.size)
            assertTrue(gateway.documents.isEmpty())
        }
    }

    @Test
    fun cancellationDuringPendingWriteIsNotConvertedToResultFailure() = runTest {
        val writeStarted = CompletableDeferred<Unit>()
        var writeCancelled = false
        val gateway = RecordingWriteGateway().apply {
            beforeSet = {
                writeStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    writeCancelled = true
                }
            }
        }
        val repository = repository(TestAuthRepository("user-a"), gateway)
        val pendingSave = async {
            repository.saveIncome(lkrIncome("income-a"))
        }

        writeStarted.await()
        pendingSave.cancel()
        val failure = runCatching { pendingSave.await() }.exceptionOrNull()
        runCurrent()

        assertTrue(failure is CancellationException)
        assertTrue(writeCancelled)
        assertEquals(1, gateway.setCalls.size)
        assertTrue(gateway.documents.isEmpty())
    }

    @Test
    fun saveDoesNotReportSuccessBeforePendingWriteCompletes() = runTest {
        val releaseWrite = CompletableDeferred<Unit>()
        val gateway = RecordingWriteGateway().apply {
            beforeSet = { releaseWrite.await() }
        }
        val repository = repository(TestAuthRepository("user-a"), gateway)
        val pendingSave = async {
            repository.saveIncome(lkrIncome("income-a"))
        }

        runCurrent()
        assertEquals(1, gateway.setCalls.size)
        assertFalse(pendingSave.isCompleted)

        releaseWrite.complete(Unit)

        assertSuccess(pendingSave.await())
        assertTrue(gateway.documents.containsKey("user-a" to "income-a"))
    }

    private fun repository(
        authRepository: AuthRepository,
        gateway: RecordingWriteGateway,
        snapshotSource: RecordingSnapshotSource = RecordingSnapshotSource()
    ): FirestoreIncomeRepository {
        return FirestoreIncomeRepository(
            authRepository = authRepository,
            observer = IncomeFirestoreObserver(authRepository, snapshotSource),
            writeGateway = gateway
        )
    }

    private fun lkrIncome(
        id: String,
        source: IncomeSource = IncomeSource.SALARY,
        projectReference: String? = null,
        receivableId: String? = null,
        note: String? = null
    ): Income {
        val occurredDate = LocalDate.of(2026, 10, 21)
        return Income(
            id = id,
            source = source,
            entryKind = IncomeEntryKind.RECEIPT,
            money = MoneyConverter.fromLkr("125000.50", occurredDate).getOrThrow(),
            occurredDate = occurredDate,
            projectReference = projectReference,
            receivableId = receivableId,
            note = note
        )
    }

    private fun usdIncome(
        id: String,
        source: IncomeSource,
        entryKind: IncomeEntryKind = IncomeEntryKind.RECEIPT,
        pnlDirection: PnlDirection? = null,
        projectReference: String? = null,
        assetSymbol: String? = null,
        note: String? = null
    ): Income {
        return Income(
            id = id,
            source = source,
            entryKind = entryKind,
            money = MoneyConverter.fromUsd(
                amountInput = "125.00",
                rateLkrPerUsdInput = "310.123456",
                rateSource = RateSource.MANUAL_TRANSFER,
                rateDate = LocalDate.of(2026, 10, 18)
            ).getOrThrow(),
            occurredDate = LocalDate.of(2026, 10, 21),
            pnlDirection = pnlDirection,
            projectReference = projectReference,
            assetSymbol = assetSymbol,
            note = note
        )
    }

    private fun document(income: Income): IncomeFirestoreDocument {
        return IncomeFirestoreDocument(
            id = income.id,
            fields = IncomeFirestoreMapper.toFirestoreMap(income)
        )
    }

    private fun assertSuccess(result: Result<Unit>) {
        assertTrue(result.exceptionOrNull()?.toString() ?: "Expected success", result.isSuccess)
    }

    private class TestAuthRepository(
        initialUid: UserId? = null,
        private val authFlow: Flow<UserId?>? = null
    ) : AuthRepository {
        private val authState = MutableStateFlow(initialUid)

        override fun observeAuthState(): Flow<UserId?> = authFlow ?: authState

        fun setUid(uid: UserId?) {
            authState.value = uid
        }

        override suspend fun login(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by repository tests"))
        }

        override suspend fun register(email: String, password: String): Result<UserId> {
            return Result.failure(UnsupportedOperationException("Not used by repository tests"))
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by repository tests"))
        }

        override suspend fun signOut(): Result<Unit> {
            return Result.failure(UnsupportedOperationException("Not used by repository tests"))
        }
    }

    private class RecordingWriteGateway : IncomeFirestoreWriteGateway {
        val setCalls = mutableListOf<SetCall>()
        val deleteCalls = mutableListOf<Pair<UserId, String>>()
        val documents = mutableMapOf<Pair<UserId, String>, Map<String, Any>>()

        var beforeSet: suspend () -> Unit = {}
        var beforeDelete: suspend () -> Unit = {}
        var setFailure: Throwable? = null
        var deleteFailure: Throwable? = null

        override suspend fun setIncome(
            authenticatedUid: UserId,
            incomeId: String,
            fields: Map<String, Any>
        ) {
            setCalls += SetCall(authenticatedUid, incomeId, fields)
            beforeSet()
            setFailure?.let { throw it }
            documents[authenticatedUid to incomeId] = fields.toMap()
        }

        override suspend fun deleteIncomeIfExists(
            authenticatedUid: UserId,
            incomeId: String
        ) {
            val key = authenticatedUid to incomeId
            deleteCalls += key
            beforeDelete()
            deleteFailure?.let { throw it }
            if (!documents.containsKey(key)) {
                throw NoSuchElementException("Income not found")
            }
            documents.remove(key)
        }

        fun seed(authenticatedUid: UserId, income: Income) {
            documents[authenticatedUid to income.id] = IncomeFirestoreMapper.toFirestoreMap(income)
        }

        data class SetCall(
            val authenticatedUid: UserId,
            val incomeId: String,
            val fields: Map<String, Any>
        )
    }

    private class RecordingSnapshotSource : IncomeFirestoreSnapshotSource {
        val subscriptions = mutableListOf<Subscription>()

        override fun listen(
            query: IncomeFirestoreQuery,
            onSnapshot: (Result<List<IncomeFirestoreDocument>>) -> Unit
        ): IncomeFirestoreListenerRegistration {
            val subscription = Subscription(query, onSnapshot)
            subscriptions += subscription
            return IncomeFirestoreListenerRegistration {
                subscription.removed = true
            }
        }

        data class Subscription(
            val query: IncomeFirestoreQuery,
            val callback: (Result<List<IncomeFirestoreDocument>>) -> Unit,
            var removed: Boolean = false
        )
    }

    private enum class TestFailureCode {
        PERMISSION_DENIED,
        UNAVAILABLE,
        TRANSACTION_ABORTED,
        OFFLINE
    }

    private class TestFirestoreFailure(
        val code: TestFailureCode
    ) : Exception("Firestore operation failed: $code")
}
