package com.example.personalfinanceapp.feature.offline

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.FirebaseAuthRepository
import com.example.personalfinanceapp.feature.expense.FirestoreExpenseRepository
import com.example.personalfinanceapp.feature.income.FirestoreIncomeRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import java.net.InetSocketAddress
import java.net.Socket
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M08 verification against local Firebase emulators only.
 *
 * Setup fails closed unless both 10.0.2.2 emulator endpoints are reachable. A non-default
 * FirebaseApp with a demo project ID prevents these tests from reading or writing live data.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreOfflineBehaviorEmulatorTest {

    private lateinit var appName: String
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authRepository: FirebaseAuthRepository
    private lateinit var incomeRepository: FirestoreIncomeRepository
    private lateinit var expenseRepository: FirestoreExpenseRepository

    private val users = mutableListOf<TestUser>()

    @Before
    fun setUp() {
        requireAndroidEmulator()
        requireLocalNetworkPermission()
        requireReachable(AUTH_PORT)
        requireReachable(FIRESTORE_PORT)
        appName = "m08-offline-${suffix()}"
        createClients()
    }

    @After
    fun tearDown() = runBlocking {
        if (!::firebaseApp.isInitialized) return@runBlocking

        withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
            runCatching { firestore.enableNetwork().awaitM08Task() }
            users.forEach { user ->
                if (authRepository.login(user.email, user.password).isSuccess) {
                    runCatching {
                        firestore.collection("users/${user.uid}/incomes")
                            .get(Source.SERVER)
                            .awaitM08Task()
                            .documents
                            .forEach { it.reference.delete().awaitM08Task() }
                    }
                    runCatching {
                        firestore.collection("users/${user.uid}/expenses")
                            .get(Source.SERVER)
                            .awaitM08Task()
                            .documents
                            .forEach { it.reference.delete().awaitM08Task() }
                    }
                    runCatching { auth.currentUser?.delete()?.awaitM08Task() }
                }
            }
        }
        terminateAndClearPersistence()
    }

    @Test
    fun onlineRecordsRemainCorrectInCacheOfflineAndServerErrorsAreNotEmptyData() = runBlocking {
        val user = register("cache")
        val income = income("cached-income-${suffix()}", "1250.25")
        val expense = expense("cached-expense-${suffix()}", "249.75")

        assertTrue(incomeRepository.saveIncome(income).isSuccess)
        assertTrue(expenseRepository.saveExpense(expense).isSuccess)

        val onlineIncomes = withTimeout(TIMEOUT_MS) {
            incomeRepository.observeIncomes(MONTH_START, MONTH_END).first { it.isNotEmpty() }
        }
        val onlineExpenses = withTimeout(TIMEOUT_MS) {
            expenseRepository.observeExpenses(MONTH_START, MONTH_END).first { it.isNotEmpty() }
        }
        val onlineBalance = balance(onlineIncomes, onlineExpenses)
        assertEquals(income.money.baseAmountMinor - expense.money.baseAmountMinor, onlineBalance)

        firestore.disableNetwork().awaitM08Task()

        val cachedIncomes = withTimeout(TIMEOUT_MS) {
            incomeRepository.observeIncomes(MONTH_START, MONTH_END).first { it.isNotEmpty() }
        }
        val cachedExpenses = withTimeout(TIMEOUT_MS) {
            expenseRepository.observeExpenses(MONTH_START, MONTH_END).first { it.isNotEmpty() }
        }
        assertEquals(listOf(income), cachedIncomes)
        assertEquals(listOf(expense), cachedExpenses)
        assertEquals(onlineBalance, balance(cachedIncomes, cachedExpenses))

        val genuinelyEmpty = firestore.collection("users/${user.uid}/incomes")
            .whereGreaterThanOrEqualTo("occurredDate", "2030-01-01")
            .whereLessThan("occurredDate", "2030-02-01")
            .get(Source.CACHE)
            .awaitM08Task()
        assertTrue(genuinelyEmpty.isEmpty)

        val serverFailure = runCatching {
            withTimeout(TIMEOUT_MS) {
                firestore.document("users/${user.uid}/incomes/${income.id}")
                    .get(Source.SERVER)
                    .awaitM08Task()
            }
        }.exceptionOrNull()
        assertTrue(serverFailure is FirebaseFirestoreException)
        assertEquals(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            (serverFailure as FirebaseFirestoreException).code
        )

        firestore.enableNetwork().awaitM08Task()
        val reconnectedIncome = firestore.document("users/${user.uid}/incomes/${income.id}")
            .get(Source.SERVER)
            .awaitM08Task()
        val reconnectedExpense = firestore.document("users/${user.uid}/expenses/${expense.id}")
            .get(Source.SERVER)
            .awaitM08Task()
        assertEquals(income.money.baseAmountMinor, reconnectedIncome.getLong("money.baseAmountMinor"))
        assertEquals(expense.money.baseAmountMinor, reconnectedExpense.getLong("baseAmountMinor"))
    }

    @Test
    fun offlineCreatesStayPendingThenSynchronizeOnceWithoutChangingAmounts() = runBlocking {
        val user = register("pending")
        val income = income("pending-income-${suffix()}", "4100.10")
        val expense = expense("pending-expense-${suffix()}", "875.55")

        firestore.disableNetwork().awaitM08Task()
        val pendingIncome = async { incomeRepository.saveIncome(income) }
        val pendingExpense = async { expenseRepository.saveExpense(expense) }

        val cachedIncome = awaitCachedDocument(
            "users/${user.uid}/incomes/${income.id}"
        )
        val cachedExpense = awaitCachedDocument(
            "users/${user.uid}/expenses/${expense.id}"
        )
        assertTrue(cachedIncome.metadata.hasPendingWrites())
        assertTrue(cachedExpense.metadata.hasPendingWrites())
        assertFalse(pendingIncome.isCompleted)
        assertFalse(pendingExpense.isCompleted)

        firestore.enableNetwork().awaitM08Task()
        assertTrue(withTimeout(TIMEOUT_MS) { pendingIncome.await() }.isSuccess)
        assertTrue(withTimeout(TIMEOUT_MS) { pendingExpense.await() }.isSuccess)
        firestore.waitForPendingWrites().awaitM08Task()

        assertTrue(incomeRepository.saveIncome(income).isSuccess)
        assertTrue(expenseRepository.saveExpense(expense).isSuccess)
        firestore.waitForPendingWrites().awaitM08Task()

        val serverIncomes = firestore.collection("users/${user.uid}/incomes")
            .get(Source.SERVER)
            .awaitM08Task()
        val serverExpenses = firestore.collection("users/${user.uid}/expenses")
            .get(Source.SERVER)
            .awaitM08Task()
        assertEquals(1, serverIncomes.size())
        assertEquals(1, serverExpenses.size())
        assertEquals(
            income.money.baseAmountMinor,
            serverIncomes.single().getLong("money.baseAmountMinor")
        )
        assertEquals(expense.money.baseAmountMinor, serverExpenses.single().getLong("baseAmountMinor"))
    }

    @Test
    fun pendingOfflineWriteCannotMoveFromSignedOutAccountAToAccountB() = runBlocking {
        val userA = register("owner-a")
        val pending = income("owner-a-pending-${suffix()}", "700.00")

        firestore.disableNetwork().awaitM08Task()
        val pendingSave: Deferred<Result<Unit>> = async {
            incomeRepository.saveIncomeForOwner(userA.uid, pending)
        }
        awaitCachedDocument("users/${userA.uid}/incomes/${pending.id}")
        assertFalse(pendingSave.isCompleted)

        assertTrue(authRepository.signOut().isSuccess)
        val userB = register("owner-b")
        val accountBValues = withTimeout(TIMEOUT_MS) {
            incomeRepository.observeIncomes(MONTH_START, MONTH_END).first()
        }
        assertTrue(accountBValues.none { it.id == pending.id })
        val accountBCacheLookup = runCatching {
            firestore.document("users/${userB.uid}/incomes/${pending.id}")
                .get(Source.CACHE)
                .awaitM08Task()
        }
        assertTrue(
            accountBCacheLookup.fold(
                onSuccess = { !it.exists() },
                onFailure = {
                    it is FirebaseFirestoreException &&
                        it.code == FirebaseFirestoreException.Code.UNAVAILABLE
                }
            )
        )

        firestore.enableNetwork().awaitM08Task()
        val completionWhileBIsAuthenticated = withTimeoutOrNull(1_500L) {
            pendingSave.await()
        }
        assertTrue(
            firestore.collection("users/${userB.uid}/incomes")
                .get(Source.SERVER)
                .awaitM08Task()
                .isEmpty
        )

        assertTrue(authRepository.signOut().isSuccess)
        assertEquals(userA.uid, authRepository.login(userA.email, userA.password).getOrThrow())
        val eventualCompletion = completionWhileBIsAuthenticated ?: withTimeoutOrNull(TIMEOUT_MS) {
            pendingSave.await()
        }
        if (eventualCompletion?.isSuccess == true) {
            val synchronizedForA = firestore.document("users/${userA.uid}/incomes/${pending.id}")
                .get(Source.SERVER)
                .awaitM08Task()
            assertTrue(synchronizedForA.exists())
            assertEquals(
                pending.money.baseAmountMinor,
                synchronizedForA.getLong("money.baseAmountMinor")
            )
        }
        if (!pendingSave.isCompleted) pendingSave.cancelAndJoin()

        assertTrue(authRepository.signOut().isSuccess)
        assertEquals(userB.uid, authRepository.login(userB.email, userB.password).getOrThrow())
        assertTrue(
            firestore.collection("users/${userB.uid}/incomes")
                .get(Source.SERVER)
                .awaitM08Task()
                .isEmpty
        )
    }

    private fun createClients() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId(testApplicationId())
            .setApiKey(TEST_API_KEY)
            .build()
        firebaseApp = checkNotNull(FirebaseApp.initializeApp(context, options, appName))
        assertEquals(PROJECT_ID, firebaseApp.options.projectId)
        assertFalse(firebaseApp.name == FirebaseApp.DEFAULT_APP_NAME)

        auth = FirebaseAuth.getInstance(firebaseApp).apply {
            useEmulator(HOST, AUTH_PORT)
        }
        firestore = FirebaseFirestore.getInstance(firebaseApp).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(CACHE_SIZE_BYTES)
                        .build()
                )
                .build()
            useEmulator(HOST, FIRESTORE_PORT)
        }
        authRepository = FirebaseAuthRepository(auth)
        incomeRepository = FirestoreIncomeRepository(authRepository, firestore)
        expenseRepository = FirestoreExpenseRepository(firestore, auth)
    }

    private suspend fun terminateAndClearPersistence() {
        if (::firestore.isInitialized) {
            withTimeoutOrNull(CLEANUP_TIMEOUT_MS) {
                runCatching { firestore.terminate().awaitM08Task() }
                runCatching { firestore.clearPersistence().awaitM08Task() }
            }
        }
    }

    private suspend fun register(label: String): TestUser {
        val user = TestUser(
            email = "$label-${suffix()}@example.com",
            password = PASSWORD
        )
        val result = auth.createUserWithEmailAndPassword(user.email, user.password).awaitM08Task()
        val registered = user.copy(uid = checkNotNull(result.user?.uid))
        users += registered
        return registered
    }

    private suspend fun awaitCachedDocument(path: String) = withTimeout(TIMEOUT_MS) {
        while (true) {
            val snapshot = runCatching {
                firestore.document(path).get(Source.CACHE).awaitM08Task()
            }.getOrNull()
            if (snapshot?.exists() == true) return@withTimeout snapshot
            delay(25)
        }
        error("Unreachable")
    }

    private fun income(id: String, amount: String): Income = Income(
        id = id,
        source = IncomeSource.SALARY,
        entryKind = IncomeEntryKind.RECEIPT,
        money = MoneyConverter.fromLkr(amount, RECORD_DATE).getOrThrow(),
        occurredDate = RECORD_DATE,
        note = "M08 emulator verification"
    )

    private fun expense(id: String, amount: String): Expense = Expense(
        id = id,
        categoryId = "m08-test-category",
        categoryNameSnapshot = "M08 Test",
        spendingClass = SpendingClass.DISCRETIONARY,
        paymentChannel = PaymentChannel.CARD,
        money = MoneyConverter.fromLkr(amount, RECORD_DATE).getOrThrow(),
        occurredDate = RECORD_DATE,
        note = "M08 emulator verification"
    )

    private fun balance(incomes: List<Income>, expenses: List<Expense>): Long =
        incomes.sumOf { it.money.baseAmountMinor } - expenses.sumOf { it.money.baseAmountMinor }

    private fun requireAndroidEmulator() {
        val fingerprint = Build.FINGERPRINT.lowercase()
        check(
            fingerprint.contains("generic") ||
                fingerprint.contains("emulator") ||
                Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.HARDWARE.contains("goldfish", ignoreCase = true)
        ) { "M08 tests require an Android Emulator" }
    }

    @Suppress("DEPRECATION")
    private fun requireLocalNetworkPermission() {
        if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_MIN_API) return

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext
        val requestedPermissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
        check(LOCAL_NETWORK_PERMISSION in requestedPermissions) {
            "$LOCAL_NETWORK_PERMISSION is not declared by ${context.packageName}"
        }
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            LOCAL_NETWORK_PERMISSION
        )
        check(
            context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        ) { "$LOCAL_NETWORK_PERMISSION was not granted" }
    }

    private fun requireReachable(port: Int) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(HOST, port), 1_500)
        }
    }

    private fun suffix(): String = UUID.randomUUID().toString().replace("-", "")

    private fun testApplicationId(): String =
        "1:1234567890:android:${appName.removePrefix("m08-offline-")}"

    private data class TestUser(
        val email: String,
        val password: String,
        val uid: String = ""
    )

    private companion object {
        const val PROJECT_ID = "demo-personal-finance-income-rules"
        const val TEST_API_KEY = "m08-local-emulator-key"
        const val HOST = "10.0.2.2"
        const val AUTH_PORT = 9099
        const val FIRESTORE_PORT = 8080
        const val PASSWORD = "emulator-password-123"
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_NETWORK_PERMISSION_MIN_API = 37
        const val CACHE_SIZE_BYTES = 100L * 1024L * 1024L
        const val TIMEOUT_MS = 15_000L
        const val CLEANUP_TIMEOUT_MS = 10_000L
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 11, 2)
        val MONTH_START: LocalDate = LocalDate.of(2026, 11, 1)
        val MONTH_END: LocalDate = LocalDate.of(2026, 12, 1)
    }
}

private suspend fun <T> Task<T>.awaitM08Task(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            when {
                task.isCanceled -> continuation.cancel()
                task.isSuccessful -> continuation.resume(task.result)
                else -> continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed")
                )
            }
        }
    }
