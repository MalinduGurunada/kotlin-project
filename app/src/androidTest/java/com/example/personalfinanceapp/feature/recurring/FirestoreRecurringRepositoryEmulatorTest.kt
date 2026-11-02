package com.example.personalfinanceapp.feature.recurring

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import java.net.InetSocketAddress
import java.net.Socket
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreRecurringRepositoryEmulatorTest {

    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: FirestoreRecurringRepository

    @Before
    fun setUp() {
        requireAndroidEmulator()
        requireReachable(AUTH_PORT)
        requireReachable(FIRESTORE_PORT)
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:1234567890:android:recurring-emulator-tests")
            .setApiKey("local-emulator-key")
            .build()
        app = checkNotNull(
            FirebaseApp.initializeApp(context, options, "recurring-emulator-${suffix()}")
        )
        auth = FirebaseAuth.getInstance(app).apply { useEmulator(HOST, AUTH_PORT) }
        firestore = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            useEmulator(HOST, FIRESTORE_PORT)
        }
        repository = FirestoreRecurringRepository(firestore, auth)
    }

    @After
    fun tearDown() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::firestore.isInitialized) {
            runCatching { firestore.terminate().awaitRecurringTask() }
        }
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun confirmationAndSkipAreAtomicIdempotentAndPersistent() = runBlocking {
        val userId = register()
        seedCategory(userId)
        val recurring = recurringExpense()
        assertTrue(repository.saveRecurringExpense(recurring).isSuccess)
        val dueDate = LocalDate.of(2026, 10, 31)

        val first = repository.confirmPayment(
            recurringExpenseId = recurring.id,
            dueDate = dueDate,
            paidDate = dueDate,
            amountMinor = recurring.expectedAmountMinor
        )
        val retry = repository.confirmPayment(
            recurringExpenseId = recurring.id,
            dueDate = dueDate,
            paidDate = dueDate,
            amountMinor = recurring.expectedAmountMinor
        )

        assertTrue(first.isSuccess)
        assertEquals(first.getOrThrow(), retry.getOrThrow())
        val expenseId = first.getOrThrow()
        val expense = firestore.document("users/$userId/expenses/$expenseId")
            .get(Source.SERVER)
            .awaitRecurringTask()
        assertTrue(expense.exists())
        assertEquals(recurring.id, expense.getString("recurringExpenseId"))

        val recreated = FirestoreRecurringRepository(firestore, auth)
        val occurrence = withTimeout(TIMEOUT_MS) {
            recreated.observeOccurrences().first { it.isNotEmpty() }.single()
        }
        assertEquals(RecurringOccurrenceStatus.CONFIRMED, occurrence.status)
        assertEquals(expenseId, occurrence.expenseId)

        val expenses = firestore.collection("users/$userId/expenses")
            .get(Source.SERVER)
            .awaitRecurringTask()
        assertEquals(1, expenses.size())

        val skippedRecurring = recurringExpense()
        assertTrue(repository.saveRecurringExpense(skippedRecurring).isSuccess)
        assertTrue(repository.skipOccurrence(skippedRecurring.id, dueDate).isSuccess)
        val confirmation = repository.confirmPayment(
            recurringExpenseId = skippedRecurring.id,
            dueDate = dueDate,
            paidDate = dueDate,
            amountMinor = skippedRecurring.expectedAmountMinor
        )

        assertTrue(confirmation.isFailure)
        val expensesAfterSkip = firestore.collection("users/$userId/expenses")
            .get(Source.SERVER)
            .awaitRecurringTask()
        assertEquals(1, expensesAfterSkip.size())
    }

    private suspend fun register(): String {
        val result = auth.createUserWithEmailAndPassword(
            "recurring-${suffix()}@example.com",
            PASSWORD
        ).awaitRecurringTask()
        return checkNotNull(result.user?.uid)
    }

    private suspend fun seedCategory(userId: String) {
        firestore.document("users/$userId/categories/bills")
            .set(
                mapOf(
                    "id" to "bills",
                    "name" to "Bills",
                    "defaultSpendingClass" to "COMMITTED",
                    "archived" to false
                )
            )
            .awaitRecurringTask()
    }

    private fun recurringExpense() = RecurringExpense(
        id = "electricity-${suffix()}",
        title = "Electricity",
        categoryId = "bills",
        expectedAmountMinor = 12_500_00L,
        frequency = RecurrenceFrequency.MONTHLY,
        anchorDate = LocalDate.of(2026, 10, 31),
        startDate = LocalDate.of(2026, 10, 31),
        reminderEnabled = true,
        reminderLeadDays = 2L
    )

    private fun suffix() = UUID.randomUUID().toString().replace("-", "")

    private fun requireAndroidEmulator() {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val isEmulator =
            fingerprint.contains("generic") ||
                fingerprint.contains("emulator") ||
                Build.MODEL.contains("emulator", ignoreCase = true) ||
                Build.MODEL.contains("android sdk", ignoreCase = true) ||
                Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.HARDWARE.contains("goldfish", ignoreCase = true)
        check(isEmulator) {
            "Recurring rules tests require an Android Emulator"
        }
    }

    private fun requireReachable(port: Int) {
        Socket().use { it.connect(InetSocketAddress(HOST, port), 1_500) }
    }

    private companion object {
        const val PROJECT_ID = "demo-personal-finance-income-rules"
        const val HOST = "127.0.0.1"
        const val AUTH_PORT = 9099
        const val FIRESTORE_PORT = 8080
        const val PASSWORD = "emulator-password-123"
        const val TIMEOUT_MS = 15_000L
    }
}

private suspend fun <T> Task<T>.awaitRecurringTask(): T =
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
