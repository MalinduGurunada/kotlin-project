package com.example.personalfinanceapp.feature.auth

import com.example.personalfinanceapp.feature.auth.ui.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
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
    fun loginSuccessUpdatesAuthState() = runTest {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password123")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val userId = fakeAuthRepository.observeAuthState().first()
        assertEquals("user_user_at_example_com", userId)
    }

    @Test
    fun registerWithMismatchedPasswordsShowsError() = runTest {
        viewModel.onRegisterEmailChanged("newuser@example.com")
        viewModel.onRegisterPasswordChanged("password123")
        viewModel.onRegisterConfirmPasswordChanged("different123")
        viewModel.register()

        val state = viewModel.registerUiState.value
        assertEquals("Passwords do not match", state.errorMessage)
    }

    @Test
    fun registerSuccessUpdatesAuthState() = runTest {
        viewModel.onRegisterEmailChanged("newuser@example.com")
        viewModel.onRegisterPasswordChanged("password123")
        viewModel.onRegisterConfirmPasswordChanged("password123")
        viewModel.register()

        testDispatcher.scheduler.advanceUntilIdle()

        val userId = fakeAuthRepository.observeAuthState().first()
        assertEquals("user_newuser_at_example_com", userId)
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
    fun signOutClearsAuthState() = runTest {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password123")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        val userId = fakeAuthRepository.observeAuthState().first()
        assertNull(userId)
    }
}
