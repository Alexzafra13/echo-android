package com.echo.feature.auth.presentation.firstlogin

import app.cash.turbine.test
import com.echo.feature.auth.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class FirstLoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: FirstLoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = FirstLoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.currentPassword)
            assertEquals("", state.newPassword)
            assertEquals("", state.confirmPassword)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.isSuccess)
        }
    }

    @Test
    fun `onCurrentPasswordChange updates current password and clears error`() = runTest {
        // When
        viewModel.onCurrentPasswordChange("oldpass")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("oldpass", state.currentPassword)
            assertNull(state.error)
        }
    }

    @Test
    fun `onNewPasswordChange updates new password and clears error`() = runTest {
        // When
        viewModel.onNewPasswordChange("newpassword123")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("newpassword123", state.newPassword)
            assertNull(state.error)
        }
    }

    @Test
    fun `onConfirmPasswordChange updates confirm password and clears error`() = runTest {
        // When
        viewModel.onConfirmPasswordChange("newpassword123")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("newpassword123", state.confirmPassword)
            assertNull(state.error)
        }
    }

    @Test
    fun `isValid returns true when all fields are correct`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpass")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isValid)
        }
    }

    @Test
    fun `isValid returns false when current password is empty`() = runTest {
        // Given
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
        }
    }

    @Test
    fun `isValid returns false when new password is less than 8 chars`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpass")
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
            assertTrue(state.passwordTooShort)
        }
    }

    @Test
    fun `isValid returns false when passwords do not match`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpass")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("differentpassword")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
            assertTrue(state.passwordMismatch)
        }
    }

    @Test
    fun `passwordMismatch is false when confirm is empty`() = runTest {
        // Given
        viewModel.onNewPasswordChange("newpassword123")
        // confirmPassword is empty

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.passwordMismatch)
        }
    }

    @Test
    fun `passwordTooShort is false when new password is empty`() = runTest {
        // Given - newPassword is empty by default

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.passwordTooShort)
        }
    }

    @Test
    fun `changePassword sets error when state is not valid`() = runTest {
        // Given - empty fields
        viewModel.onCurrentPasswordChange("")
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")

        // When
        viewModel.changePassword()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Por favor, completa todos los campos correctamente", state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `changePassword calls repository on valid state`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")
        coEvery { authRepository.changePassword("oldpassword", "newpassword123") } returns Result.success(Unit)

        // When
        viewModel.changePassword()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { authRepository.changePassword("oldpassword", "newpassword123") }
    }

    @Test
    fun `changePassword sets success state on success`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")
        coEvery { authRepository.changePassword(any(), any()) } returns Result.success(Unit)

        // When
        viewModel.changePassword()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isSuccess)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `changePassword sets error state on failure`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")
        coEvery { authRepository.changePassword(any(), any()) } returns
            Result.failure(Exception("Contraseña actual incorrecta"))

        // When
        viewModel.changePassword()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSuccess)
            assertFalse(state.isLoading)
            assertEquals("Contraseña actual incorrecta", state.error)
        }
    }

    @Test
    fun `changePassword shows loading state while processing`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")
        coEvery { authRepository.changePassword(any(), any()) } returns Result.success(Unit)

        // When
        viewModel.changePassword()

        // Then - immediately after call, should be loading
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `changePassword uses default error message when exception has no message`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpassword")
        viewModel.onNewPasswordChange("newpassword123")
        viewModel.onConfirmPasswordChange("newpassword123")
        coEvery { authRepository.changePassword(any(), any()) } returns
            Result.failure(Exception())

        // When
        viewModel.changePassword()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Error al cambiar la contraseña", state.error)
        }
    }

    @Test
    fun `password validation edge case - exactly 8 characters is valid`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpass")
        viewModel.onNewPasswordChange("exactly8")
        viewModel.onConfirmPasswordChange("exactly8")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isValid)
            assertFalse(state.passwordTooShort)
        }
    }

    @Test
    fun `password validation edge case - 7 characters is too short`() = runTest {
        // Given
        viewModel.onCurrentPasswordChange("oldpass")
        viewModel.onNewPasswordChange("seven77")
        viewModel.onConfirmPasswordChange("seven77")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isValid)
            assertTrue(state.passwordTooShort)
        }
    }

    @Test
    fun `updating fields clears previous error`() = runTest {
        // Given - trigger an error first
        viewModel.changePassword() // Will fail with empty fields

        viewModel.state.test {
            val stateWithError = awaitItem()
            assertTrue(stateWithError.error != null)
        }

        // When - update any field
        viewModel.onCurrentPasswordChange("test")

        // Then - error should be cleared
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}
