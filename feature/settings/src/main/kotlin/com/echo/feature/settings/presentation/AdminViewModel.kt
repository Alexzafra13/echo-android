package com.echo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.feature.settings.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminState(
    // Server Status
    val serverOnline: Boolean = true,
    val serverVersion: String = "1.0.0",
    val activeSessions: Int = 0,

    // Library Stats
    val totalTracks: Int = 0,
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0,
    val totalPlaylists: Int = 0,

    // System Resources
    val cpuUsage: Int = 0,
    val memoryUsage: Int = 0,
    val diskUsage: Int = 0,

    // Config
    val serverName: String = "",
    val serverPort: Int = 4533,
    val transcodingEnabled: Boolean = true,
    val defaultQuality: String = "320kbps",
    val musicPath: String = "/music",
    val storageUsed: String = "0 GB",
    val storageAvailable: String = "0 GB",

    // Users
    val users: List<AdminUser> = emptyList(),

    // Logs
    val logs: List<AdminLog> = emptyList(),

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AdminUser(
    val id: String,
    val username: String,
    val isAdmin: Boolean,
    val isOnline: Boolean,
    val lastSeen: String? = null
)

data class AdminLog(
    val id: String,
    val level: LogLevel,
    val category: String = "",
    val message: String,
    val timestamp: String
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val serverPreferences: ServerPreferences,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        loadAdminData()
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val server = serverPreferences.activeServer.first()

                // Load server info
                _state.value = _state.value.copy(
                    serverOnline = true,
                    serverVersion = "Echo Server",
                    serverName = server?.name ?: "Echo Server",
                    serverPort = server?.url?.let { url ->
                        try { java.net.URL(url).port.takeIf { it != -1 } ?: 4533 } catch (_: Exception) { 4533 }
                    } ?: 4533
                )

                // Load logs from API
                loadLogs()

                // Load log stats
                loadLogStats()

                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private suspend fun loadLogs() {
        adminRepository.getLogs(take = 50)
            .onSuccess { logs ->
                _state.value = _state.value.copy(logs = logs)
            }
            .onFailure { error ->
                // If logs API fails, show empty list with error
                _state.value = _state.value.copy(
                    logs = emptyList(),
                    error = "Error cargando logs: ${error.message}"
                )
            }
    }

    private suspend fun loadLogStats() {
        adminRepository.getLogStats()
            .onSuccess { stats ->
                // Update any stats-related state if needed
                _state.value = _state.value.copy(
                    activeSessions = stats.total
                )
            }
    }

    fun filterLogs(level: String? = null, category: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            adminRepository.getLogs(level = level, category = category)
                .onSuccess { logs ->
                    _state.value = _state.value.copy(
                        logs = logs,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    fun refresh() {
        loadAdminData()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
