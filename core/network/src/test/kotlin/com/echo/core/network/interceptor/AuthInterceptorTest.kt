package com.echo.core.network.interceptor

import com.echo.core.datastore.preferences.SessionData
import com.echo.core.datastore.preferences.SessionPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var client: OkHttpClient

    private val sessionFlow = MutableStateFlow<SessionData?>(null)

    private fun createTestSession(accessToken: String): SessionData {
        return SessionData(
            serverId = "server-1",
            userId = "user-1",
            username = "testuser",
            accessToken = accessToken,
            refreshToken = "refresh-token",
            expiresAt = System.currentTimeMillis() + 3600000
        )
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sessionPreferences = mockk {
            every { session } returns sessionFlow
        }

        authInterceptor = AuthInterceptor(sessionPreferences)

        client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `adds Authorization header when token is present`() {
        // Given
        sessionFlow.value = createTestSession("test-token-123")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/albums"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer test-token-123", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `does not add Authorization header when token is null`() {
        // Given
        sessionFlow.value = null
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/albums"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `skips auth for login endpoint`() {
        // Given
        sessionFlow.value = createTestSession("test-token")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/auth/login"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `skips auth for health endpoint`() {
        // Given
        sessionFlow.value = createTestSession("test-token")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/health"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `skips auth for setup endpoints`() {
        // Given
        sessionFlow.value = createTestSession("test-token")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/setup/status"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `adds auth for regular API endpoints`() {
        // Given
        sessionFlow.value = createTestSession("my-token")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/playlists"))
            .build()

        // When
        client.newCall(request).execute()

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer my-token", recordedRequest.getHeader("Authorization"))
    }
}
