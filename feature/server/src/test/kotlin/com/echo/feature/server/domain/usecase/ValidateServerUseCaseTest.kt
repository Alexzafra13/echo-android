package com.echo.feature.server.domain.usecase

import com.echo.feature.server.domain.model.ServerValidationResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateServerUseCaseTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var validateServerUseCase: ValidateServerUseCase
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        validateServerUseCase = ValidateServerUseCase(json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `invoke returns Success when server responds with valid health`() = runTest {
        // Given
        val healthResponse = """{"status": "ok", "version": "1.0.0", "name": "Echo Server"}"""
        mockWebServer.enqueue(MockResponse().setBody(healthResponse).setResponseCode(200))

        val serverUrl = mockWebServer.url("/").toString().trimEnd('/')

        // When
        val result = validateServerUseCase(serverUrl)

        // Then
        assertTrue(result is ServerValidationResult.Success)
        val success = result as ServerValidationResult.Success
        assertEquals("Echo Server", success.serverInfo.name)
        assertEquals("1.0.0", success.serverInfo.version)
        assertTrue(success.serverInfo.healthy)
    }

    @Test
    fun `invoke returns Success with extracted name when server has no name in response`() = runTest {
        // Given
        val healthResponse = """{"status": "healthy"}"""
        mockWebServer.enqueue(MockResponse().setBody(healthResponse).setResponseCode(200))

        val serverUrl = mockWebServer.url("/").toString().trimEnd('/')

        // When
        val result = validateServerUseCase(serverUrl)

        // Then
        assertTrue(result is ServerValidationResult.Success)
        val success = result as ServerValidationResult.Success
        assertNotNull(success.serverInfo.name)
        assertTrue(success.serverInfo.healthy)
    }

    @Test
    fun `invoke returns Success even with empty body`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setBody("").setResponseCode(200))

        val serverUrl = mockWebServer.url("/").toString().trimEnd('/')

        // When
        val result = validateServerUseCase(serverUrl)

        // Then
        assertTrue(result is ServerValidationResult.Success)
    }

    @Test
    fun `invoke returns Error when server responds with error code`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val serverUrl = mockWebServer.url("/").toString().trimEnd('/')

        // When
        val result = validateServerUseCase(serverUrl)

        // Then
        assertTrue(result is ServerValidationResult.Error)
        val error = result as ServerValidationResult.Error
        assertTrue(error.message.contains("500"))
    }

    @Test
    fun `invoke returns Error when server responds with 404`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val serverUrl = mockWebServer.url("/").toString().trimEnd('/')

        // When
        val result = validateServerUseCase(serverUrl)

        // Then
        assertTrue(result is ServerValidationResult.Error)
        val error = result as ServerValidationResult.Error
        assertTrue(error.message.contains("404"))
    }

    @Test
    fun `invoke returns Error for unknown host`() = runTest {
        // When
        val result = validateServerUseCase("https://this-server-does-not-exist-12345.invalid")

        // Then
        assertTrue(result is ServerValidationResult.Error)
        val error = result as ServerValidationResult.Error
        assertTrue(error.message.contains("No se puede encontrar"))
    }

    @Test
    fun `normalizeUrl adds https when no scheme provided`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("example.com")

        // Then
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl preserves http scheme`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("http://example.com")

        // Then
        assertEquals("http://example.com", result)
    }

    @Test
    fun `normalizeUrl preserves https scheme`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("https://example.com")

        // Then
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl removes trailing slash`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("https://example.com/")

        // Then
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl trims whitespace`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("  https://example.com  ")

        // Then
        assertEquals("https://example.com", result)
    }

    @Test
    fun `normalizeUrl handles complex URLs`() {
        // When
        val result = ValidateServerUseCase.normalizeUrl("  example.com:8080/  ")

        // Then
        assertEquals("https://example.com:8080", result)
    }
}
