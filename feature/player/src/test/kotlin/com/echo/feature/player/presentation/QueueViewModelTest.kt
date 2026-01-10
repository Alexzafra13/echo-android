package com.echo.feature.player.presentation

import app.cash.turbine.test
import com.echo.core.media.model.PlayableTrack
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.player.PlayerState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var player: EchoPlayer
    private lateinit var viewModel: QueueViewModel

    private val testTracks = listOf(
        PlayableTrack(
            id = "track1",
            title = "Track One",
            artist = "Artist One",
            albumId = "album1",
            albumTitle = "Album One",
            duration = 180000L,
            trackNumber = 1,
            coverUrl = "https://example.com/cover1.jpg",
            streamUrl = "https://example.com/stream1.mp3"
        ),
        PlayableTrack(
            id = "track2",
            title = "Track Two",
            artist = "Artist Two",
            albumId = "album1",
            albumTitle = "Album One",
            duration = 240000L,
            trackNumber = 2,
            coverUrl = "https://example.com/cover1.jpg",
            streamUrl = "https://example.com/stream2.mp3"
        ),
        PlayableTrack(
            id = "track3",
            title = "Track Three",
            artist = "Artist Three",
            albumId = "album2",
            albumTitle = "Album Two",
            duration = 200000L,
            trackNumber = 1,
            coverUrl = "https://example.com/cover2.jpg",
            streamUrl = "https://example.com/stream3.mp3"
        )
    )

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
    fun `playerState exposes player state flow`() = runTest {
        // Given
        val stateWithQueue = PlayerState(
            queue = testTracks,
            currentIndex = 1,
            currentTrack = testTracks[1],
            isPlaying = true
        )
        playerStateFlow.value = stateWithQueue

        // When
        viewModel = QueueViewModel(player)

        // Then
        viewModel.playerState.test {
            val state = awaitItem()
            assertEquals(testTracks, state.queue)
            assertEquals(1, state.currentIndex)
            assertEquals(testTracks[1], state.currentTrack)
            assertTrue(state.isPlaying)
        }
    }

    @Test
    fun `skipToQueueItem calls player with correct index`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.skipToQueueItem(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.skipToQueueItem(2)

        // Then
        verify { player.skipToQueueItem(2) }
    }

    @Test
    fun `removeFromQueue calls player with correct index`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.removeFromQueue(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.removeFromQueue(1)

        // Then
        verify { player.removeFromQueue(1) }
    }

    @Test
    fun `clearQueue calls player clearQueue`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.clearQueue() } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.clearQueue()

        // Then
        verify { player.clearQueue() }
    }

    @Test
    fun `playerState reflects empty queue`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = emptyList())

        // When
        viewModel = QueueViewModel(player)

        // Then
        viewModel.playerState.test {
            val state = awaitItem()
            assertTrue(state.queue.isEmpty())
        }
    }

    @Test
    fun `playerState reflects queue changes`() = runTest {
        // Given
        viewModel = QueueViewModel(player)

        // When - update queue
        playerStateFlow.value = PlayerState(queue = testTracks)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.playerState.test {
            val state = awaitItem()
            assertEquals(3, state.queue.size)
        }

        // When - remove item (simulated by updating state)
        playerStateFlow.value = PlayerState(queue = testTracks.drop(1))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.playerState.test {
            val state = awaitItem()
            assertEquals(2, state.queue.size)
        }
    }

    @Test
    fun `skipToQueueItem with zero index`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks, currentIndex = 2)
        every { player.skipToQueueItem(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.skipToQueueItem(0)

        // Then
        verify { player.skipToQueueItem(0) }
    }

    @Test
    fun `skipToQueueItem with last index`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks, currentIndex = 0)
        every { player.skipToQueueItem(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.skipToQueueItem(testTracks.lastIndex)

        // Then
        verify { player.skipToQueueItem(testTracks.lastIndex) }
    }

    @Test
    fun `removeFromQueue with first item`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.removeFromQueue(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.removeFromQueue(0)

        // Then
        verify { player.removeFromQueue(0) }
    }

    @Test
    fun `removeFromQueue with last item`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.removeFromQueue(any()) } just Runs

        viewModel = QueueViewModel(player)

        // When
        viewModel.removeFromQueue(testTracks.lastIndex)

        // Then
        verify { player.removeFromQueue(testTracks.lastIndex) }
    }

    @Test
    fun `playerState includes current track info`() = runTest {
        // Given
        val stateWithCurrent = PlayerState(
            queue = testTracks,
            currentIndex = 0,
            currentTrack = testTracks[0],
            isPlaying = true,
            position = 60000L,
            duration = 180000L
        )
        playerStateFlow.value = stateWithCurrent

        // When
        viewModel = QueueViewModel(player)

        // Then
        viewModel.playerState.test {
            val state = awaitItem()
            assertEquals(testTracks[0], state.currentTrack)
            assertEquals(60000L, state.position)
            assertEquals(180000L, state.duration)
        }
    }

    @Test
    fun `multiple operations in sequence`() = runTest {
        // Given
        playerStateFlow.value = PlayerState(queue = testTracks)
        every { player.skipToQueueItem(any()) } just Runs
        every { player.removeFromQueue(any()) } just Runs
        every { player.clearQueue() } just Runs

        viewModel = QueueViewModel(player)

        // When - perform sequence of operations
        viewModel.skipToQueueItem(1)
        viewModel.removeFromQueue(2)
        viewModel.clearQueue()

        // Then
        verify(exactly = 1) { player.skipToQueueItem(1) }
        verify(exactly = 1) { player.removeFromQueue(2) }
        verify(exactly = 1) { player.clearQueue() }
    }
}
