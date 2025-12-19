package com.echo.feature.auth.presentation.firstlogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.feature.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirstLoginState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    val isValid: Boolean
        get() = currentPassword.isNotBlank() &&
                newPassword.length >= 8 &&
                newPassword == confirmPassword

    val passwordMismatch: Boolean
        get() = confirmPassword.isNotBlank() && newPassword != confirmPassword

    val passwordTooShort: Boolean
        get() = newPassword.isNotBlank() && newPassword.length < 8
}

@HiltViewModel
class FirstLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FirstLoginState())
    val state: StateFlow<FirstLoginState> = _state.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _state.update { it.copy(currentPassword = value, error = null) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun changePassword() {
        val currentState = _state.value
        if (!currentState.isValid) {
            _state.update { it.copy(error = "Por favor, completa todos los campos correctamente") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.changePassword(
                currentPassword = currentState.currentPassword,
                newPassword = currentState.newPassword
            ).onSuccess {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { exception ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al cambiar la contraseña"
                    )
                }
            }
        }
    }
}
