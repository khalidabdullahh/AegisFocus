package com.aegisfocus.app.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.remote.AuthResult
import com.aegisfocus.app.data.remote.AuthUser
import com.aegisfocus.app.data.remote.SupabaseAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isEmailVerificationPending: Boolean = false,
    val unverifiedEmail: String? = null,
    val currentUser: AuthUser? = null
)

class AuthViewModel(
    private val authService: SupabaseAuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isEmailVerificationPending = if (user != null) false else _uiState.value.isEmailVerificationPending
                )
            }
        }
    }

    fun setAuthMode(isSignUp: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSignUp = isSignUp,
            errorMessage = null,
            successMessage = null,
            isEmailVerificationPending = false
        )
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(pass: String) {
        _uiState.value = _uiState.value.copy(password = pass, errorMessage = null)
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || !state.email.contains("@")) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }
        if (state.isSignUp && state.displayName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your full name.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null,
                isEmailVerificationPending = false
            )

            val result = if (state.isSignUp) {
                authService.signUp(state.email, state.password, state.displayName)
            } else {
                authService.signIn(state.email, state.password)
            }

            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = result.message,
                        currentUser = result.user,
                        isEmailVerificationPending = false
                    )
                }
                is AuthResult.EmailVerificationRequired -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmailVerificationPending = true,
                        unverifiedEmail = result.email,
                        successMessage = result.message
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun resendVerification() {
        val targetEmail = _uiState.value.unverifiedEmail ?: _uiState.value.email
        if (targetEmail.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authService.resendVerificationEmail(targetEmail)
            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = result.message
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is AuthResult.EmailVerificationRequired -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = result.message
                    )
                }
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String, userEmail: String? = null, displayName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = authService.signInWithGoogle(idToken, userEmail = userEmail, displayName = displayName)
            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = result.message,
                        currentUser = result.user
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is AuthResult.EmailVerificationRequired -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmailVerificationPending = true,
                        unverifiedEmail = result.email,
                        successMessage = result.message
                    )
                }
            }
        }
    }

    fun launchBrowserGoogleAuth(context: Context) {
        authService.launchBrowserGoogleAuth(context)
    }

    fun updateDisplayName(newName: String) {
        if (newName.isBlank()) return
        authService.updateDisplayName(newName)
        _uiState.value = _uiState.value.copy(successMessage = "Profile name updated successfully!")
    }

    fun updateAvatarUri(uri: String) {
        authService.updateAvatarUri(uri)
        _uiState.value = _uiState.value.copy(successMessage = "Profile photo updated successfully!")
    }

    fun updatePassword(newPass: String) {
        if (newPass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "New password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val res = authService.updatePassword(newPass)
            when (res) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, successMessage = res.message)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                is AuthResult.EmailVerificationRequired -> _uiState.value = _uiState.value.copy(isLoading = false, successMessage = res.message)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val res = authService.sendPasswordReset(email)
            when (res) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, successMessage = res.message)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                is AuthResult.EmailVerificationRequired -> _uiState.value = _uiState.value.copy(isLoading = false, successMessage = res.message)
            }
        }
    }

    fun setErrorMessage(msg: String) {
        _uiState.value = _uiState.value.copy(errorMessage = msg, isLoading = false)
    }

    fun signOut() {
        authService.signOut()
        _uiState.value = _uiState.value.copy(
            currentUser = null,
            isEmailVerificationPending = false,
            successMessage = "Signed out successfully."
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
