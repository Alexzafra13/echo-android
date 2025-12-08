package com.echo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.datastore.preferences.ThemeMode
import com.echo.core.datastore.preferences.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val serverUrl: String = "",
    val serverName: String = "",
    val username: String = "",
    val isAdmin: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "1.0.0",
    val showLogoutDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val serverPreferences: ServerPreferences,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val _showLogoutDialog = MutableStateFlow(false)

    val state: StateFlow<SettingsState> = combine(
        themePreferences.themeMode,
        serverPreferences.activeServer,
        sessionPreferences.session,
        _showLogoutDialog
    ) { themeMode, server, session, showLogout ->
        SettingsState(
            serverUrl = server?.url ?: "",
            serverName = server?.name ?: "Echo Server",
            username = session?.username ?: "",
            isAdmin = session?.isAdmin ?: false,
            themeMode = themeMode,
            showLogoutDialog = showLogout
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun showLogoutDialog() {
        _showLogoutDialog.value = true
    }

    fun hideLogoutDialog() {
        _showLogoutDialog.value = false
    }

    fun logout() {
        viewModelScope.launch {
            sessionPreferences.clearSession()
            _showLogoutDialog.value = false
        }
    }
}
