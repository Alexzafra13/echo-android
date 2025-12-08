package com.echo.feature.server.presentation.addserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.feature.server.domain.model.ServerInfo
import com.echo.feature.server.domain.model.ServerValidationResult
import com.echo.feature.server.domain.usecase.ValidateServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddServerState(
    val serverUrl: String = "",
    val isValidating: Boolean = false,
    val isValid: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val serverInfo: ServerInfo? = null,
    val serverAddedId: String? = null
)

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val validateServerUseCase: ValidateServerUseCase,
    private val serverPreferences: ServerPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AddServerState())
    val state: StateFlow<AddServerState> = _state.asStateFlow()

    fun onServerUrlChange(url: String) {
        _state.update {
            it.copy(
                serverUrl = url,
                isValid = false,
                error = null,
                serverInfo = null
            )
        }
    }

    fun validateServer() {
        val url = _state.value.serverUrl.trim()
        if (url.isBlank()) {
            _state.update { it.copy(error = "Ingresa la dirección del servidor") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isValidating = true, error = null) }

            when (val result = validateServerUseCase(url)) {
                is ServerValidationResult.Success -> {
                    _state.update {
                        it.copy(
                            isValidating = false,
                            isValid = true,
                            serverInfo = result.serverInfo,
                            error = null
                        )
                    }
                }
                is ServerValidationResult.Error -> {
                    _state.update {
                        it.copy(
                            isValidating = false,
                            isValid = false,
                            error = result.message,
                            serverInfo = null
                        )
                    }
                }
            }
        }
    }

    fun addServer() {
        val currentState = _state.value
        if (!currentState.isValid || currentState.serverInfo == null) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAdding = true) }

            val serverId = UUID.randomUUID().toString()
            val normalizedUrl = ValidateServerUseCase.normalizeUrl(currentState.serverUrl)

            val server = SavedServer(
                id = serverId,
                name = currentState.serverInfo.name,
                url = normalizedUrl,
                addedAt = System.currentTimeMillis()
            )

            serverPreferences.addServer(server)
            serverPreferences.setActiveServer(serverId)

            _state.update {
                it.copy(
                    isAdding = false,
                    serverAddedId = serverId
                )
            }
        }
    }
}
