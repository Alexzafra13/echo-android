package com.echo.feature.auth.data.repository

import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionData
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.auth.data.api.AuthApi
import com.echo.feature.auth.data.dto.LoginRequest
import com.echo.feature.auth.domain.model.LoginResult
import com.echo.feature.auth.domain.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val sessionPreferences: SessionPreferences,
    private val serverPreferences: ServerPreferences
) {
    suspend fun login(
        server: SavedServer,
        username: String,
        password: String,
        rememberCredentials: Boolean = false
    ): LoginResult {
        return try {
            val api = apiClientFactory.getClient(server.url).create(AuthApi::class.java)
            val response = api.login(LoginRequest(username, password))

            // Calculate expiration time (default 1 hour if not provided)
            val expiresAt = System.currentTimeMillis() + (response.expiresIn ?: 3600) * 1000

            // Save session
            val sessionData = SessionData(
                serverId = server.id,
                userId = response.user.id,
                username = response.user.username,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAt = expiresAt,
                isAdmin = response.user.isAdmin,
                mustChangePassword = response.user.mustChangePassword
            )
            sessionPreferences.saveSession(sessionData)

            // Update last connected
            serverPreferences.updateLastConnected(server.id)

            // Save credentials if requested
            if (rememberCredentials) {
                sessionPreferences.saveCredentials(server.id, username, password)
            }

            val user = User(
                id = response.user.id,
                username = response.user.username,
                name = response.user.name,
                isAdmin = response.user.isAdmin,
                hasAvatar = response.user.hasAvatar,
                mustChangePassword = response.user.mustChangePassword
            )

            LoginResult.Success(user, response.user.mustChangePassword)

        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> LoginResult.Error("Usuario o contraseña incorrectos")
                429 -> LoginResult.Error("Demasiados intentos. Espera un momento.")
                else -> LoginResult.Error("Error del servidor: ${e.code()}")
            }
        } catch (e: java.net.UnknownHostException) {
            LoginResult.Error("No se puede conectar al servidor")
        } catch (e: java.net.SocketTimeoutException) {
            LoginResult.Error("El servidor no responde")
        } catch (e: Exception) {
            LoginResult.Error(e.localizedMessage ?: "Error desconocido")
        }
    }

    suspend fun logout() {
        sessionPreferences.clearSession()
        apiClientFactory.clearAllClients()
    }

    suspend fun getCurrentServer(): SavedServer? {
        return serverPreferences.activeServer.first()
    }

    fun getSavedCredentials(serverId: String) = sessionPreferences.getCredentials(serverId)
}
