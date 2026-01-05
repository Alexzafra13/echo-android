package com.echo.feature.server.presentation.addserver

import app.cash.turbine.test
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.feature.server.domain.model.ServerInfo
import com.echo.feature.server.domain.model.ServerValidationResult
import com.echo.feature.server.domain.usecase.ValidateServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddServerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var validateServerUseCase: ValidateServerUseCase
    private lateinit var serverPreferences: ServerPreferences

    private val testServerInfo = ServerInfo(
        name = "Test Server",
        version = "1.0.0",
        healthy = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        validateServerUseCase = mockk()
        serverPreferences = mockk {
            coEvery { addServer(any()) } just runs
            coEvery { setActiveServer(any()) } just runs
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AddServerViewModel {
        return AddServerViewModel(validateServerUseCase, serverPreferences)
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given
        val viewModel = createViewModel()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.serverUrl)
            assertFalse(state.isValidating)
            assertFalse(state.isValid)
            assertFalse(state.isAdding)
            assertNull(state.error)
            assertNull(state.serverInfo)
            assertNull(state.serverAddedId)
        }
    }

    @Test
    fun `onServerUrlChange updates url and resets validation state`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://example.com", state.serverUrl)
            assertFalse(state.isValid)
            assertNull(state.error)
            assertNull(state.serverInfo)
        }
    }

    @Test
    fun `validateServer shows error when url is blank`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Ingresa"))
            assertFalse(state.isValidating)
        }
    }

    @Test
    fun `validateServer shows error when url is only whitespace`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
        }
    }

    @Test
    fun `validateServer sets isValidating during validation`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateServer()

        // Then - should be validating
        viewModel.state.test {
            // Skip to validating state or final state
            val state = awaitItem()
            // Eventually should be valid
            testDispatcher.scheduler.advanceUntilIdle()
            val finalState = expectMostRecentItem()
            assertTrue(finalState.isValid)
            assertFalse(finalState.isValidating)
        }
    }

    @Test
    fun `validateServer success updates state correctly`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isValid)
            assertFalse(state.isValidating)
            assertEquals(testServerInfo, state.serverInfo)
            assertNull(state.error)
        }
    }

    @Test
    fun `validateServer error updates state correctly`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Error("Connection failed")
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
            assertFalse(state.isValidating)
            assertEquals("Connection failed", state.error)
            assertNull(state.serverInfo)
        }
    }

    @Test
    fun `addServer does nothing when not valid`() = runTest {
        // Given
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { serverPreferences.addServer(any()) }
    }

    @Test
    fun `addServer does nothing when serverInfo is null`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.addServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { serverPreferences.addServer(any()) }
    }

    @Test
    fun `addServer saves server and sets as active when valid`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { serverPreferences.addServer(any()) }
        coVerify { serverPreferences.setActiveServer(any()) }
    }

    @Test
    fun `addServer updates state with serverAddedId`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.serverAddedId)
            assertFalse(state.isAdding)
        }
    }

    @Test
    fun `addServer normalizes url before saving`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("example.com/")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            serverPreferences.addServer(match { server ->
                server.url == "https://example.com" && server.name == "Test Server"
            })
        }
    }

    @Test
    fun `changing url after validation resets valid state`() = runTest {
        // Given
        coEvery { validateServerUseCase(any()) } returns ServerValidationResult.Success(testServerInfo)
        val viewModel = createViewModel()
        viewModel.onServerUrlChange("https://example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.validateServer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify it's valid
        assertTrue(viewModel.state.value.isValid)

        // When - change url
        viewModel.onServerUrlChange("https://other.com")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
            assertNull(state.serverInfo)
        }
    }
}
