package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.feature.auth.ui.AuthNavigationState
import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var pendingStore: InMemoryPendingRegistrationStore
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        pendingStore = InMemoryPendingRegistrationStore()
        viewModel = AuthViewModel(fakeAuthRepository, pendingStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialAuthStateIsNull() = runTest {
        assertNull(viewModel.authState.value)
    }

    @Test
    fun loginWithInvalidEmailShowsError() = runTest {
        viewModel.onLoginEmailChanged("invalid-email")
        viewModel.onLoginPasswordChanged("password123")
        viewModel.login()

        val state = viewModel.loginUiState.value
        assertEquals("Please enter a valid email address", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun loginWithShortPasswordShowsError() = runTest {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("12345")
        viewModel.login()

        val state = viewModel.loginUiState.value
        assertEquals("Password must be at least 6 characters", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun existingUserWithoutDisplayNameCanSignInNormally() = runTest {
        val repository = FakeAuthRepository(
            initialUserId = "legacy-user",
            initialEmail = "legacy@example.com"
        )
        val restartedViewModel = AuthViewModel(repository, pendingStore)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.Authenticated("legacy-user"),
            restartedViewModel.navigationState.value
        )
        assertNull(repository.displayName)
    }

    @Test
    fun loginSuccessUpdatesAuthState() = runTest {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password123")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val userId = fakeAuthRepository.observeAuthState().first()
        assertEquals("user_user_at_example_com", userId)
        assertEquals(
            AuthNavigationState.Authenticated(userId!!),
            viewModel.navigationState.value
        )
    }

    @Test
    fun registerWithMismatchedPasswordsShowsError() = runTest {
        fillValidRegistration()
        viewModel.onRegisterConfirmPasswordChanged("different123")

        viewModel.register()

        assertEquals(
            "Passwords do not match",
            viewModel.registerUiState.value.errorMessage
        )
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun normalRegistrationPersistsNameAndClearsPendingMarker() = runTest {
        fillValidRegistration(fullName = "  New User  ")

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val userId = fakeAuthRepository.observeAuthState().first()
        assertEquals("user_newuser_at_example_com", userId)
        assertEquals("New User", fakeAuthRepository.displayName)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertEquals(
            AuthNavigationState.Authenticated(userId!!),
            viewModel.navigationState.value
        )
    }

    @Test
    fun registerWithBlankFullNameShowsErrorWithoutCreatingAccount() = runTest {
        fillValidRegistration(fullName = "   ")

        viewModel.register()

        assertEquals(
            "Please enter your full name",
            viewModel.registerUiState.value.errorMessage
        )
        assertEquals(0, fakeAuthRepository.registerCallCount)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun registrationFailureClearsPersistedIntent() = runTest {
        fakeAuthRepository.shouldFailRegister = true
        fillValidRegistration()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeAuthRepository.registerCallCount)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertFalse(viewModel.registerUiState.value.isProfileUpdatePending)
        assertNull(fakeAuthRepository.observeAuthState().first())
    }

    @Test
    fun profileFailureSurvivesRestartAndRetryDoesNotCreateAnotherAccount() = runTest {
        fakeAuthRepository.shouldFailProfileUpdate = true
        fillValidRegistration()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val createdUserId = fakeAuthRepository.observeAuthState().first()
        assertEquals(1, fakeAuthRepository.registerCallCount)
        assertTrue(viewModel.registerUiState.value.isAccountCreated)
        assertEquals(createdUserId, pendingStore.validRegistration()?.userId)
        assertEquals(
            AuthNavigationState.RegistrationSetup,
            viewModel.navigationState.value
        )

        val restartedRepository = FakeAuthRepository(
            initialUserId = createdUserId,
            initialEmail = "newuser@example.com"
        )
        val restartedViewModel = AuthViewModel(restartedRepository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )
        assertTrue(restartedViewModel.registerUiState.value.isAccountCreated)

        restartedViewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, restartedRepository.registerCallCount)
        assertEquals("New User", restartedRepository.displayName)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertEquals(
            AuthNavigationState.Authenticated(createdUserId!!),
            restartedViewModel.navigationState.value
        )
    }

    @Test
    fun crashWindowMarkerIsBoundToCreatedFirebaseAccountOnRestart() = runTest {
        pendingStore.save(
            PendingRegistration(
                email = "newuser@example.com",
                fullName = "New User"
            )
        )
        val createdUserId = "user_newuser_at_example_com"
        val restartedRepository = FakeAuthRepository(
            initialUserId = createdUserId,
            initialEmail = "newuser@example.com"
        )
        val restartedViewModel = AuthViewModel(restartedRepository, pendingStore)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(createdUserId, pendingStore.validRegistration()?.userId)
        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )

        restartedViewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, restartedRepository.registerCallCount)
        assertEquals("New User", restartedRepository.displayName)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun preCreationIntentRestoresRegistrationAndRequiresPasswordAgain() = runTest {
        pendingStore.save(
            PendingRegistration(
                email = "newuser@example.com",
                fullName = "New User"
            )
        )
        val restartedViewModel = AuthViewModel(
            FakeAuthRepository(),
            pendingStore
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )
        val state = restartedViewModel.registerUiState.value
        assertFalse(state.isAccountCreated)
        assertEquals("newuser@example.com", state.emailInput)
        assertEquals("", state.passwordInput)
    }

    @Test
    fun uidBoundMarkerSurvivesSignedOutRestartAndResumesForSameAccount() = runTest {
        val userId = "user_newuser_at_example_com"
        pendingStore.save(
            PendingRegistration(
                email = "newuser@example.com",
                fullName = "New User",
                userId = userId
            )
        )
        val repository = FakeAuthRepository()
        val restartedViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.Unauthenticated,
            restartedViewModel.navigationState.value
        )
        assertEquals(userId, pendingStore.validRegistration()?.userId)

        restartedViewModel.onLoginEmailChanged("newuser@example.com")
        restartedViewModel.onLoginPasswordChanged("password123")
        restartedViewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )
        assertTrue(restartedViewModel.registerUiState.value.isAccountCreated)
    }

    @Test
    fun pendingMarkerForDifferentAccountIsClearedWithoutBlockingUser() = runTest {
        pendingStore.save(
            PendingRegistration(
                email = "first@example.com",
                fullName = "First User",
                userId = "first-user"
            )
        )
        val repository = FakeAuthRepository(
            initialUserId = "second-user",
            initialEmail = "second@example.com"
        )
        val restartedViewModel = AuthViewModel(repository, pendingStore)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertEquals(
            AuthNavigationState.Authenticated("second-user"),
            restartedViewModel.navigationState.value
        )
    }

    @Test
    fun duplicateRegisterSubmitWhileLoadingCreatesOneAccount() = runTest {
        fillValidRegistration()

        viewModel.register()
        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeAuthRepository.registerCallCount)
        assertEquals("New User", fakeAuthRepository.displayName)
    }

    @Test
    fun abandoningFailedProfileSetupSignsOutAndClearsPendingState() = runTest {
        fakeAuthRepository.shouldFailProfileUpdate = true
        fillValidRegistration()
        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.abandonPendingRegistration()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeAuthRepository.observeAuthState().first())
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertEquals(
            AuthNavigationState.Unauthenticated,
            viewModel.navigationState.value
        )
        assertFalse(viewModel.registerUiState.value.isProfileUpdatePending)
    }

    @Test
    fun passwordResetSuccessUpdatesState() = runTest {
        viewModel.onResetEmailChanged("user@example.com")
        viewModel.sendPasswordReset()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.passwordResetUiState.value
        assertTrue(state.isSuccess)
        assertNull(state.errorMessage)
    }

    @Test
    fun signOutClearsAuthAndPendingRegistration() = runTest {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password123")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()
        pendingStore.save(
            PendingRegistration(
                email = "user@example.com",
                fullName = "User",
                userId = "user_user_at_example_com"
            )
        )

        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeAuthRepository.observeAuthState().first())
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
        assertEquals(
            AuthNavigationState.Unauthenticated,
            viewModel.navigationState.value
        )
    }

    @Test
    fun delayedProfileUpdateAfterSignOutCannotPublishAuthenticated() = runTest {
        val repository = delayedProfileRepository()
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        recoveringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        assertTrue(repository.updateStarted.isCompleted)

        recoveringViewModel.signOut()
        testDispatcher.scheduler.runCurrent()
        repository.releaseUpdate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.Unauthenticated, recoveringViewModel.navigationState.value)
        assertNull(repository.currentUserId())
    }

    @Test
    fun delayedProfileUpdateAfterAccountSwitchCannotAuthenticateOriginalUid() = runTest {
        val repository = delayedProfileRepository()
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        recoveringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        repository.switchUser("other-user", "other@example.com")
        testDispatcher.scheduler.runCurrent()
        repository.releaseUpdate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.Authenticated("other-user"),
            recoveringViewModel.navigationState.value
        )
        assertEquals(1, repository.updateCallCount)
    }

    @Test
    fun abandonmentInvalidatesDelayedProfileCompletion() = runTest {
        val repository = delayedProfileRepository()
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        recoveringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        recoveringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        repository.releaseUpdate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.Unauthenticated, recoveringViewModel.navigationState.value)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun abandonmentWhileAccountCreationIsDelayedCannotResumeRegistration() = runTest {
        val repository = DelayedRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        assertTrue(repository.registerStarted.isCompleted)

        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        assertEquals(AuthNavigationState.Unauthenticated, registeringViewModel.navigationState.value)

        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.RegistrationSetup, registeringViewModel.navigationState.value)
        assertEquals("delayed-user", repository.currentUserId())
        assertEquals(0, repository.profileUpdateCallCount)
        assertEquals(
            PendingRegistrationPhase.Abandoned,
            pendingStore.validRegistration()?.phase
        )
    }

    @Test
    fun signOutWhileAccountCreationIsDelayedCannotResumeRegistration() = runTest {
        val repository = DelayedRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        registeringViewModel.signOut()
        testDispatcher.scheduler.runCurrent()

        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.RegistrationSetup, registeringViewModel.navigationState.value)
        assertEquals("delayed-user", repository.currentUserId())
        assertEquals(0, repository.profileUpdateCallCount)
        assertEquals(
            PendingRegistrationPhase.Abandoned,
            pendingStore.validRegistration()?.phase
        )
    }

    @Test
    fun normalDelayedAccountCreationCompletesProfileAndAuthenticates() = runTest {
        val repository = DelayedRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.Authenticated("delayed-user"),
            registeringViewModel.navigationState.value
        )
        assertEquals(1, repository.profileUpdateCallCount)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun delayedRegistrationFailureAfterAbandonmentCannotRestoreOldErrorOrMarker() = runTest {
        val repository = DelayedRegistrationAuthRepository().apply {
            shouldFailRegistration = true
        }
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()

        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.Unauthenticated, registeringViewModel.navigationState.value)
        assertNull(registeringViewModel.registerUiState.value.errorMessage)
        assertEquals(PendingRegistrationState.Absent, pendingStore.read())
    }

    @Test
    fun delayedAccountCreationAfterAccountSwitchCannotOverwriteOtherAccountState() = runTest {
        val repository = DelayedRegistrationAuthRepository().apply {
            completeAsCurrentUser = false
        }
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        repository.switchUser("other-user", "other@example.com")
        testDispatcher.scheduler.runCurrent()

        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.Authenticated("other-user"),
            registeringViewModel.navigationState.value
        )
        assertEquals("other-user", repository.currentUserId())
        assertEquals(0, repository.profileUpdateCallCount)
        assertEquals(
            PendingRegistrationPhase.Abandoned,
            pendingStore.validRegistration()?.phase
        )
    }

    @Test
    fun abandonedInFlightCreationBlocksNewerRegistrationUntilResolved() = runTest {
        val repository = SupersedingRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(
            registeringViewModel,
            fullName = "First User",
            email = "first@example.com"
        )

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        assertTrue(repository.firstRegistrationStarted.isCompleted)

        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        fillValidRegistration(
            registeringViewModel,
            fullName = "Second User",
            email = "second@example.com"
        )
        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        assertFalse(repository.secondRegistrationStarted.isCompleted)
        assertEquals(
            PendingRegistrationPhase.Abandoned,
            pendingStore.validRegistration()?.phase
        )

        repository.releaseFirstRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            registeringViewModel.navigationState.value
        )
        assertEquals(0, repository.profileUpdateCallCount)
    }

    @Test
    fun staleCleanupCannotSignOutAccountThatReplacesCreatedUserAfterUidCheck() = runTest {
        val repository = CleanupRaceRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.runCurrent()
        val signOutCallsAfterAbandonment = repository.signOutCallCount

        repository.switchUser("other-user", "other@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(signOutCallsAfterAbandonment, repository.signOutCallCount)
        assertEquals("other-user", repository.currentUserId())
        assertEquals(
            AuthNavigationState.Authenticated("other-user"),
            registeringViewModel.navigationState.value
        )
    }

    @Test
    fun restartWhileStaleCreationCleanupIsBlockedCannotAuthenticateAbandonedAccount() = runTest {
        val repository = CleanupRaceRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.runCurrent()

        val restartedViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.runCurrent()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )
    }

    @Test
    fun cleanupFailureRemainsProtectedAcrossViewModelRestart() = runTest {
        val repository = CleanupRaceRegistrationAuthRepository(failSignOut = true)
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.advanceUntilIdle()

        val restartedViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            restartedViewModel.navigationState.value
        )
    }

    @Test
    fun abandonmentPersistenceFailureStillInvalidatesAccountCreation() = runTest {
        val repository = DelayedRegistrationAuthRepository()
        val registeringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidRegistration(registeringViewModel)

        registeringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        pendingStore.failSave = true
        registeringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.runCurrent()
        pendingStore.failSave = false

        repository.releaseRegistration.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            registeringViewModel.navigationState.value
        )
        assertEquals(0, repository.profileUpdateCallCount)
    }

    @Test
    fun signingOutUnrelatedAccountPreservesAbandonedRegistrationMarker() = runTest {
        pendingStore.save(
            PendingRegistration(
                email = "abandoned@example.com",
                fullName = "Abandoned User",
                userId = "abandoned-user",
                phase = PendingRegistrationPhase.Abandoned
            )
        )
        val repository = FakeAuthRepository(
            initialUserId = "other-user",
            initialEmail = "other@example.com"
        )
        val signedInViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            AuthNavigationState.Authenticated("other-user"),
            signedInViewModel.navigationState.value
        )

        signedInViewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthNavigationState.Unauthenticated, signedInViewModel.navigationState.value)
        assertEquals(
            PendingRegistrationPhase.Abandoned,
            pendingStore.validRegistration()?.phase
        )
        assertEquals("abandoned-user", pendingStore.validRegistration()?.userId)
    }

    @Test
    fun duplicateRetryWhileProfileUpdateIsDelayedCannotRestoreStaleAuthentication() = runTest {
        val repository = delayedProfileRepository()
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        recoveringViewModel.register()
        recoveringViewModel.register()
        testDispatcher.scheduler.runCurrent()
        repository.switchUser("other-user", "other@example.com")
        repository.releaseUpdate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updateCallCount)
        assertEquals(
            AuthNavigationState.Authenticated("other-user"),
            recoveringViewModel.navigationState.value
        )
    }

    @Test
    fun corruptActiveMarkerBlocksAuthenticatedStartupUntilAbandoned() = runTest {
        pendingStore.forcedReadState = PendingRegistrationState.Corrupt
        val repository = FakeAuthRepository(
            initialUserId = "legacy-user",
            initialEmail = "legacy@example.com"
        )
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            recoveringViewModel.navigationState.value
        )
        assertTrue(recoveringViewModel.registerUiState.value.requiresRecoveryAbandonment)

        recoveringViewModel.abandonPendingRegistration()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthNavigationState.Unauthenticated, recoveringViewModel.navigationState.value)
    }

    @Test
    fun registrationIntentWriteFailureDoesNotCreateAccountOrAuthenticate() = runTest {
        pendingStore.failSave = true
        fillValidRegistration()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeAuthRepository.registerCallCount)
        assertEquals(AuthNavigationState.Unauthenticated, viewModel.navigationState.value)
    }

    @Test
    fun markerClearFailureAfterProfileSuccessRemainsInRegistrationSetup() = runTest {
        pendingStore.failClear = true
        fillValidRegistration()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            viewModel.navigationState.value
        )
        assertTrue(viewModel.registerUiState.value.isProfileUpdatePending)
        assertEquals("New User", fakeAuthRepository.displayName)
    }

    @Test
    fun staleMarkerClearFailureCannotAuthenticateDifferentAccount() = runTest {
        pendingStore.save(
            PendingRegistration(
                email = "first@example.com",
                fullName = "First User",
                userId = "first-user"
            )
        )
        pendingStore.failClear = true
        val repository = FakeAuthRepository(
            initialUserId = "second-user",
            initialEmail = "second@example.com"
        )
        val recoveringViewModel = AuthViewModel(repository, pendingStore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AuthNavigationState.RegistrationSetup,
            recoveringViewModel.navigationState.value
        )
    }

    private fun delayedProfileRepository(): DelayedProfileAuthRepository {
        val userId = "pending-user"
        pendingStore.save(
            PendingRegistration(
                email = "pending@example.com",
                fullName = "Pending User",
                userId = userId
            )
        )
        return DelayedProfileAuthRepository(userId, "pending@example.com")
    }

    private fun fillValidRegistration(fullName: String = "New User") {
        fillValidRegistration(viewModel, fullName)
    }

    private fun fillValidRegistration(
        targetViewModel: AuthViewModel,
        fullName: String = "New User",
        email: String = "newuser@example.com"
    ) {
        targetViewModel.onRegisterFullNameChanged(fullName)
        targetViewModel.onRegisterEmailChanged(email)
        targetViewModel.onRegisterPasswordChanged("password123")
        targetViewModel.onRegisterConfirmPasswordChanged("password123")
    }

    private class InMemoryPendingRegistrationStore(
        private var pendingRegistration: PendingRegistration? = null
    ) : PendingRegistrationStore {

        var forcedReadState: PendingRegistrationState? = null
        var failSave = false
        var failClear = false

        override fun read(): PendingRegistrationState {
            return forcedReadState ?: pendingRegistration?.let(PendingRegistrationState::Valid)
                ?: PendingRegistrationState.Absent
        }

        override fun save(pendingRegistration: PendingRegistration): Result<Unit> {
            if (failSave) return Result.failure(IllegalStateException("save failed"))
            this.pendingRegistration = pendingRegistration
            return Result.success(Unit)
        }

        override fun clear(): Result<Unit> {
            if (failClear) return Result.failure(IllegalStateException("clear failed"))
            pendingRegistration = null
            forcedReadState = null
            return Result.success(Unit)
        }

        fun validRegistration(): PendingRegistration? {
            return (read() as? PendingRegistrationState.Valid)?.registration
        }
    }

    private class DelayedProfileAuthRepository(
        initialUserId: String,
        initialEmail: String
    ) : AuthRepository {
        private val userId = MutableStateFlow<String?>(initialUserId)
        private var email: String? = initialEmail

        val updateStarted = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()
        var updateCallCount = 0
            private set

        override fun observeAuthState(): Flow<String?> = userId.asStateFlow()
        override fun currentUserId(): String? = userId.value
        override fun currentUserEmail(): String? = email

        override suspend fun login(email: String, password: String): Result<String> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun register(email: String, password: String): Result<String> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun updateDisplayName(userId: String, fullName: String): Result<Unit> {
            updateCallCount += 1
            updateStarted.complete(Unit)
            releaseUpdate.await()
            return Result.success(Unit)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun signOut(): Result<Unit> {
            email = null
            userId.value = null
            return Result.success(Unit)
        }

        fun switchUser(newUserId: String, newEmail: String) {
            email = newEmail
            userId.value = newUserId
        }
    }

    private class DelayedRegistrationAuthRepository : AuthRepository {
        private val userId = MutableStateFlow<String?>(null)
        private var email: String? = null

        val registerStarted = CompletableDeferred<Unit>()
        val releaseRegistration = CompletableDeferred<Unit>()
        var profileUpdateCallCount = 0
            private set
        var shouldFailRegistration = false
        var completeAsCurrentUser = true

        override fun observeAuthState(): Flow<String?> = userId.asStateFlow()
        override fun currentUserId(): String? = userId.value
        override fun currentUserEmail(): String? = email

        override suspend fun login(email: String, password: String): Result<String> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun register(email: String, password: String): Result<String> {
            registerStarted.complete(Unit)
            releaseRegistration.await()
            if (shouldFailRegistration) {
                return Result.failure(IllegalStateException("Registration failed"))
            }
            val createdUserId = "delayed-user"
            if (completeAsCurrentUser) {
                this.email = email
                userId.value = createdUserId
            }
            return Result.success(createdUserId)
        }

        override suspend fun updateDisplayName(userId: String, fullName: String): Result<Unit> {
            profileUpdateCallCount += 1
            return Result.success(Unit)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun signOut(): Result<Unit> {
            email = null
            userId.value = null
            return Result.success(Unit)
        }

        fun switchUser(newUserId: String, newEmail: String) {
            email = newEmail
            userId.value = newUserId
        }
    }

    private class SupersedingRegistrationAuthRepository : AuthRepository {
        private val userId = MutableStateFlow<String?>(null)
        private var email: String? = null

        val firstRegistrationStarted = CompletableDeferred<Unit>()
        val secondRegistrationStarted = CompletableDeferred<Unit>()
        val releaseFirstRegistration = CompletableDeferred<Unit>()
        val releaseSecondRegistration = CompletableDeferred<Unit>()
        var profileUpdateCallCount = 0
            private set
        var displayName: String? = null
            private set

        override fun observeAuthState(): Flow<String?> = userId.asStateFlow()
        override fun currentUserId(): String? = userId.value
        override fun currentUserEmail(): String? = email

        override suspend fun login(email: String, password: String): Result<String> {
            return Result.failure(UnsupportedOperationException())
        }

        override suspend fun register(email: String, password: String): Result<String> {
            return if (email == "first@example.com") {
                firstRegistrationStarted.complete(Unit)
                releaseFirstRegistration.await()
                this.email = email
                userId.value = "first-user"
                Result.success("first-user")
            } else {
                secondRegistrationStarted.complete(Unit)
                releaseSecondRegistration.await()
                this.email = email
                userId.value = "second-user"
                Result.success("second-user")
            }
        }

        override suspend fun updateDisplayName(userId: String, fullName: String): Result<Unit> {
            if (this.userId.value != userId) {
                return Result.failure(IllegalStateException("Wrong user"))
            }
            profileUpdateCallCount += 1
            displayName = fullName
            return Result.success(Unit)
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun signOut(): Result<Unit> {
            email = null
            userId.value = null
            return Result.success(Unit)
        }
    }

    private class CleanupRaceRegistrationAuthRepository(
        private val failSignOut: Boolean = false
    ) : AuthRepository {
        private val userId = MutableStateFlow<String?>(null)
        private var email: String? = null

        val releaseRegistration = CompletableDeferred<Unit>()
        var signOutCallCount = 0
            private set

        override fun observeAuthState(): Flow<String?> = userId.asStateFlow()
        override fun currentUserId(): String? = userId.value
        override fun currentUserEmail(): String? = email

        override suspend fun login(email: String, password: String): Result<String> =
            Result.failure(UnsupportedOperationException())

        override suspend fun register(email: String, password: String): Result<String> {
            releaseRegistration.await()
            this.email = email
            userId.value = "abandoned-user"
            return Result.success("abandoned-user")
        }

        override suspend fun updateDisplayName(userId: String, fullName: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun signOut(): Result<Unit> {
            signOutCallCount += 1
            if (failSignOut) return Result.failure(IllegalStateException("sign-out failed"))
            email = null
            userId.value = null
            return Result.success(Unit)
        }

        fun switchUser(newUserId: String, newEmail: String) {
            email = newEmail
            userId.value = newUserId
        }
    }
}
