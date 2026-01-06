package com.echo.core.network.interceptor

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles automatic token refresh when receiving 401 Unauthorized responses.
 * Uses OkHttp's Authenticator to retry failed requests with a new token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionPreferences: SessionPreferences,
    private val serverPreferences: ServerPreferences,
    private val json: Json
) : Authenticator {

    // Simple HTTP client for refresh requests (no interceptors to avoid loops)
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long? = null
    )

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite loops - only retry once
        if (responseCount(response) >= 2) {
            return null
        }

        val currentSession = sessionPreferences.session.value ?: return null
        val refreshToken = currentSession.refreshToken

        // Get the base URL for the refresh endpoint
        val baseUrl = runBlocking {
            serverPreferences.activeServer.first()?.url
        } ?: return null

        // Attempt to refresh the token
        val newTokens = refreshTokenSync(baseUrl, refreshToken) ?: run {
            // Refresh failed - clear session and let the user re-login
            runBlocking { sessionPreferences.clearSession() }
            return null
        }

        // Update stored tokens
        val expiresAt = System.currentTimeMillis() + ((newTokens.expiresIn ?: 3600) * 1000)
        sessionPreferences.updateTokens(
            accessToken = newTokens.accessToken,
            refreshToken = newTokens.refreshToken,
            expiresAt = expiresAt
        )

        // Retry the original request with the new token
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    private fun refreshTokenSync(baseUrl: String, refreshToken: String): RefreshResponse? {
        return try {
            val requestBody = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/auth/refresh")
                .post(requestBody)
                .build()

            val response = refreshClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    json.decodeFromString(RefreshResponse.serializer(), body)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}
