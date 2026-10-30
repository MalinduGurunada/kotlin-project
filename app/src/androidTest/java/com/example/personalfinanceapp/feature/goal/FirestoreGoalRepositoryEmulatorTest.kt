package com.example.personalfinanceapp.feature.goal

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RunWith(AndroidJUnit4::class)
class FirestoreGoalRepositoryEmulatorTest {

    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authRepository: FirebaseAuthRepository
    private lateinit var repository: FirestoreGoalRepository

    @Before
    fun setUp() {
        requireAndroidEmulator()
        requireReachable(AUTH_PORT)
        requireReachable(FIRESTORE_PORT)
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val options = FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId("1:1234567890:android:goal-emulator-tests")
            .setApiKey("local-emulator-key")
            .build()
        app = checkNotNull(
            FirebaseApp.initializeApp(context, options, "goal-emulator-${suffix()}")
        )
        auth = FirebaseAuth.getInstance(app).apply { useEmulator(HOST, AUTH_PORT) }
        firestore = FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            useEmulator(HOST, FIRESTORE_PORT)
        }
        authRepository = FirebaseAuthRepository(auth)
        repository = FirestoreGoalRepository(authRepository, firestore)
    }

    @After
    fun tearDown() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::firestore.isInitialized) runCatching { firestore.terminate().awaitGoalTask() }
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun persistenceRulesAndIdenticalIdsRemainUserScoped() = runBlocking {
        val sharedGoalId = "goal-${suffix()}"
        val sharedContributionId = "contribution-${suffix()}"
        val userA = register("a")
        val goalA = goal(sharedGoalId, "User A goal")
        val contributionA = contribution(sharedContributionId, sharedGoalId, 12_345L)
        assertTrue(repository.saveGoalForOwner(userA, goalA).isSuccess)
        assertTrue(repository.addContributionForOwner(userA, contributionA).isSuccess)

        val recreated = FirestoreGoalRepository(authRepository, firestore)
        assertEquals(goalA, withTimeout(TIMEOUT_MS) {
            recreated.observeGoals().first { it.isNotEmpty() }.single()
        })
        assertEquals(contributionA, withTimeout(TIMEOUT_MS) {
            recreated.observeContributions(sharedGoalId).first { it.isNotEmpty() }.single()
        })

        auth.signOut()
        val userB = register("b")
        expectPermissionDenied {
            firestore.document("users/$userA/goals/$sharedGoalId")
                .get(Source.SERVER)
                .awaitGoalTask()
        }
        assertTrue(repository.observeGoals().first().isEmpty())

        val goalB = goal(sharedGoalId, "User B goal")
        val contributionB = contribution(sharedContributionId, sharedGoalId, 99_999L)
        assertTrue(repository.saveGoalForOwner(userB, goalB).isSuccess)
        assertTrue(repository.addContributionForOwner(userB, contributionB).isSuccess)
        assertEquals("User B goal", withTimeout(TIMEOUT_MS) {
            repository.observeGoals().first { it.isNotEmpty() }.single().name
        })

        expectPermissionDenied {
            firestore.document("users/$userA/goals/forged-${suffix()}")
                .set(GoalFirestoreMapper.toFirestoreMap(goal("forged-${suffix()}", "Forged")))
                .awaitGoalTask()
        }
        expectPermissionDenied {
            firestore.document("users/$userB/goals/malformed-${suffix()}")
                .set(mapOf("id" to "malformed"))
                .awaitGoalTask()
        }
    }

    private suspend fun register(label: String): String {
        val result = auth.createUserWithEmailAndPassword(
            "goal-$label-${suffix()}@example.com",
            PASSWORD
        ).awaitGoalTask()
        return checkNotNull(result.user?.uid)
    }

    private suspend fun expectPermissionDenied(operation: suspend () -> Unit) {
        val failure = runCatching { operation() }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(failure is FirebaseFirestoreException)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            (failure as FirebaseFirestoreException).code
        )
    }

    private fun goal(id: String, name: String) = Goal(
        id = id,
        name = name,
        targetAmountMinor = 1_000_000L,
        startDate = LocalDate.of(2026, 10, 1),
        targetDate = LocalDate.of(2027, 10, 1)
    )

    private fun contribution(id: String, goalId: String, amount: Long) = Contribution(
        id = id,
        goalId = goalId,
        amountMinor = amount,
        kind = ContributionKind.ALLOCATION,
        occurredDate = LocalDate.of(2026, 10, 20)
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
            "Goal rules tests require an Android Emulator"
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

private suspend fun <T> Task<T>.awaitGoalTask(): T =
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
