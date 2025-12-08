package com.echo.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.feature.auth.data.repository.AuthRepository
import com.echo.feature.auth.domain.model.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val serverId: String? = null,
    val serverName: String? = null,
    val serverUrl: String? = null,
    val username: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val mustChangePassword: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverPreferences: ServerPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var currentServer: SavedServer? = null

    fun loadServer(serverId: String) {
        viewModelScope.launch {
            val servers = serverPreferences.servers.first()
            val server = servers.find { it.id == serverId }

            if (server != null) {
                currentServer = server
                serverPreferences.setActiveServer(serverId)

                // Check for saved credentials
                val savedCreds = authRepository.getSavedCredentials(serverId)

                _state.update {
                    it.copy(
                        serverId = serverId,
                        serverName = server.name,
                        serverUrl = server.url.removePrefix("https://").removePrefix("http://"),
                        username = savedCreds?.username ?: "",
                        password = savedCreds?.password ?: "",
                        rememberCredentials = savedCreds != null
                    )
                }
            }
        }
    }

    fun onUsernameChange(username: String) {
        _state.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onRememberCredentialsChange(remember: Boolean) {
        _state.update { it.copy(rememberCredentials = remember) }
    }

    fun login() {
        val server = currentServer ?: return
        val currentState = _state.value

        if (currentState.username.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu usuario") }
            return
        }

        if (currentState.password.isBlank()) {
            _state.update { it.copy(error = "Ingresa tu contraseña") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.login(
                server = server,
                username = currentState.username,
                password = currentState.password,
                rememberCredentials = currentState.rememberCredentials
            )) {
                is LoginResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            mustChangePassword = result.mustChangePassword
                        )
                    }
                }
                is LoginResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
}
