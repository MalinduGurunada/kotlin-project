package com.example.personalfinanceapp.feature.income

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.FirebaseAuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * End-to-end tests for the production income repository against local Firebase emulators.
 *
 * Start Auth and Firestore with project [TEST_PROJECT_ID] before running this class. The setup is
 * deliberately fail-closed: it requires an Android Emulator, verifies both local ports before any
 * Firebase write, and constructs a non-default FirebaseApp whose project ID is asserted before use.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreIncomeRepositoryEmulatorTest {

    private lateinit var testApp: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authRepository: FirebaseAuthRepository
    private lateinit var repository: FirestoreIncomeRepository

    private val users = mutableListOf<TestUser>()
    private val documentIdsByUid = mutableMapOf<String, MutableSet<String>>()

    @Before
    fun setUp() {
        requireAndroidEmulator()
        requireLocalNetworkPermission()
        requireReachableEmulator(AUTH_EMULATOR_PORT, "Authentication")
        requireReachableEmulator(FIRESTORE_EMULATOR_PORT, "Firestore")

        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val options = FirebaseOptions.Builder()
            .setProjectId(TEST_PROJECT_ID)
            .setApplicationId(TEST_APPLICATION_ID)
            .setApiKey(TEST_API_KEY)
            .build()

        testApp = checkNotNull(
            FirebaseApp.initializeApp(context, options, "income-emulator-${uniqueSuffix()}")
        )
        assertEquals(TEST_PROJECT_ID, testApp.options.projectId)
        assertNotEquals(FirebaseApp.DEFAULT_APP_NAME, testApp.name)

        auth = FirebaseAuth.getInstance(testApp)
        firestore = FirebaseFirestore.getInstance(testApp)
        assertEquals(testApp.name, auth.app.name)
        assertEquals(testApp.name, firestore.app.name)

        auth.useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
        firestore.useEmulator(EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)

        authRepository = FirebaseAuthRepository(auth)
        repository = FirestoreIncomeRepository(authRepository, firestore)
    }

    @After
    fun tearDown() {
        if (!::testApp.isInitialized) return

        if (::firestore.isInitialized && ::auth.isInitialized && ::authRepository.isInitialized) {
            runBlocking {
                withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
                    runCatching { firestore.enableNetwork().awaitTask() }
                    users.forEach { user ->
                        if (authRepository.login(user.email, user.password).isSuccess) {
                            documentIdsByUid[user.uid].orEmpty().forEach { incomeId ->
                                runCatching {
                                    incomeDocument(user.uid, incomeId).delete().awaitTask()
                                }
                            }
                            runCatching { auth.currentUser?.delete()?.awaitTask() }
                        }
                    }
                    runCatching { auth.signOut() }
                }
                withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
                    runCatching { firestore.terminate().awaitTask() }
                }
            }
        }
    }

    @Test
    fun createUpdateAndRepeatedSaveUseOneStableUserScopedDocument() = runBlocking {
        val user = registerUser("crud")
        val incomeId = "income-crud-${uniqueSuffix()}"
        val original = lkrIncome(
            id = incomeId,
            occurredDate = LocalDate.of(2026, 10, 10),
            projectReference = "project-original",
            note = "original"
        )

        assertSuccess(saveAndTrack(original))

        val reference = incomeDocument(user.uid, incomeId)
        assertEquals("users/${user.uid}/incomes/$incomeId", reference.path)
        val created = reference.get(Source.SERVER).awaitTask()
        assertTrue(created.exists())
        assertEquals(incomeId, created.id)
        assertEquals(1L, created.getLong("schemaVersion"))
        assertEquals(IncomeFirestoreMapper.toFirestoreMap(original), created.data)
        assertFalse(created.data.orEmpty().containsKey("userId"))
        assertFalse(created.data.orEmpty().containsKey("ownerUid"))
        assertFalse(containsValue(created.data, user.uid))
        assertEquals(1, incomeCollection(user.uid).get(Source.SERVER).awaitTask().size())

        assertSuccess(saveAndTrack(original))
        assertEquals(1, incomeCollection(user.uid).get(Source.SERVER).awaitTask().size())

        val updatedDate = LocalDate.of(2026, 10, 12)
        val updated = original.copy(
            money = MoneyConverter.fromUsd(
                amountInput = "25.50",
                rateLkrPerUsdInput = "310.123456",
                rateSource = RateSource.MANUAL_REFERENCE,
                rateDate = LocalDate.of(2026, 10, 11)
            ).getOrThrow(),
            occurredDate = updatedDate,
            projectReference = null,
            note = "updated"
        )
        assertSuccess(saveAndTrack(updated))

        val updatedSnapshot = reference.get(Source.SERVER).awaitTask()
        assertEquals(IncomeFirestoreMapper.toFirestoreMap(updated), updatedSnapshot.data)
        assertFalse(updatedSnapshot.data.orEmpty().containsKey("projectReference"))
        assertEquals("updated", updatedSnapshot.getString("note"))
        assertEquals(1, incomeCollection(user.uid).get(Source.SERVER).awaitTask().size())

        val invalid = updated.copy(
            money = updated.money.copy(baseAmountMinor = updated.money.baseAmountMinor + 1L)
        )
        val invalidResult = repository.saveIncome(invalid)
        assertTrue(invalidResult.exceptionOrNull() is IllegalArgumentException)
        assertEquals(
            IncomeFirestoreMapper.toFirestoreMap(updated),
            reference.get(Source.SERVER).awaitTask().data
        )
    }

    @Test
    fun observationUsesRealtimeRangeOrderingUpdatesAndTransactionalDeletion() = runBlocking {
        val user = registerUser("observe")
        val start = LocalDate.of(2026, 10, 1)
        val endExclusive = LocalDate.of(2026, 11, 1)
        assertTrue(incomeCollection(user.uid).get(Source.SERVER).awaitTask().isEmpty)

        val probe = FlowProbe(this, repository.observeIncomes(start, endExclusive))
        assertEquals(emptyList<Income>(), probe.awaitValue { it.isEmpty() })

        val atStart = lkrIncome("start-${uniqueSuffix()}", start, note = "start")
        val middleB = lkrIncome("b-${uniqueSuffix()}", LocalDate.of(2026, 10, 15))
        val middleA = lkrIncome("a-${uniqueSuffix()}", LocalDate.of(2026, 10, 15))
        val atEnd = lkrIncome("end-${uniqueSuffix()}", endExclusive)
        val beforeStart = lkrIncome("before-${uniqueSuffix()}", start.minusDays(1))

        assertSuccess(saveAndTrack(atStart))
        assertEquals(listOf(atStart), probe.awaitValue { values -> values == listOf(atStart) })
        listOf(middleB, middleA, atEnd, beforeStart).forEach { income ->
            assertSuccess(saveAndTrack(income))
        }

        val expectedOrder = listOf(atStart, middleA, middleB)
        assertEquals(
            expectedOrder,
            probe.awaitValue { values -> values.map(Income::id) == expectedOrder.map(Income::id) }
        )

        val updatedMiddle = middleA.copy(note = "snapshot update")
        assertSuccess(saveAndTrack(updatedMiddle))
        val afterUpdate = probe.awaitValue { values ->
            values.any { it.id == updatedMiddle.id && it.note == "snapshot update" }
        }
        assertEquals(updatedMiddle.money, afterUpdate.single { it.id == updatedMiddle.id }.money)

        assertSuccess(repository.deleteIncome(updatedMiddle.id))
        val afterDelete = probe.awaitValue { values -> values.none { it.id == updatedMiddle.id } }
        assertEquals(listOf(atStart.id, middleB.id), afterDelete.map(Income::id))

        val missingDelete = repository.deleteIncome(updatedMiddle.id)
        assertTrue(missingDelete.exceptionOrNull() is NoSuchElementException)
        listOf("", ".", "..", "folder/income", "__reserved__").forEach { invalidId ->
            assertTrue(repository.deleteIncome(invalidId).exceptionOrNull() is IllegalArgumentException)
        }

        probe.cancelAndJoin()
        assertTrue(probe.isStopped)
    }

    @Test
    fun signOutAndAccountSwitchClearPriorUsersRealtimeData() = runBlocking {
        val userA = registerUser("account-a")
        val incomeA = lkrIncome("income-a-${uniqueSuffix()}", LocalDate.of(2026, 10, 8))
        assertSuccess(saveAndTrack(incomeA))

        val probe = FlowProbe(
            this,
            repository.observeIncomes(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1)
            )
        )
        assertEquals(listOf(incomeA), probe.awaitValue { it == listOf(incomeA) })

        assertSuccess(authRepository.signOut())
        assertEquals(emptyList<Income>(), probe.awaitValue { it.isEmpty() })
        val signedOutSave = repository.saveIncome(
            lkrIncome("signed-out-${uniqueSuffix()}", LocalDate.of(2026, 10, 9))
        )
        val signedOutDelete = repository.deleteIncome(incomeA.id)
        assertTrue(signedOutSave.exceptionOrNull() is IllegalStateException)
        assertTrue(signedOutDelete.exceptionOrNull() is IllegalStateException)

        val userB = registerUser("account-b")
        assertEquals(
            emptyList<Income>(),
            probe.awaitValue(assertEvery = { values ->
                assertTrue("User A data leaked after switching to B", values.none { it.id == incomeA.id })
            }) { it.isEmpty() }
        )
        val incomeB = lkrIncome("income-b-${uniqueSuffix()}", LocalDate.of(2026, 10, 9))
        assertSuccess(saveAndTrack(incomeB))
        assertEquals(
            listOf(incomeB),
            probe.awaitValue(assertEvery = { values ->
                assertTrue("User A data leaked to B", values.none { it.id == incomeA.id })
            }) { it == listOf(incomeB) }
        )

        assertEquals(userA.uid, authRepository.login(userA.email, userA.password).getOrThrow())
        assertEquals(listOf(incomeA), probe.awaitValue { it == listOf(incomeA) })
        assertSuccess(authRepository.signOut())
        assertEquals(userB.uid, authRepository.login(userB.email, userB.password).getOrThrow())
        assertEquals(
            emptyList<Income>(),
            probe.awaitValue(assertEvery = { values ->
                assertTrue("A stale emission crossed the rapid switch", values.none { it.id == incomeA.id })
            }) { it.isEmpty() }
        )
        assertEquals(
            listOf(incomeB),
            probe.awaitValue(assertEvery = { values ->
                assertTrue("A stale emission crossed the rapid switch", values.none { it.id == incomeA.id })
            }) { it == listOf(incomeB) }
        )

        probe.cancelAndJoin()

        assertEquals(userA.uid, authRepository.login(userA.email, userA.password).getOrThrow())
        assertCrossUserAccessDenied(userB, incomeB)
    }

    @Test
    fun firestoreRulesRejectMalformedIncomeDocumentsFromAndroidSdk() = runBlocking {
        val user = registerUser("rules")
        val invalidCases = listOf<Pair<String, (MutableMap<String, Any?>) -> Unit>>(
            "embedded-id" to { it["id"] = "different-id" },
            "unknown-field" to { it["unexpected"] = true },
            "missing-money-field" to { mutableMoney(it).remove("rateDate") },
            "incorrect-money-type" to { it["money"] = "not-a-map" },
            "unsupported-source" to { it["source"] = "BONUS" },
            "crypto-entry-kind" to {
                it["source"] = "CRYPTO"
                it["entryKind"] = "RECEIPT"
                it["pnlDirection"] = "GAIN"
            },
            "crypto-missing-pnl" to {
                it["source"] = "CRYPTO"
                it["entryKind"] = "REALIZED_PNL"
                it.remove("pnlDirection")
            },
            "currency-rate-source" to {
                mutableMoney(it)["rateSource"] = "MANUAL_REFERENCE"
            },
            "lkr-above-one" to {
                mutableMoney(it)["rateLkrPerUnitDecimal"] = "1.0000000000000001"
            },
            "lkr-below-one" to {
                mutableMoney(it)["rateLkrPerUnitDecimal"] = "0.99999999999999999"
            }
        )

        invalidCases.forEach { (caseName, mutation) ->
            val documentId = "$caseName-${uniqueSuffix()}"
            val fields = mutableDocument(lkrIncome(documentId, LocalDate.of(2026, 10, 20)))
            mutation(fields)
            expectPermissionDenied {
                incomeDocument(user.uid, documentId).set(fields).awaitTask()
            }
        }

        assertTrue(incomeCollection(user.uid).get(Source.SERVER).awaitTask().isEmpty)
    }

    @Test
    fun offlineFirestoreTransactionDoesNotReportDeletionSuccess() = runBlocking {
        val user = registerUser("offline")
        val income = lkrIncome("offline-${uniqueSuffix()}", LocalDate.of(2026, 10, 20))
        assertSuccess(saveAndTrack(income))
        assertTrue(incomeDocument(user.uid, income.id).get(Source.SERVER).awaitTask().exists())

        // Firestore 26.6.0 transactions use direct lookup/commit RPCs that are not stopped by
        // disableNetwork(). Pin a separate, production-identical repository to a verified-closed
        // local emulator port so the transaction itself observes unavailable backend transport.
        requireUnavailableLocalEndpoint(UNAVAILABLE_FIRESTORE_EMULATOR_PORT)
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val unavailableApp = checkNotNull(
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(TEST_PROJECT_ID)
                    .setApplicationId(UNAVAILABLE_TEST_APPLICATION_ID)
                    .setApiKey(UNAVAILABLE_TEST_API_KEY)
                    .build(),
                "income-unavailable-${uniqueSuffix()}"
            )
        )
        var unavailableAuth: FirebaseAuth? = null
        var unavailableFirestore: FirebaseFirestore? = null
        try {
            assertEquals(TEST_PROJECT_ID, unavailableApp.options.projectId)
            assertNotEquals(FirebaseApp.DEFAULT_APP_NAME, unavailableApp.name)

            val isolatedAuth = FirebaseAuth.getInstance(unavailableApp).also {
                it.useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)
            }
            unavailableAuth = isolatedAuth
            val isolatedFirestore = FirebaseFirestore.getInstance(unavailableApp).also {
                it.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                it.useEmulator(EMULATOR_HOST, UNAVAILABLE_FIRESTORE_EMULATOR_PORT)
            }
            unavailableFirestore = isolatedFirestore
            val unavailableAuthRepository = FirebaseAuthRepository(isolatedAuth)
            assertEquals(
                user.uid,
                unavailableAuthRepository.login(user.email, user.password).getOrThrow()
            )
            val unavailableRepository = FirestoreIncomeRepository(
                unavailableAuthRepository,
                isolatedFirestore
            )

            val result = withTimeout(TRANSACTION_FAILURE_TIMEOUT_MS) {
                unavailableRepository.deleteIncome(income.id)
            }
            assertTrue("Transport-unavailable transaction unexpectedly succeeded", result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue("Expected FirebaseFirestoreException but was $failure", failure is FirebaseFirestoreException)
            assertEquals(
                FirebaseFirestoreException.Code.UNAVAILABLE,
                (failure as FirebaseFirestoreException).code
            )
        } finally {
            runCatching { unavailableAuth?.signOut() }
            try {
                unavailableFirestore?.let { instance ->
                    withTimeout(CLEANUP_TIMEOUT_MS) {
                        instance.terminate().awaitTask()
                    }
                }
            } finally {
                unavailableApp.delete()
            }
        }

        assertTrue(incomeDocument(user.uid, income.id).get(Source.SERVER).awaitTask().exists())
    }

    private suspend fun assertCrossUserAccessDenied(userB: TestUser, incomeB: Income) {
        val bDocument = incomeDocument(userB.uid, incomeB.id)
        expectPermissionDenied { bDocument.get(Source.SERVER).awaitTask() }
        expectPermissionDenied {
            incomeCollection(userB.uid).whereEqualTo("id", incomeB.id)
                .get(Source.SERVER)
                .awaitTask()
        }

        val attackId = "attack-${uniqueSuffix()}"
        expectPermissionDenied {
            incomeDocument(userB.uid, attackId)
                .set(IncomeFirestoreMapper.toFirestoreMap(lkrIncome(attackId, incomeB.occurredDate)))
                .awaitTask()
        }
        expectPermissionDenied { bDocument.update("note", "cross-user update").awaitTask() }
        expectPermissionDenied { bDocument.delete().awaitTask() }
    }

    private suspend fun registerUser(label: String): TestUser {
        val email = "$label-${uniqueSuffix()}@example.test"
        val uid = authRepository.register(email, TEST_PASSWORD).getOrThrow()
        return TestUser(email, TEST_PASSWORD, uid).also(users::add)
    }

    private suspend fun saveAndTrack(income: Income): Result<Unit> {
        val uid = checkNotNull(auth.currentUser?.uid)
        return repository.saveIncome(income).also { result ->
            if (result.isSuccess) {
                documentIdsByUid.getOrPut(uid, ::mutableSetOf).add(income.id)
            }
        }
    }

    private fun incomeCollection(uid: String) = firestore
        .collection("users")
        .document(uid)
        .collection("incomes")

    private fun incomeDocument(uid: String, incomeId: String) =
        incomeCollection(uid).document(incomeId)

    private fun lkrIncome(
        id: String,
        occurredDate: LocalDate,
        projectReference: String? = null,
        note: String? = null
    ): Income = Income(
        id = id,
        source = IncomeSource.SALARY,
        entryKind = IncomeEntryKind.RECEIPT,
        money = MoneyConverter.fromLkr("1000.00", occurredDate).getOrThrow(),
        occurredDate = occurredDate,
        projectReference = projectReference,
        note = note
    )

    private fun mutableDocument(income: Income): MutableMap<String, Any?> =
        IncomeFirestoreMapper.toFirestoreMap(income)
            .mapValuesTo(linkedMapOf()) { it.value }

    private fun mutableMoney(document: MutableMap<String, Any?>): MutableMap<String, Any?> {
        val money = (document.getValue("money") as Map<*, *>)
            .entries
            .associateTo(linkedMapOf()) { (key, value) -> key as String to value }
        document["money"] = money
        return money
    }

    private suspend fun expectPermissionDenied(operation: suspend () -> Unit) {
        val failure = runCatching { operation() }.exceptionOrNull()
        assertNotNull("Expected Firestore permission-denied", failure)
        assertTrue("Expected FirebaseFirestoreException but was $failure", failure is FirebaseFirestoreException)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            (failure as FirebaseFirestoreException).code
        )
    }

    private fun assertSuccess(result: Result<Unit>) {
        assertTrue(result.exceptionOrNull()?.toString() ?: "Expected success", result.isSuccess)
    }

    private fun containsValue(value: Any?, expected: String): Boolean = when (value) {
        is Map<*, *> -> value.entries.any { (key, nested) ->
            key == expected || containsValue(nested, expected)
        }
        is Iterable<*> -> value.any { containsValue(it, expected) }
        else -> value == expected
    }

    private fun requireAndroidEmulator() {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        check(
            fingerprint.contains("generic") ||
                fingerprint.contains("emulator") ||
                model.contains("emulator") ||
                model.contains("sdk_gphone") ||
                product.contains("sdk_gphone")
        ) {
            "These tests require an Android Emulator because $EMULATOR_HOST is the host loopback alias"
        }
    }

    @Suppress("DEPRECATION")
    private fun requireLocalNetworkPermission() {
        if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_MIN_API) return

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext
        val packageName = context.packageName
        val requestedPermissions = context.packageManager
            .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()

        check(LOCAL_NETWORK_PERMISSION in requestedPermissions) {
            "$LOCAL_NETWORK_PERMISSION is not declared by target package $packageName"
        }

        try {
            instrumentation.uiAutomation.grantRuntimePermission(
                packageName,
                LOCAL_NETWORK_PERMISSION
            )
        } catch (exception: Exception) {
            throw AssertionError(
                "Unable to grant $LOCAL_NETWORK_PERMISSION to target package $packageName",
                exception
            )
        }

        check(
            context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            "$LOCAL_NETWORK_PERMISSION was not granted to target package $packageName"
        }
    }

    private fun requireReachableEmulator(port: Int, service: String) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(EMULATOR_HOST, port), PORT_CHECK_TIMEOUT_MS)
            }
        } catch (exception: Exception) {
            throw AssertionError(
                "$service Emulator is unavailable at $EMULATOR_HOST:$port; refusing Firebase access",
                exception
            )
        }
    }

    private fun requireUnavailableLocalEndpoint(port: Int) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(EMULATOR_HOST, port), PORT_CHECK_TIMEOUT_MS)
            }
        } catch (_: IOException) {
            return
        }
        throw AssertionError(
            "Expected unavailable local Firestore endpoint at $EMULATOR_HOST:$port"
        )
    }

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().replace("-", "")

    private data class TestUser(
        val email: String,
        val password: String,
        val uid: String
    )

    private class FlowProbe(
        scope: CoroutineScope,
        flow: Flow<List<Income>>
    ) {
        private val values = Channel<List<Income>>(Channel.UNLIMITED)
        private val collectionJob = scope.launch {
            try {
                flow.collect { values.send(it) }
            } finally {
                values.close()
            }
        }

        val isStopped: Boolean
            get() = collectionJob.isCompleted

        suspend fun awaitValue(
            assertEvery: (List<Income>) -> Unit = {},
            predicate: (List<Income>) -> Boolean
        ): List<Income> = withTimeout(OPERATION_TIMEOUT_MS) {
            while (true) {
                val value = values.receive()
                assertEvery(value)
                if (predicate(value)) return@withTimeout value
            }
            error("Unreachable")
        }

        suspend fun cancelAndJoin() {
            collectionJob.cancelAndJoin()
        }
    }

    private companion object {
        const val TEST_PROJECT_ID = "demo-personal-finance-income-rules"
        const val TEST_APPLICATION_ID = "1:1234567890:android:income-emulator-tests"
        const val TEST_API_KEY = "fake-api-key-for-local-emulators-only"
        const val UNAVAILABLE_TEST_APPLICATION_ID =
            "1:1234567890:android:income-unavailable-emulator-test"
        const val UNAVAILABLE_TEST_API_KEY = "fake-api-key-for-unavailable-emulator-only"
        const val TEST_PASSWORD = "emulator-password-123"
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_NETWORK_PERMISSION_MIN_API = 37
        const val EMULATOR_HOST = "10.0.2.2"
        const val AUTH_EMULATOR_PORT = 9099
        const val FIRESTORE_EMULATOR_PORT = 8080
        const val UNAVAILABLE_FIRESTORE_EMULATOR_PORT = 65_534
        const val PORT_CHECK_TIMEOUT_MS = 1_500
        const val OPERATION_TIMEOUT_MS = 15_000L
        const val TRANSACTION_FAILURE_TIMEOUT_MS = 30_000L
        const val CLEANUP_TIMEOUT_MS = 10_000L
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { completed ->
        if (!continuation.isActive) return@addOnCompleteListener
        when {
            completed.isCanceled -> continuation.cancel()
            completed.isSuccessful -> continuation.resume(completed.result)
            else -> continuation.resumeWithException(
                completed.exception ?: IllegalStateException("Firebase Task failed without an exception")
            )
        }
    }
}
