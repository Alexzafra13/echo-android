package com.echo.feature.auth.presentation.login

import app.cash.turbine.test
import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.feature.auth.data.repository.AuthRepository
import com.echo.feature.auth.domain.model.LoginResult
import com.echo.feature.auth.domain.model.User
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var viewModel: LoginViewModel

    private val testServer = SavedServer(
        id = "server1",
        name = "Test Server",
        url = "https://echo.example.com",
        lastConnected = null
    )

    private val testUser = User(
        id = "user1",
        username = "testuser",
        name = "Test User",
        isAdmin = false,
        hasAvatar = false,
        mustChangePassword = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        serverPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel = LoginViewModel(authRepository, serverPreferences)

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.serverId)
            assertEquals("", state.username)
            assertEquals("", state.password)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.loginSuccess)
        }
    }

    @Test
    fun `loadServer updates state with server info`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("server1", state.serverId)
            assertEquals("Test Server", state.serverName)
            assertEquals("echo.example.com", state.serverUrl)
        }
    }

    @Test
    fun `loadServer with saved credentials fills username and password`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials("server1") } returns
            SessionPreferences.SavedCredentials("saveduser", "savedpass")

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("saveduser", state.username)
            assertEquals("savedpass", state.password)
            assertTrue(state.rememberCredentials)
        }
    }

    @Test
    fun `onUsernameChange updates username and clears error`() = runTest {
        viewModel = LoginViewModel(authRepository, serverPreferences)

        viewModel.onUsernameChange("newuser")

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("newuser", state.username)
            assertNull(state.error)
        }
    }

    @Test
    fun `onPasswordChange updates password and clears error`() = runTest {
        viewModel = LoginViewModel(authRepository, serverPreferences)

        viewModel.onPasswordChange("newpass")

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("newpass", state.password)
            assertNull(state.error)
        }
    }

    @Test
    fun `login with empty username shows error`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPasswordChange("somepassword")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Ingresa tu usuario", state.error)
            assertFalse(state.loginSuccess)
        }
    }

    @Test
    fun `login with empty password shows error`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("someuser")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Ingresa tu contraseña", state.error)
            assertFalse(state.loginSuccess)
        }
    }

    @Test
    fun `login success updates state correctly`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null
        coEvery {
            authRepository.login(any(), any(), any(), any())
        } returns LoginResult.Success(testUser, false)

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.loginSuccess)
            assertFalse(state.mustChangePassword)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `login success with mustChangePassword flag`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null
        coEvery {
            authRepository.login(any(), any(), any(), any())
        } returns LoginResult.Success(testUser.copy(mustChangePassword = true), true)

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("testpass")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.loginSuccess)
            assertTrue(state.mustChangePassword)
        }
    }

    @Test
    fun `login error updates state with error message`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null
        coEvery {
            authRepository.login(any(), any(), any(), any())
        } returns LoginResult.Error("Usuario o contraseña incorrectos")

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("testuser")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.loginSuccess)
            assertFalse(state.isLoading)
            assertEquals("Usuario o contraseña incorrectos", state.error)
        }
    }

    @Test
    fun `login calls repository with correct parameters`() = runTest {
        every { serverPreferences.servers } returns flowOf(listOf(testServer))
        coEvery { authRepository.getSavedCredentials(any()) } returns null
        coEvery {
            authRepository.login(any(), any(), any(), any())
        } returns LoginResult.Success(testUser, false)

        viewModel = LoginViewModel(authRepository, serverPreferences)
        viewModel.loadServer("server1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUsernameChange("myuser")
        viewModel.onPasswordChange("mypass")
        viewModel.onRememberCredentialsChange(true)
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            authRepository.login(
                server = testServer,
                username = "myuser",
                password = "mypass",
                rememberCredentials = true
            )
        }
    }
}
