package com.echo.feature.server.presentation.welcome

import app.cash.turbine.test
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var serversFlow: MutableStateFlow<List<SavedServer>>

    private val testServer = SavedServer(
        id = "server-1",
        name = "Test Server",
        url = "https://test.example.com",
        addedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        serversFlow = MutableStateFlow(emptyList())
        serverPreferences = mockk {
            every { servers } returns serversFlow
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `servers initially emits empty list`() = runTest {
        // Given
        val viewModel = WelcomeViewModel(serverPreferences)

        // Then
        viewModel.servers.test {
            val initialServers = awaitItem()
            assertTrue(initialServers.isEmpty())
        }
    }

    @Test
    fun `servers emits list when preferences has servers`() = runTest {
        // Given
        serversFlow.value = listOf(testServer)
        val viewModel = WelcomeViewModel(serverPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.servers.test {
            val servers = awaitItem()
            assertEquals(1, servers.size)
            assertEquals(testServer, servers[0])
        }
    }

    @Test
    fun `servers updates when preferences changes`() = runTest {
        // Given
        val viewModel = WelcomeViewModel(serverPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val newServer = SavedServer(
            id = "server-2",
            name = "New Server",
            url = "https://new.example.com",
            addedAt = System.currentTimeMillis()
        )
        serversFlow.value = listOf(testServer, newServer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.servers.test {
            val servers = awaitItem()
            assertEquals(2, servers.size)
        }
    }

    @Test
    fun `servers handles multiple servers correctly`() = runTest {
        // Given
        val servers = listOf(
            SavedServer("1", "Server 1", "https://s1.com", System.currentTimeMillis()),
            SavedServer("2", "Server 2", "https://s2.com", System.currentTimeMillis()),
            SavedServer("3", "Server 3", "https://s3.com", System.currentTimeMillis())
        )
        serversFlow.value = servers

        val viewModel = WelcomeViewModel(serverPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.servers.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Server 1", result[0].name)
            assertEquals("Server 2", result[1].name)
            assertEquals("Server 3", result[2].name)
        }
    }
}
