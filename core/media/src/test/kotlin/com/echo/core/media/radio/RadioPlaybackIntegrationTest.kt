package com.echo.core.media.radio

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.cash.turbine.test
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.media.player.EchoPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/**
 * Integration tests that verify the complete radio playback flow.
 * These tests ensure that RadioPlaybackManager, RadioMetadataService,
 * and EchoPlayer work together correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadioPlaybackIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var echoPlayer: EchoPlayer
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var metadataService: RadioMetadataService
    private lateinit var metadataFlow: MutableStateFlow<RadioMetadata?>
    private lateinit var radioPlaybackManager: RadioPlaybackManager

    private val listenerSlot = slot<Player.Listener>()

    private val stationA = PlayableRadioStation(
        id = "1",
        stationUuid = "station-a-uuid",
        name = "Station A",
        url = "http://stream.a.com",
        urlResolved = "http://stream.a.com/resolved",
        favicon = "http://a.com/icon.png",
        country = "Spain",
        countryCode = "ES",
        tags = listOf("pop"),
        codec = "MP3",
        bitrate = 128,
        isOnline = true
    )

    private val stationB = PlayableRadioStation(
        id = "2",
        stationUuid = "station-b-uuid",
        name = "Station B",
        url = "http://stream.b.com",
        urlResolved = null,
        favicon = "http://b.com/icon.png",
        country = "USA",
        countryCode = "US",
        tags = listOf("jazz", "blues"),
        codec = "AAC",
        bitrate = 192,
        isOnline = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        exoPlayer = mockk(relaxed = true) {
            every { addListener(capture(listenerSlot)) } returns Unit
        }

        echoPlayer = mockk(relaxed = true) {
            every { exoPlayer } returns this@RadioPlaybackIntegrationTest.exoPlayer
        }

        metadataFlow = MutableStateFlow(null)
        metadataService = mockk(relaxed = true) {
            every { metadata } returns metadataFlow
        }

        radioPlaybackManager = RadioPlaybackManager(echoPlayer, metadataService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================
    // Complete Playback Flow Tests
    // ============================================

    @Test
    fun `complete flow - start playing, receive metadata, stop`() = runTest {
        // Step 1: Start playing station A
        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify station is set
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertTrue(state.isRadioMode)
            assertEquals(stationA, state.currentStation)
            assertTrue(state.isBuffering)
        }

        // Step 2: Simulate player becomes ready
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertTrue(state.isPlaying)
            assertFalse(state.isBuffering)
            assertEquals(RadioSignalStatus.GOOD, state.signalStatus)
        }

        // Step 3: Receive metadata from SSE
        val metadata = RadioMetadata(
            stationUuid = "station-a-uuid",
            title = "Current Song",
            artist = "Test Artist",
            song = null
        )
        radioPlaybackManager.updateMetadata(metadata)
        testDispatcher.scheduler.advanceUntilIdle()

        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertNotNull(state.metadata)
            assertEquals("Current Song", state.metadata?.title)
            assertEquals("Test Artist", state.metadata?.artist)
        }

        // Step 4: Stop playback
        radioPlaybackManager.stop()
        testDispatcher.scheduler.advanceUntilIdle()

        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertFalse(state.isRadioMode)
            assertFalse(state.isPlaying)
            assertNull(state.currentStation)
            assertNull(state.metadata)
        }

        // Verify metadata service disconnected
        verify { metadataService.disconnect() }
    }

    @Test
    fun `switching stations clears previous state`() = runTest {
        // Start with station A
        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        // Add metadata for station A
        radioPlaybackManager.updateMetadata(
            RadioMetadata("station-a-uuid", "Song A", "Artist A", null)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Switch to station B
        radioPlaybackManager.playStation(stationB)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state is reset for new station
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertEquals(stationB, state.currentStation)
            assertNull(state.metadata) // Previous metadata cleared
            assertTrue(state.isBuffering) // Back to buffering state
        }

        // Verify disconnect was called before connecting to new station
        verify(exactly = 2) { metadataService.disconnect() }
        verify { metadataService.connect("station-b-uuid", stationB.streamUrl) }
    }

    @Test
    fun `pause and resume flow`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify playing
        assertTrue(radioPlaybackManager.state.value.isPlaying)

        // Pause
        radioPlaybackManager.pause()
        listenerSlot.captured.onIsPlayingChanged(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify paused but still in radio mode
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertFalse(state.isPlaying)
            assertTrue(state.isRadioMode)
            assertEquals(stationA, state.currentStation)
        }

        // Resume
        radioPlaybackManager.resume()
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify playing again
        assertTrue(radioPlaybackManager.state.value.isPlaying)
    }

    @Test
    fun `toggle play pause works correctly`() = runTest {
        radioPlaybackManager.playStation(stationA)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle to pause
        radioPlaybackManager.togglePlayPause()
        verify { echoPlayer.pause() }

        // Simulate pause
        listenerSlot.captured.onIsPlayingChanged(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle to play
        radioPlaybackManager.togglePlayPause()
        verify { echoPlayer.play() }
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    fun `error during playback updates state correctly`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate error
        val error = mockk<androidx.media3.common.PlaybackException>(relaxed = true) {
            every { localizedMessage } returns "Stream unavailable"
        }
        listenerSlot.captured.onPlayerError(error)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertEquals(RadioSignalStatus.ERROR, state.signalStatus)
            assertEquals("Stream unavailable", state.error)
            assertFalse(state.isPlaying)
            assertTrue(state.isRadioMode) // Still in radio mode
        }
    }

    @Test
    fun `stream ended unexpectedly sets error state`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate stream ended
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_ENDED)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state for live stream ended
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertEquals(RadioSignalStatus.ERROR, state.signalStatus)
            assertNotNull(state.error)
        }
    }

    // ============================================
    // Buffering State Tests
    // ============================================

    @Test
    fun `buffering during playback shows weak signal`() = runTest {
        // Start playing and become ready
        radioPlaybackManager.playStation(stationA)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        listenerSlot.captured.onIsPlayingChanged(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RadioSignalStatus.GOOD, radioPlaybackManager.state.value.signalStatus)

        // Start buffering (rebuffering)
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_BUFFERING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify buffering state
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertTrue(state.isBuffering)
            assertEquals(RadioSignalStatus.WEAK, state.signalStatus)
        }

        // Recovery
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify recovered
        radioPlaybackManager.state.test {
            val state = awaitItem()
            assertFalse(state.isBuffering)
            assertEquals(RadioSignalStatus.GOOD, state.signalStatus)
        }
    }

    // ============================================
    // Metadata Update Tests
    // ============================================

    @Test
    fun `metadata updates are ignored when not in radio mode`() = runTest {
        // Not playing anything
        assertFalse(radioPlaybackManager.isRadioMode)

        // Try to update metadata
        radioPlaybackManager.updateMetadata(
            RadioMetadata("some-uuid", "Song", "Artist", null)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Metadata should be null (not in radio mode)
        assertNull(radioPlaybackManager.state.value.metadata)
    }

    @Test
    fun `clear metadata removes current metadata`() = runTest {
        // Start playing and add metadata
        radioPlaybackManager.playStation(stationA)
        radioPlaybackManager.updateMetadata(
            RadioMetadata("station-a-uuid", "Song", "Artist", null)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(radioPlaybackManager.state.value.metadata)

        // Clear metadata
        radioPlaybackManager.clearMetadata()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(radioPlaybackManager.state.value.metadata)
    }

    // ============================================
    // Queue Clearing Tests
    // ============================================

    @Test
    fun `starting radio clears track queue`() = runTest {
        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { echoPlayer.clearQueue() }
    }

    // ============================================
    // Station Properties Tests
    // ============================================

    @Test
    fun `station uses resolved URL when available`() = runTest {
        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        // Station A has urlResolved, should use it
        assertEquals("http://stream.a.com/resolved", stationA.streamUrl)
    }

    @Test
    fun `station uses regular URL when resolved not available`() = runTest {
        radioPlaybackManager.playStation(stationB)
        testDispatcher.scheduler.advanceUntilIdle()

        // Station B has no urlResolved, should use regular url
        assertEquals("http://stream.b.com", stationB.streamUrl)
    }

    // ============================================
    // Property Accessors Tests
    // ============================================

    @Test
    fun `isRadioMode property reflects state`() = runTest {
        assertFalse(radioPlaybackManager.isRadioMode)

        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(radioPlaybackManager.isRadioMode)

        radioPlaybackManager.stop()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(radioPlaybackManager.isRadioMode)
    }

    @Test
    fun `currentStation property reflects state`() = runTest {
        assertNull(radioPlaybackManager.currentStation)

        radioPlaybackManager.playStation(stationA)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(stationA, radioPlaybackManager.currentStation)

        radioPlaybackManager.playStation(stationB)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(stationB, radioPlaybackManager.currentStation)
    }
}
