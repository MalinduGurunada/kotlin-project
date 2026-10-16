package com.example.personalfinanceapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class PasswordResetUiState(
    val emailInput: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<UserId?> = authRepository
        .observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _passwordResetUiState = MutableStateFlow(PasswordResetUiState())
    val passwordResetUiState: StateFlow<PasswordResetUiState> = _passwordResetUiState.asStateFlow()

    fun onLoginEmailChanged(email: String) {
        _loginUiState.value = _loginUiState.value.copy(
            emailInput = email,
            errorMessage = null
        )
    }

    fun onLoginPasswordChanged(password: String) {
        _loginUiState.value = _loginUiState.value.copy(
            passwordInput = password,
            errorMessage = null
        )
    }

    fun login() {
        val email = _loginUiState.value.emailInput.trim()
        val password = _loginUiState.value.passwordInput

        if (!isValidEmail(email)) {
            _loginUiState.value = _loginUiState.value.copy(
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        if (password.length < 6) {
            _loginUiState.value = _loginUiState.value.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }

        _loginUiState.value = _loginUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result.isFailure) {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed"
                )
            } else {
                _loginUiState.value = LoginUiState()
            }
        }
    }

    fun onRegisterEmailChanged(email: String) {
        _registerUiState.value = _registerUiState.value.copy(
            emailInput = email,
            errorMessage = null
        )
    }

    fun onRegisterPasswordChanged(password: String) {
        _registerUiState.value = _registerUiState.value.copy(
            passwordInput = password,
            errorMessage = null
        )
    }

    fun onRegisterConfirmPasswordChanged(password: String) {
        _registerUiState.value = _registerUiState.value.copy(
            confirmPasswordInput = password,
            errorMessage = null
        )
    }

    fun register() {
        val email = _registerUiState.value.emailInput.trim()
        val password = _registerUiState.value.passwordInput
        val confirmPassword = _registerUiState.value.confirmPasswordInput

        if (!isValidEmail(email)) {
            _registerUiState.value = _registerUiState.value.copy(
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        if (password.length < 6) {
            _registerUiState.value = _registerUiState.value.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }

        if (password != confirmPassword) {
            _registerUiState.value = _registerUiState.value.copy(
                errorMessage = "Passwords do not match"
            )
            return
        }

        _registerUiState.value = _registerUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = authRepository.register(email, password)
            if (result.isFailure) {
                _registerUiState.value = _registerUiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Registration failed"
                )
            } else {
                _registerUiState.value = RegisterUiState()
            }
        }
    }

    fun onResetEmailChanged(email: String) {
        _passwordResetUiState.value = _passwordResetUiState.value.copy(
            emailInput = email,
            errorMessage = null,
            isSuccess = false
        )
    }

    fun sendPasswordReset() {
        val email = _passwordResetUiState.value.emailInput.trim()

        if (!isValidEmail(email)) {
            _passwordResetUiState.value = _passwordResetUiState.value.copy(
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        _passwordResetUiState.value = _passwordResetUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            if (result.isFailure) {
                _passwordResetUiState.value = _passwordResetUiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Password reset failed"
                )
            } else {
                _passwordResetUiState.value = _passwordResetUiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    errorMessage = null
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _loginUiState.value = LoginUiState()
            _registerUiState.value = RegisterUiState()
            _passwordResetUiState.value = PasswordResetUiState()
        }
    }

    companion object {
        fun isValidEmail(email: String): Boolean {
            return email.isNotBlank() &&
                    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)
        }
    }
}
