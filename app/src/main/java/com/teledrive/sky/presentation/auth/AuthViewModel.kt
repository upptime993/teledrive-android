package com.teledrive.sky.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teledrive.sky.domain.model.AppResult
import com.teledrive.sky.domain.repository.AuthRepository
import com.teledrive.sky.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI States ───────────────────────────────────────────────────────────────

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ── Login State ─────────────────────────────────────────────────────────
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    fun onEmailChange(email: String) {
        _loginState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _loginState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun login() {
        val state = _loginState.value
        var hasError = false

        if (!ValidationUtils.isValidEmail(state.email)) {
            _loginState.update { it.copy(emailError = "Format email tidak valid") }
            hasError = true
        }
        if (state.password.isBlank()) {
            _loginState.update { it.copy(passwordError = "Kata sandi tidak boleh kosong") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AppResult.Success -> {
                    _loginState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is AppResult.Error -> {
                    _loginState.update { it.copy(isLoading = false, generalError = result.message) }
                }
                else -> {}
            }
        }
    }

    // ── Register State ──────────────────────────────────────────────────────
    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState = _registerState.asStateFlow()

    fun onNameChange(name: String) {
        _registerState.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    fun onRegisterEmailChange(email: String) {
        _registerState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onRegisterPasswordChange(password: String) {
        _registerState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun register() {
        val state = _registerState.value
        var hasError = false

        if (!ValidationUtils.isValidName(state.name)) {
            _registerState.update { it.copy(nameError = "Nama tidak boleh kosong") }
            hasError = true
        }
        if (!ValidationUtils.isValidEmail(state.email)) {
            _registerState.update { it.copy(emailError = "Format email tidak valid") }
            hasError = true
        }
        if (state.password.length < 6) {
            _registerState.update { it.copy(passwordError = "Kata sandi minimal 6 karakter") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepository.register(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password
            )) {
                is AppResult.Success -> {
                    _registerState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is AppResult.Error -> {
                    _registerState.update { it.copy(isLoading = false, generalError = result.message) }
                }
                else -> {}
            }
        }
    }
}
