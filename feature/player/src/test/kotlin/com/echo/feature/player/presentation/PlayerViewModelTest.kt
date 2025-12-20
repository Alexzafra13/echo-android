package com.echo.feature.player.presentation

import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.player.PlayerState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var player: EchoPlayer
    private lateinit var viewModel: PlayerViewModel
    private val playerStateFlow = MutableStateFlow(PlayerState())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        player = mockk(relaxed = true)
        every { player.state } returns playerStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playerState exposes player state`() = runTest {
        viewModel = PlayerViewModel(player)

        val testState = PlayerState(isPlaying = true, duration = 5000L)
        playerStateFlow.value = testState

        assertEquals(testState, viewModel.playerState.value)
    }

    @Test
    fun `togglePlayPause calls player`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.togglePlayPause()

        verify { player.togglePlayPause() }
    }

    @Test
    fun `seekTo calls player with position`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.seekTo(5000L)

        verify { player.seekTo(5000L) }
    }

    @Test
    fun `seekToProgress calculates correct position`() = runTest {
        playerStateFlow.value = PlayerState(duration = 10000L)
        viewModel = PlayerViewModel(player)

        viewModel.seekToProgress(0.5f)

        verify { player.seekTo(5000L) }
    }

    @Test
    fun `seekToProgress with zero duration seeks to zero`() = runTest {
        playerStateFlow.value = PlayerState(duration = 0L)
        viewModel = PlayerViewModel(player)

        viewModel.seekToProgress(0.5f)

        verify { player.seekTo(0L) }
    }

    @Test
    fun `skipToNext calls player`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.skipToNext()

        verify { player.seekToNext() }
    }

    @Test
    fun `skipToPrevious calls player`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.skipToPrevious()

        verify { player.seekToPrevious() }
    }

    @Test
    fun `toggleRepeatMode calls player`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.toggleRepeatMode()

        verify { player.toggleRepeatMode() }
    }

    @Test
    fun `toggleShuffle calls player`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.toggleShuffle()

        verify { player.toggleShuffle() }
    }

    @Test
    fun `skipToQueueItem calls player with index`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.skipToQueueItem(3)

        verify { player.skipToQueueItem(3) }
    }

    @Test
    fun `removeFromQueue calls player with index`() = runTest {
        viewModel = PlayerViewModel(player)

        viewModel.removeFromQueue(2)

        verify { player.removeFromQueue(2) }
    }
}
