package com.example.personalfinanceapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.auth.PendingRegistration
import com.example.personalfinanceapp.feature.auth.PendingRegistrationPhase
import com.example.personalfinanceapp.feature.auth.PendingRegistrationState
import com.example.personalfinanceapp.feature.auth.PendingRegistrationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val fullNameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isLoading: Boolean = false,
    val isProfileUpdatePending: Boolean = false,
    val isAccountCreated: Boolean = false,
    val requiresRecoveryAbandonment: Boolean = false,
    val errorMessage: String? = null
)

data class PasswordResetUiState(
    val emailInput: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthNavigationState {
    data object Resolving : AuthNavigationState
    data object Unauthenticated : AuthNavigationState
    data object RegistrationSetup : AuthNavigationState
    data class Authenticated(val userId: UserId) : AuthNavigationState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pendingRegistrationStore: PendingRegistrationStore
) : ViewModel() {

    private var pendingRegistrationState = readPendingRegistrationState()
    private val restoredPendingRegistration =
        (pendingRegistrationState as? PendingRegistrationState.Valid)?.registration
    private val _pendingRegistration = MutableStateFlow(restoredPendingRegistration)
    private var operationGeneration = 0L
    private val inFlightAccountCreations = mutableMapOf<Long, PendingRegistration>()

    private val _authState = MutableStateFlow<UserId?>(null)
    val authState: StateFlow<UserId?> = _authState.asStateFlow()

    private val _navigationState =
        MutableStateFlow<AuthNavigationState>(AuthNavigationState.Resolving)
    val navigationState: StateFlow<AuthNavigationState> = _navigationState.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(
        when (pendingRegistrationState) {
            PendingRegistrationState.Absent -> RegisterUiState()
            PendingRegistrationState.Corrupt -> corruptMarkerUiState()
            is PendingRegistrationState.Valid -> restoredPendingRegistration!!.toRestoredUiState()
        }
    )
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _passwordResetUiState = MutableStateFlow(PasswordResetUiState())
    val passwordResetUiState: StateFlow<PasswordResetUiState> =
        _passwordResetUiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect(::reconcileAuthenticatedUser)
        }
    }

    fun onRegisterFullNameChanged(fullName: String) {
        _registerUiState.value = _registerUiState.value.copy(
            fullNameInput = fullName,
            errorMessage = null
        )
    }

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
        if (_loginUiState.value.isLoading) return

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
        val currentState = _registerUiState.value
        if (currentState.isLoading) return
        val durableState = readPendingRegistrationState()
        if (
            durableState is PendingRegistrationState.Valid &&
            durableState.registration.phase == PendingRegistrationPhase.Abandoned
        ) {
            pendingRegistrationState = durableState
            _pendingRegistration.value = durableState.registration
            _registerUiState.value = abandonedRegistrationUiState()
            _navigationState.value = AuthNavigationState.RegistrationSetup
            return
        }
        if (currentState.requiresRecoveryAbandonment) {
            _registerUiState.value = currentState.copy(
                errorMessage = CORRUPT_MARKER_MESSAGE
            )
            return
        }

        val fullName = currentState.fullNameInput.trim()
        if (fullName.isBlank()) {
            _registerUiState.value = currentState.copy(
                errorMessage = "Please enter your full name"
            )
            return
        }

        if (currentState.isAccountCreated) {
            retryProfileSetup(fullName)
            return
        }

        val email = currentState.emailInput.trim().lowercase(Locale.ROOT)
        val password = currentState.passwordInput
        val confirmPassword = currentState.confirmPasswordInput

        if (!isValidEmail(email)) {
            _registerUiState.value = currentState.copy(
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        if (password.length < 6) {
            _registerUiState.value = currentState.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }

        if (password != confirmPassword) {
            _registerUiState.value = currentState.copy(
                errorMessage = "Passwords do not match"
            )
            return
        }

        val registrationIntent = PendingRegistration(
            email = email,
            fullName = fullName
        )
        val persistenceResult = pendingRegistrationStore.save(registrationIntent)
        if (persistenceResult.isFailure) {
            _registerUiState.value = currentState.copy(
                errorMessage = "Unable to save registration progress. Please try again."
            )
            return
        }

        pendingRegistrationState = PendingRegistrationState.Valid(registrationIntent)
        _pendingRegistration.value = registrationIntent
        _registerUiState.value = currentState.copy(
            fullNameInput = fullName,
            emailInput = email,
            isLoading = true,
            isProfileUpdatePending = true,
            errorMessage = null
        )

        val operationId = ++operationGeneration
        inFlightAccountCreations[operationId] = registrationIntent
        viewModelScope.launch {
            val registrationResult = authRepository.register(email, password)
            if (registrationResult.isFailure) {
                val validation = validateAccountCreationIntent(operationId, registrationIntent)
                if (validation == AccountCreationValidation.Stale) {
                    resolveStaleAccountCreationFailure(operationId, registrationIntent)
                    return@launch
                }
                inFlightAccountCreations.remove(operationId)
                if (validation == AccountCreationValidation.InvalidMarker) {
                    enterRegistrationRecovery()
                    return@launch
                }

                invalidateOperations()
                val clearResult = pendingRegistrationStore.clear()
                if (clearResult.isSuccess) {
                    pendingRegistrationState = PendingRegistrationState.Absent
                    _pendingRegistration.value = null
                    _registerUiState.value = _registerUiState.value.copy(
                        isLoading = false,
                        isProfileUpdatePending = false,
                        errorMessage = registrationResult.exceptionOrNull()?.localizedMessage
                            ?: "Registration failed"
                    )
                } else {
                    enterRegistrationRecovery(
                        "Registration failed and its saved recovery state could not be cleared. " +
                            "Abandon setup to retry safely."
                    )
                }
                return@launch
            }

            val userId = registrationResult.getOrThrow()
            when (validateAccountCreationCompletion(operationId, registrationIntent, userId)) {
                AccountCreationValidation.Stale -> {
                    recordStaleAccountCreation(operationId, registrationIntent, userId)
                    return@launch
                }
                AccountCreationValidation.InvalidMarker -> {
                    recordStaleAccountCreation(operationId, registrationIntent, userId)
                    enterRegistrationRecovery()
                    return@launch
                }
                AccountCreationValidation.Current -> Unit
            }

            val currentPending = _pendingRegistration.value ?: registrationIntent
            val boundPending = currentPending.copy(userId = userId)
            val bindingResult = pendingRegistrationStore.save(boundPending)
            if (bindingResult.isFailure) {
                inFlightAccountCreations.remove(operationId)
                enterRegistrationRecovery(
                    "Your account was created, but setup progress could not be secured. " +
                        "Abandon setup and sign out, then try again."
                )
                return@launch
            }
            pendingRegistrationState = PendingRegistrationState.Valid(boundPending)
            _pendingRegistration.value = boundPending
            inFlightAccountCreations.remove(operationId)
            _registerUiState.value = _registerUiState.value.copy(
                isAccountCreated = true,
                isProfileUpdatePending = true
            )

            startProfileSetup(userId, fullName, operationId)
        }
    }

    fun abandonPendingRegistration() {
        if (pendingRegistrationState == PendingRegistrationState.Absent) {
            _registerUiState.value = RegisterUiState()
            return
        }

        invalidateOperations()
        val protectsInFlightCreation = persistAbandonedInFlightMarker()
        if (protectsInFlightCreation == null) return
        viewModelScope.launch {
            if (authRepository.currentUserId() != null) {
                val signOutResult = authRepository.signOut()
                if (signOutResult.isFailure) {
                    _registerUiState.value = _registerUiState.value.copy(
                        isLoading = false,
                        errorMessage = "Unable to sign out. Please try again."
                    )
                    return@launch
                }
            }

            if (protectsInFlightCreation) {
                _authState.value = null
                _registerUiState.value = RegisterUiState()
                _navigationState.value = AuthNavigationState.Unauthenticated
                return@launch
            }

            val clearResult = pendingRegistrationStore.clear()
            if (clearResult.isFailure) {
                enterRegistrationRecovery(
                    "You are signed out, but registration recovery data could not be cleared. " +
                        "Please try abandoning setup again."
                )
                return@launch
            }
            pendingRegistrationState = PendingRegistrationState.Absent
            _pendingRegistration.value = null
            _authState.value = null
            _registerUiState.value = RegisterUiState()
            _navigationState.value = AuthNavigationState.Unauthenticated
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
        if (_passwordResetUiState.value.isLoading) return

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
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "Password reset failed"
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
        invalidateOperations()
        val protectsInFlightCreation = persistAbandonedInFlightMarker()
        if (protectsInFlightCreation == null) return
        val signingOutUser = authRepository.currentUserSnapshot()
        val signingOutUserId = signingOutUser?.userId
        val signingOutEmail = signingOutUser?.email
            ?.trim()
            ?.lowercase(Locale.ROOT)
        val pendingBeforeSignOut =
            (readPendingRegistrationState() as? PendingRegistrationState.Valid)?.registration
        val preserveUnrelatedAbandonedMarker =
            pendingBeforeSignOut?.phase == PendingRegistrationPhase.Abandoned &&
                pendingBeforeSignOut.userId != signingOutUserId &&
                !(pendingBeforeSignOut.userId == null &&
                    pendingBeforeSignOut.email == signingOutEmail)
        viewModelScope.launch {
            val result = authRepository.signOut()
            if (result.isFailure) return@launch

            if (protectsInFlightCreation) {
                _authState.value = null
                _loginUiState.value = LoginUiState()
                _registerUiState.value = RegisterUiState()
                _passwordResetUiState.value = PasswordResetUiState()
                _navigationState.value = AuthNavigationState.Unauthenticated
                return@launch
            }

            if (preserveUnrelatedAbandonedMarker) {
                pendingRegistrationState = readPendingRegistrationState()
                _pendingRegistration.value =
                    (pendingRegistrationState as? PendingRegistrationState.Valid)?.registration
                _authState.value = null
                _loginUiState.value = LoginUiState()
                _registerUiState.value = RegisterUiState()
                _passwordResetUiState.value = PasswordResetUiState()
                _navigationState.value = AuthNavigationState.Unauthenticated
                return@launch
            }

            pendingRegistrationState = readPendingRegistrationState()
            if (pendingRegistrationState != PendingRegistrationState.Absent) {
                val clearResult = pendingRegistrationStore.clear()
                if (clearResult.isFailure) {
                    _authState.value = null
                    enterRegistrationRecovery(
                        "You are signed out, but registration recovery data could not be cleared. " +
                            "Please abandon setup to continue."
                    )
                    return@launch
                }
            }
            pendingRegistrationState = PendingRegistrationState.Absent
            _pendingRegistration.value = null
            _authState.value = null
            _loginUiState.value = LoginUiState()
            _registerUiState.value = RegisterUiState()
            _passwordResetUiState.value = PasswordResetUiState()
            _navigationState.value = AuthNavigationState.Unauthenticated
        }
    }

    private fun retryProfileSetup(fullName: String) {
        val userId = _authState.value
        val pendingRegistration = _pendingRegistration.value
        if (
            userId == null ||
            pendingRegistration == null ||
            pendingRegistration.userId != userId
        ) {
            _registerUiState.value = _registerUiState.value.copy(
                errorMessage = "Your registration session is no longer available. Please sign in again."
            )
            return
        }

        val updatedPending = pendingRegistration.copy(fullName = fullName)
        val persistenceResult = pendingRegistrationStore.save(updatedPending)
        if (persistenceResult.isFailure) {
            _registerUiState.value = _registerUiState.value.copy(
                errorMessage = "Unable to save registration progress. Please try again."
            )
            return
        }

        pendingRegistrationState = PendingRegistrationState.Valid(updatedPending)
        _pendingRegistration.value = updatedPending
        _registerUiState.value = _registerUiState.value.copy(
            fullNameInput = fullName,
            isLoading = true,
            isProfileUpdatePending = true,
            errorMessage = null
        )

        startProfileSetup(userId, fullName)
    }

    private fun startProfileSetup(
        userId: UserId,
        fullName: String,
        accountCreationOperationId: Long? = null
    ) {
        val operationId = accountCreationOperationId ?: ++operationGeneration
        val validation = validateProfileOperation(operationId, userId)
        if (validation != ProfileOperationValidation.Current) {
            if (validation == ProfileOperationValidation.InvalidMarker) {
                enterRegistrationRecovery()
            }
            return
        }

        viewModelScope.launch {
            completeProfileSetup(operationId, userId, fullName)
        }
    }

    private suspend fun completeProfileSetup(
        operationId: Long,
        userId: UserId,
        fullName: String
    ) {
        if (validateProfileOperation(operationId, userId) != ProfileOperationValidation.Current) {
            return
        }

        val profileResult = authRepository.updateDisplayName(userId, fullName)
        when (validateProfileOperation(operationId, userId)) {
            ProfileOperationValidation.Stale -> return
            ProfileOperationValidation.InvalidMarker -> {
                enterRegistrationRecovery()
                return
            }
            ProfileOperationValidation.Current -> Unit
        }

        if (profileResult.isFailure) {
            _registerUiState.value = _registerUiState.value.copy(
                isLoading = false,
                isProfileUpdatePending = true,
                isAccountCreated = true,
                errorMessage = "Profile setup is incomplete. Save your full name to continue."
            )
            _navigationState.value = AuthNavigationState.RegistrationSetup
            return
        }

        val clearResult = pendingRegistrationStore.clear()
        if (operationId != operationGeneration || authRepository.currentUserId() != userId) {
            return
        }
        if (clearResult.isFailure) {
            _registerUiState.value = _registerUiState.value.copy(
                isLoading = false,
                isProfileUpdatePending = true,
                isAccountCreated = true,
                errorMessage = "Your name was saved, but setup could not be finalized. Please retry."
            )
            _navigationState.value = AuthNavigationState.RegistrationSetup
            return
        }

        pendingRegistrationState = PendingRegistrationState.Absent
        _pendingRegistration.value = null
        _registerUiState.value = RegisterUiState()
        _navigationState.value = AuthNavigationState.Authenticated(userId)
    }

    private fun validateAccountCreationIntent(
        operationId: Long,
        registrationIntent: PendingRegistration
    ): AccountCreationValidation {
        if (
            operationId != operationGeneration ||
            inFlightAccountCreations[operationId] != registrationIntent
        ) {
            return AccountCreationValidation.Stale
        }

        return if (hasMatchingRegistrationMarker(registrationIntent, null)) {
            AccountCreationValidation.Current
        } else {
            AccountCreationValidation.InvalidMarker
        }
    }

    private fun validateAccountCreationCompletion(
        operationId: Long,
        registrationIntent: PendingRegistration,
        createdUserId: UserId
    ): AccountCreationValidation {
        val currentUser = authRepository.currentUserSnapshot()
        if (
            operationId != operationGeneration ||
            inFlightAccountCreations[operationId] != registrationIntent ||
            currentUser?.userId != createdUserId ||
            currentUser.email
                ?.trim()
                ?.lowercase(Locale.ROOT) != registrationIntent.email
        ) {
            return AccountCreationValidation.Stale
        }

        return if (hasMatchingRegistrationMarker(registrationIntent, createdUserId)) {
            AccountCreationValidation.Current
        } else {
            AccountCreationValidation.InvalidMarker
        }
    }

    private fun hasMatchingRegistrationMarker(
        registrationIntent: PendingRegistration,
        allowedUserId: UserId?
    ): Boolean {
        val inMemory = _pendingRegistration.value ?: return false
        val persisted = (readPendingRegistrationState() as? PendingRegistrationState.Valid)
            ?.registration ?: return false

        fun PendingRegistration.matchesIntent(): Boolean {
            return email == registrationIntent.email &&
                fullName == registrationIntent.fullName &&
                (userId == null || userId == allowedUserId)
        }

        return inMemory.matchesIntent() && persisted.matchesIntent()
    }

    private fun recordStaleAccountCreation(
        operationId: Long,
        registrationIntent: PendingRegistration,
        createdUserId: UserId
    ) {
        val persisted = (readPendingRegistrationState() as? PendingRegistrationState.Valid)
            ?.registration
        if (
            persisted != null &&
            persisted.email == registrationIntent.email &&
            persisted.fullName == registrationIntent.fullName &&
            persisted.phase == PendingRegistrationPhase.Abandoned
        ) {
            val boundAbandoned = persisted.copy(userId = createdUserId)
            if (pendingRegistrationStore.save(boundAbandoned).isSuccess) {
                pendingRegistrationState = PendingRegistrationState.Valid(boundAbandoned)
                _pendingRegistration.value = boundAbandoned
            }
        }
        inFlightAccountCreations.remove(operationId)
        reconcileAuthenticatedUser(authRepository.currentUserId())
    }

    private fun resolveStaleAccountCreationFailure(
        operationId: Long,
        registrationIntent: PendingRegistration
    ) {
        val persisted = (readPendingRegistrationState() as? PendingRegistrationState.Valid)
            ?.registration
        if (
            persisted != null &&
            persisted.email == registrationIntent.email &&
            persisted.fullName == registrationIntent.fullName &&
            persisted.phase == PendingRegistrationPhase.Abandoned
        ) {
            pendingRegistrationStore.clear()
        }
        inFlightAccountCreations.remove(operationId)
        reconcileAuthenticatedUser(authRepository.currentUserId())
    }

    private fun persistAbandonedInFlightMarker(): Boolean? {
        val pending = _pendingRegistration.value ?: return false
        val hasMatchingInFlightCreation = inFlightAccountCreations.values.any {
            it.email == pending.email && it.fullName == pending.fullName
        }
        if (!hasMatchingInFlightCreation) return false

        val abandoned = pending.copy(phase = PendingRegistrationPhase.Abandoned)
        if (pendingRegistrationStore.save(abandoned).isFailure) {
            enterRegistrationRecovery(
                "Registration cancellation could not be secured. Please try again."
            )
            return null
        }
        pendingRegistrationState = PendingRegistrationState.Valid(abandoned)
        _pendingRegistration.value = abandoned
        return true
    }

    private fun reconcileAuthenticatedUser(userId: UserId?) {
        val currentUser = authRepository.currentUserSnapshot()
        if (userId != currentUser?.userId) {
            if (userId != null || currentUser != null) return
        }

        val currentEmail = currentUser?.email
            ?.trim()
            ?.lowercase(Locale.ROOT)
        val belongsToStaleAccountCreation = userId != null &&
            inFlightAccountCreations.any { (operationId, registration) ->
                operationId != operationGeneration && registration.email == currentEmail
            }
        if (belongsToStaleAccountCreation) {
            _authState.value = null
            _navigationState.value = AuthNavigationState.Unauthenticated
            return
        }

        val previousUserId = _authState.value
        if (previousUserId != userId && !isExpectedRegistrationAuthentication(previousUserId, userId)) {
            invalidateOperations()
        }
        _authState.value = userId

        pendingRegistrationState = readPendingRegistrationState()
        val pendingRegistration =
            (pendingRegistrationState as? PendingRegistrationState.Valid)?.registration
        _pendingRegistration.value = pendingRegistration

        if (pendingRegistrationState == PendingRegistrationState.Corrupt) {
            enterRegistrationRecovery()
            return
        }

        if (pendingRegistration?.phase == PendingRegistrationPhase.Abandoned) {
            if (userId == null) {
                _navigationState.value = AuthNavigationState.Unauthenticated
                return
            }

            if (pendingRegistration.userId == userId) {
                _registerUiState.value = abandonedRegistrationUiState()
                _navigationState.value = AuthNavigationState.RegistrationSetup
                return
            }

            if (pendingRegistration.userId == null && currentEmail == pendingRegistration.email) {
                val boundAbandoned = pendingRegistration.copy(userId = userId)
                if (pendingRegistrationStore.save(boundAbandoned).isFailure) {
                    enterRegistrationRecovery(
                        "Cancelled registration recovery could not be secured. Sign out to continue safely."
                    )
                    return
                }
                pendingRegistrationState = PendingRegistrationState.Valid(boundAbandoned)
                _pendingRegistration.value = boundAbandoned
                _registerUiState.value = abandonedRegistrationUiState()
                _navigationState.value = AuthNavigationState.RegistrationSetup
                return
            }

            // The durable cancellation belongs to another account. Keep it for that
            // account while allowing this unrelated existing user to continue.
            _navigationState.value = AuthNavigationState.Authenticated(userId)
            return
        }

        if (userId == null) {
            when {
                pendingRegistration == null -> {
                    _navigationState.value = AuthNavigationState.Unauthenticated
                }

                pendingRegistration.userId == null -> {
                    _registerUiState.value = pendingRegistration.toRestoredUiState()
                    _navigationState.value = AuthNavigationState.RegistrationSetup
                }

                else -> {
                    // Keep the UID-bound marker until explicit abandonment/sign-out or
                    // the same Firebase account authenticates again. This also avoids
                    // losing setup state if Firebase briefly reports null during startup.
                    _navigationState.value = AuthNavigationState.Unauthenticated
                }
            }
            return
        }

        if (pendingRegistration == null) {
            _navigationState.value = AuthNavigationState.Authenticated(userId)
            return
        }

        if (pendingRegistration.userId == userId) {
            _registerUiState.value = pendingRegistration.toRestoredUiState(
                accountCreated = true
            )
            _navigationState.value = AuthNavigationState.RegistrationSetup
            return
        }

        if (
            pendingRegistration.userId == null &&
            currentEmail != null &&
            currentEmail == pendingRegistration.email
        ) {
            val boundPending = pendingRegistration.copy(userId = userId)
            val bindingResult = pendingRegistrationStore.save(boundPending)
            if (bindingResult.isFailure) {
                enterRegistrationRecovery(
                    "Your account exists, but registration recovery could not be secured. " +
                        "Abandon setup and sign out to recover safely."
                )
                return
            }
            pendingRegistrationState = PendingRegistrationState.Valid(boundPending)
            _pendingRegistration.value = boundPending
            _registerUiState.value = boundPending.toRestoredUiState(
                accountCreated = true
            )
            _navigationState.value = AuthNavigationState.RegistrationSetup
            return
        }

        val belongsToInFlightCreation = inFlightAccountCreations.values.any {
            it.email == pendingRegistration.email &&
                it.fullName == pendingRegistration.fullName
        }
        if (belongsToInFlightCreation) {
            val abandoned = pendingRegistration.copy(phase = PendingRegistrationPhase.Abandoned)
            if (pendingRegistrationStore.save(abandoned).isFailure) {
                enterRegistrationRecovery(
                    "Registration cancellation could not be secured. Sign out to continue safely."
                )
                return
            }
            pendingRegistrationState = PendingRegistrationState.Valid(abandoned)
            _pendingRegistration.value = abandoned
            _registerUiState.value = RegisterUiState()
            _navigationState.value = AuthNavigationState.Authenticated(userId)
            return
        }

        invalidateOperations()
        val clearResult = pendingRegistrationStore.clear()
        if (clearResult.isFailure) {
            enterRegistrationRecovery(
                "Saved registration data belongs to another account and could not be cleared. " +
                    "Abandon setup and sign out to continue safely."
            )
            return
        }
        pendingRegistrationState = PendingRegistrationState.Absent
        _pendingRegistration.value = null
        _registerUiState.value = RegisterUiState()
        _navigationState.value = AuthNavigationState.Authenticated(userId)
    }

    private fun isExpectedRegistrationAuthentication(
        previousUserId: UserId?,
        newUserId: UserId?
    ): Boolean {
        if (previousUserId != null || newUserId == null) return false
        val pending = _pendingRegistration.value ?: return false
        if (pending.userId == newUserId) return true
        if (pending.userId != null) return false
        return authRepository.currentUserEmail()
            ?.trim()
            ?.lowercase(Locale.ROOT) == pending.email
    }

    private fun validateProfileOperation(
        operationId: Long,
        userId: UserId
    ): ProfileOperationValidation {
        if (operationId != operationGeneration || authRepository.currentUserId() != userId) {
            return ProfileOperationValidation.Stale
        }

        val inMemoryPending = _pendingRegistration.value
        if (inMemoryPending?.userId != userId) {
            return ProfileOperationValidation.InvalidMarker
        }

        return when (val persisted = readPendingRegistrationState()) {
            is PendingRegistrationState.Valid -> {
                if (persisted.registration.userId == userId) {
                    ProfileOperationValidation.Current
                } else {
                    ProfileOperationValidation.InvalidMarker
                }
            }
            PendingRegistrationState.Absent,
            PendingRegistrationState.Corrupt -> ProfileOperationValidation.InvalidMarker
        }
    }

    private fun invalidateOperations() {
        operationGeneration += 1
    }

    private fun readPendingRegistrationState(): PendingRegistrationState {
        return runCatching { pendingRegistrationStore.read() }
            .getOrDefault(PendingRegistrationState.Corrupt)
    }

    private fun enterRegistrationRecovery(message: String = CORRUPT_MARKER_MESSAGE) {
        pendingRegistrationState = readPendingRegistrationState()
        _pendingRegistration.value =
            (pendingRegistrationState as? PendingRegistrationState.Valid)?.registration
        _registerUiState.value = corruptMarkerUiState(message)
        _navigationState.value = AuthNavigationState.RegistrationSetup
    }

    companion object {
        private const val CORRUPT_MARKER_MESSAGE =
            "Registration recovery data is incomplete. Abandon setup and sign out to continue safely."

        fun isValidEmail(email: String): Boolean {
            return email.isNotBlank() &&
                Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                    .matches(email)
        }
    }

    private enum class ProfileOperationValidation {
        Current,
        Stale,
        InvalidMarker
    }

    private enum class AccountCreationValidation {
        Current,
        Stale,
        InvalidMarker
    }
}

private fun PendingRegistration.toRestoredUiState(
    accountCreated: Boolean = userId != null
): RegisterUiState {
    if (phase == PendingRegistrationPhase.Abandoned) {
        return abandonedRegistrationUiState()
    }
    return RegisterUiState(
        fullNameInput = fullName,
        emailInput = email,
        isProfileUpdatePending = true,
        isAccountCreated = accountCreated,
        errorMessage = if (accountCreated) {
            "Profile setup is incomplete. Save your full name to continue."
        } else {
            "Registration was interrupted. Re-enter your password to continue."
        }
    )
}

private fun abandonedRegistrationUiState(): RegisterUiState {
    return RegisterUiState(
        isProfileUpdatePending = true,
        isAccountCreated = true,
        requiresRecoveryAbandonment = true,
        errorMessage = "This registration was cancelled but still requires a safe sign-out."
    )
}

private fun corruptMarkerUiState(
    message: String =
        "Registration recovery data is incomplete. Abandon setup and sign out to continue safely."
): RegisterUiState {
    return RegisterUiState(
        isProfileUpdatePending = true,
        isAccountCreated = true,
        requiresRecoveryAbandonment = true,
        errorMessage = message
    )
}
