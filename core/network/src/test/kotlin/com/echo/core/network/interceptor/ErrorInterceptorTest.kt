package com.echo.core.network.interceptor

import com.echo.core.common.error.AppError
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ErrorInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var errorInterceptor: ErrorInterceptor
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        errorInterceptor = ErrorInterceptor()

        client = OkHttpClient.Builder()
            .addInterceptor(errorInterceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `successful response passes through`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When
        val response = client.newCall(request).execute()

        // Then
        assertTrue(response.isSuccessful)
        assertEquals(200, response.code)
    }

    @Test
    fun `401 response passes through without throwing`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When
        val response = client.newCall(request).execute()

        // Then
        assertEquals(401, response.code)
    }

    @Test
    fun `403 response throws Forbidden error`() {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"message": "Access denied"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.Forbidden")
        } catch (e: AppError.Api.Forbidden) {
            assertTrue(e.message.contains("Access denied"))
        }
    }

    @Test
    fun `403 with password message throws MustChangePassword error`() {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"message": "You must change your password"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Auth.MustChangePassword")
        } catch (e: AppError.Auth.MustChangePassword) {
            // Success
        }
    }

    @Test
    fun `404 response throws NotFound error`() {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Resource not found"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.NotFound")
        } catch (e: AppError.Api.NotFound) {
            assertTrue(e.message.contains("Resource not found"))
        }
    }

    @Test
    fun `429 response throws RateLimited error`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(429))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.RateLimited")
        } catch (e: AppError.Api.RateLimited) {
            assertEquals(429, e.code)
        }
    }

    @Test
    fun `500 response throws ServerError`() {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"message": "Internal server error"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.ServerError")
        } catch (e: AppError.Api.ServerError) {
            assertTrue(e.message.contains("Internal server error"))
        }
    }

    @Test
    fun `502 response throws ServerError`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(502))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.ServerError")
        } catch (e: AppError.Api.ServerError) {
            // Success - 502 is in 500-599 range
        }
    }

    @Test
    fun `other error codes throw Unknown error`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(418)) // I'm a teapot

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.Unknown")
        } catch (e: AppError.Api.Unknown) {
            assertEquals(418, e.code)
        }
    }

    @Test
    fun `handles empty error body`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.NotFound")
        } catch (e: AppError.Api.NotFound) {
            // Uses default message when body is empty
            assertTrue(e.message.isNotBlank())
        }
    }

    @Test
    fun `handles malformed JSON in error body`() {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Not valid JSON")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        // When/Then
        try {
            client.newCall(request).execute()
            throw AssertionError("Expected AppError.Api.ServerError")
        } catch (e: AppError.Api.ServerError) {
            // Should still throw, using default message
            assertTrue(e.message.isNotBlank())
        }
    }
}
