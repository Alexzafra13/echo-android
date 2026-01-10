package com.echo.feature.settings.presentation

import app.cash.turbine.test
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.feature.settings.data.repository.AdminRepository
import com.echo.feature.settings.data.repository.LogStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var serverPreferences: ServerPreferences
    private lateinit var adminRepository: AdminRepository
    private lateinit var viewModel: AdminViewModel

    private val testServer = SavedServer(
        id = "server1",
        name = "Echo Server",
        url = "https://echo.example.com:4533",
        addedAt = System.currentTimeMillis()
    )

    private val testLogs = listOf(
        AdminLog(
            id = "1",
            level = LogLevel.INFO,
            category = "auth",
            message = "User logged in",
            timestamp = "2024-01-15 10:30:00"
        ),
        AdminLog(
            id = "2",
            level = LogLevel.WARNING,
            category = "playback",
            message = "Buffer underrun detected",
            timestamp = "2024-01-15 10:31:00"
        ),
        AdminLog(
            id = "3",
            level = LogLevel.ERROR,
            category = "database",
            message = "Connection timeout",
            timestamp = "2024-01-15 10:32:00"
        )
    )

    private val testLogStats = LogStats(
        total = 150,
        errorCount = 10,
        warningCount = 25,
        infoCount = 115
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        serverPreferences = mockk(relaxed = true)
        adminRepository = mockk(relaxed = true)

        every { serverPreferences.activeServer } returns flowOf(testServer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows loading`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)

        // Then - initial state should be loading
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadAdminData loads server info and logs on success`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.serverOnline)
            assertEquals("Echo Server", state.serverName)
            assertEquals(4533, state.serverPort)
            assertEquals(testLogs, state.logs)
            assertEquals(150, state.activeSessions)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadAdminData handles logs API failure gracefully`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns
            Result.failure(Exception("Network error"))
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.logs.isEmpty())
            assertTrue(state.error?.contains("Error cargando logs") == true)
        }
    }

    @Test
    fun `loadAdminData extracts port from URL correctly`() = runTest {
        // Given
        val serverWithPort = testServer.copy(url = "https://echo.example.com:8080")
        every { serverPreferences.activeServer } returns flowOf(serverWithPort)
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(8080, state.serverPort)
        }
    }

    @Test
    fun `loadAdminData defaults to port 4533 when not specified`() = runTest {
        // Given
        val serverNoPort = testServer.copy(url = "https://echo.example.com")
        every { serverPreferences.activeServer } returns flowOf(serverNoPort)
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(4533, state.serverPort)
        }
    }

    @Test
    fun `filterLogs calls repository with correct parameters`() = runTest {
        // Given
        val filteredLogs = listOf(testLogs[2]) // Only error logs
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogs(any(), any(), eq("ERROR"), any()) } returns Result.success(filteredLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterLogs(level = "ERROR")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { adminRepository.getLogs(level = "ERROR", category = null) }
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(filteredLogs, state.logs)
        }
    }

    @Test
    fun `filterLogs by category works correctly`() = runTest {
        // Given
        val authLogs = listOf(testLogs[0]) // Only auth logs
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogs(any(), any(), any(), eq("auth")) } returns Result.success(authLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterLogs(category = "auth")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { adminRepository.getLogs(level = null, category = "auth") }
    }

    @Test
    fun `filterLogs handles failure`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogs(any(), any(), eq("ERROR"), any()) } returns
            Result.failure(Exception("Filter failed"))
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterLogs(level = "ERROR")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Filter failed", state.error)
        }
    }

    @Test
    fun `refresh reloads all data`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should call getLogs twice (init + refresh)
        coVerify(exactly = 2) { adminRepository.getLogs(take = 50) }
        coVerify(exactly = 2) { adminRepository.getLogStats() }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        // Given
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns
            Result.failure(Exception("Test error"))
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error exists
        viewModel.state.test {
            val stateWithError = awaitItem()
            assertTrue(stateWithError.error != null)
        }

        // When
        viewModel.clearError()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `handles null server gracefully`() = runTest {
        // Given
        every { serverPreferences.activeServer } returns flowOf(null)
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Echo Server", state.serverName) // Default name
            assertEquals(4533, state.serverPort) // Default port
        }
    }

    @Test
    fun `log levels are mapped correctly from DTO`() = runTest {
        // Given - logs already have mapped levels in testLogs
        coEvery { adminRepository.getLogs(any(), any(), any(), any()) } returns Result.success(testLogs)
        coEvery { adminRepository.getLogStats() } returns Result.success(testLogStats)

        // When
        viewModel = AdminViewModel(serverPreferences, adminRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(LogLevel.INFO, state.logs[0].level)
            assertEquals(LogLevel.WARNING, state.logs[1].level)
            assertEquals(LogLevel.ERROR, state.logs[2].level)
        }
    }
}
