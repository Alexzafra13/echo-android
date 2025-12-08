package com.echo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.ServerPreferences
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
    private val serverPreferences: ServerPreferences
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

                // For now, populate with mock data
                // In production, this would call the admin API endpoints
                _state.value = _state.value.copy(
                    serverOnline = true,
                    serverVersion = "Echo Server 1.0.0",
                    serverName = server?.name ?: "Echo Server",
                    activeSessions = 3,

                    // Mock library stats
                    totalTracks = 15420,
                    totalAlbums = 1253,
                    totalArtists = 487,
                    totalPlaylists = 42,

                    // Mock system resources
                    cpuUsage = 23,
                    memoryUsage = 45,
                    diskUsage = 67,

                    // Config
                    serverPort = 4533,
                    transcodingEnabled = true,
                    defaultQuality = "320kbps",
                    musicPath = "/data/music",
                    storageUsed = "234.5 GB",
                    storageAvailable = "765.5 GB",

                    // Mock users
                    users = listOf(
                        AdminUser(
                            id = "1",
                            username = "admin",
                            isAdmin = true,
                            isOnline = true
                        ),
                        AdminUser(
                            id = "2",
                            username = "usuario1",
                            isAdmin = false,
                            isOnline = true
                        ),
                        AdminUser(
                            id = "3",
                            username = "usuario2",
                            isAdmin = false,
                            isOnline = false,
                            lastSeen = "Hace 2 horas"
                        ),
                        AdminUser(
                            id = "4",
                            username = "usuario3",
                            isAdmin = false,
                            isOnline = false,
                            lastSeen = "Hace 1 día"
                        )
                    ),

                    // Mock logs
                    logs = listOf(
                        AdminLog(
                            id = "1",
                            level = LogLevel.INFO,
                            message = "Servidor iniciado correctamente",
                            timestamp = "2024-01-15 10:30:00"
                        ),
                        AdminLog(
                            id = "2",
                            level = LogLevel.INFO,
                            message = "Usuario 'admin' ha iniciado sesión",
                            timestamp = "2024-01-15 10:35:22"
                        ),
                        AdminLog(
                            id = "3",
                            level = LogLevel.WARNING,
                            message = "Uso de memoria elevado (85%)",
                            timestamp = "2024-01-15 11:00:00"
                        ),
                        AdminLog(
                            id = "4",
                            level = LogLevel.INFO,
                            message = "Escaneo de biblioteca completado: 125 nuevas canciones",
                            timestamp = "2024-01-15 12:00:00"
                        ),
                        AdminLog(
                            id = "5",
                            level = LogLevel.ERROR,
                            message = "Error al transcodificar: archivo corrupto",
                            timestamp = "2024-01-15 12:15:33"
                        ),
                        AdminLog(
                            id = "6",
                            level = LogLevel.INFO,
                            message = "Backup automático completado",
                            timestamp = "2024-01-15 03:00:00"
                        )
                    ),

                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() {
        loadAdminData()
    }
}
