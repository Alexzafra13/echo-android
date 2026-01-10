package com.echo.feature.settings.presentation

import app.cash.turbine.test
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionData
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.datastore.preferences.ThemeMode
import com.echo.core.datastore.preferences.ThemePreferences
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var themePreferences: ThemePreferences
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var viewModel: SettingsViewModel

    private val testServer = SavedServer(
        id = "server1",
        name = "Test Server",
        url = "https://test.echo.com",
        addedAt = System.currentTimeMillis()
    )

    private val testSession = SessionData(
        serverId = "server1",
        userId = "user1",
        username = "testuser",
        accessToken = "token",
        refreshToken = "refresh",
        expiresAt = System.currentTimeMillis() + 3600000,
        isAdmin = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        themePreferences = mockk(relaxed = true)
        serverPreferences = mockk(relaxed = true)
        sessionPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state combines preferences correctly`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.DARK)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)

        // When
        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("https://test.echo.com", state.serverUrl)
            assertEquals("Test Server", state.serverName)
            assertEquals("testuser", state.username)
            assertTrue(state.isAdmin)
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertFalse(state.showLogoutDialog)
        }
    }

    @Test
    fun `setThemeMode updates theme preference`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)
        coEvery { themePreferences.setThemeMode(any()) } just Runs

        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { themePreferences.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `showLogoutDialog sets showLogoutDialog to true`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)

        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showLogoutDialog()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showLogoutDialog)
        }
    }

    @Test
    fun `hideLogoutDialog sets showLogoutDialog to false`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)

        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Show then hide
        viewModel.showLogoutDialog()
        viewModel.hideLogoutDialog()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showLogoutDialog)
        }
    }

    @Test
    fun `logout clears session and hides dialog`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)
        coEvery { sessionPreferences.clearSession() } just Runs

        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showLogoutDialog()

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { sessionPreferences.clearSession() }
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showLogoutDialog)
        }
    }

    @Test
    fun `state handles null server gracefully`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(null)
        every { sessionPreferences.session } returns MutableStateFlow(null)

        // When
        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.serverUrl)
            assertEquals("Echo Server", state.serverName)
            assertEquals("", state.username)
            assertFalse(state.isAdmin)
        }
    }

    @Test
    fun `state handles non-admin user`() = runTest {
        // Given
        val nonAdminSession = testSession.copy(isAdmin = false)
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(nonAdminSession)

        // When
        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isAdmin)
        }
    }

    @Test
    fun `setThemeMode cycles through all modes`() = runTest {
        // Given
        every { themePreferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { serverPreferences.activeServer } returns flowOf(testServer)
        every { sessionPreferences.session } returns MutableStateFlow(testSession)
        coEvery { themePreferences.setThemeMode(any()) } just Runs

        viewModel = SettingsViewModel(themePreferences, serverPreferences, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - cycle through all modes
        viewModel.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { themePreferences.setThemeMode(ThemeMode.LIGHT) }
        coVerify { themePreferences.setThemeMode(ThemeMode.DARK) }
        coVerify { themePreferences.setThemeMode(ThemeMode.SYSTEM) }
    }
}
