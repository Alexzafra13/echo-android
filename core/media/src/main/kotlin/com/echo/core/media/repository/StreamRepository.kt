package com.echo.core.media.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.media.api.StreamApi
import com.echo.core.network.api.ApiClientFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(
    private val serverPreferences: ServerPreferences,
    private val apiClientFactory: ApiClientFactory
) {
    private val _streamToken = MutableStateFlow<String?>(null)
    val streamToken: StateFlow<String?> = _streamToken.asStateFlow()

    private suspend fun getApi(): StreamApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(StreamApi::class.java)
    }

    private suspend fun getBaseUrl(): String {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return server.url.trimEnd('/')
    }

    suspend fun ensureStreamToken(): String {
        // Return cached token if available
        _streamToken.value?.let { return it }

        val api = getApi()

        try {
            // Try to get existing token
            val existingToken = api.getStreamToken()
            if (existingToken != null) {
                _streamToken.value = existingToken.token
                return existingToken.token
            }
        } catch (e: Exception) {
            // Token doesn't exist or error, generate new one
        }

        // Generate new token
        val newToken = api.generateStreamToken()
        _streamToken.value = newToken.token
        return newToken.token
    }

    suspend fun getStreamUrl(trackId: String): String {
        val token = ensureStreamToken()
        val baseUrl = getBaseUrl()
        return "$baseUrl/api/tracks/$trackId/stream?token=$token"
    }

    fun clearToken() {
        _streamToken.value = null
    }
}
