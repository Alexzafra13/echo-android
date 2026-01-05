package com.echo.core.media.radio

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.media.player.EchoPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var echoPlayer: EchoPlayer
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var metadataService: RadioMetadataService
    private lateinit var metadataFlow: MutableStateFlow<RadioMetadata?>
    private lateinit var mediaItemFactory: MediaItemFactory
    private lateinit var radioPlaybackManager: RadioPlaybackManager

    private var playerListener: Player.Listener? = null
    private lateinit var mockMediaItem: MediaItem

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

        mockMediaItem = mockk(relaxed = true)

        exoPlayer = mockk(relaxed = true) {
            every { addListener(any()) } answers {
                playerListener = firstArg()
            }
        }

        echoPlayer = mockk(relaxed = true) {
            every { exoPlayer } returns this@RadioPlaybackIntegrationTest.exoPlayer
        }

        metadataFlow = MutableStateFlow(null)
        metadataService = mockk(relaxed = true) {
            every { metadata } returns metadataFlow
        }

        mediaItemFactory = mockk(relaxed = true) {
            every { createFromStation(any()) } returns mockMediaItem
            every { createMetadata(any(), any()) } returns mockk(relaxed = true)
        }

        radioPlaybackManager = RadioPlaybackManager(echoPlayer, metadataService, mediaItemFactory)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        playerListener = null
    }

    // ============================================
    // Complete Playback Flow Tests
    // ============================================

    @Test
    fun `complete flow - start playing, receive metadata, stop`() = runTest {
        // Step 1: Start playing station A
        radioPlaybackManager.playStation(stationA)

        // Verify station is set
        var state = radioPlaybackManager.state.value
        assertTrue(state.isRadioMode)
        assertEquals(stationA, state.currentStation)
        assertTrue(state.isBuffering)

        // Step 2: Simulate player becomes ready
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        state = radioPlaybackManager.state.value
        assertTrue(state.isPlaying)
        assertFalse(state.isBuffering)
        assertEquals(RadioSignalStatus.GOOD, state.signalStatus)

        // Step 3: Receive metadata from SSE
        val metadata = RadioMetadata(
            stationUuid = "station-a-uuid",
            title = "Current Song",
            artist = "Test Artist",
            song = null
        )
        radioPlaybackManager.updateMetadata(metadata)

        state = radioPlaybackManager.state.value
        assertNotNull(state.metadata)
        assertEquals("Current Song", state.metadata?.title)
        assertEquals("Test Artist", state.metadata?.artist)

        // Step 4: Stop playback
        radioPlaybackManager.stop()

        state = radioPlaybackManager.state.value
        assertFalse(state.isRadioMode)
        assertFalse(state.isPlaying)
        assertNull(state.currentStation)
        assertNull(state.metadata)

        // Verify metadata service disconnected
        verify { metadataService.disconnect() }
    }

    @Test
    fun `switching stations clears previous state`() = runTest {
        // Start with station A
        radioPlaybackManager.playStation(stationA)

        // Add metadata for station A
        radioPlaybackManager.updateMetadata(
            RadioMetadata("station-a-uuid", "Song A", "Artist A", null)
        )

        // Switch to station B
        radioPlaybackManager.playStation(stationB)

        // Verify state is reset for new station
        val state = radioPlaybackManager.state.value
        assertEquals(stationB, state.currentStation)
        assertNull(state.metadata) // Previous metadata cleared
        assertTrue(state.isBuffering) // Back to buffering state

        // Verify disconnect was called before connecting to new station
        verify(exactly = 2) { metadataService.disconnect() }
        verify { metadataService.connect("station-b-uuid", stationB.streamUrl) }
    }

    @Test
    fun `pause and resume flow`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        // Verify playing
        assertTrue(radioPlaybackManager.state.value.isPlaying)

        // Pause
        radioPlaybackManager.pause()
        playerListener?.onIsPlayingChanged(false)

        // Verify paused but still in radio mode
        var state = radioPlaybackManager.state.value
        assertFalse(state.isPlaying)
        assertTrue(state.isRadioMode)
        assertEquals(stationA, state.currentStation)

        // Resume
        radioPlaybackManager.resume()
        playerListener?.onIsPlayingChanged(true)

        // Verify playing again
        assertTrue(radioPlaybackManager.state.value.isPlaying)
    }

    @Test
    fun `toggle play pause works correctly`() = runTest {
        radioPlaybackManager.playStation(stationA)
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        // Toggle to pause
        radioPlaybackManager.togglePlayPause()
        verify { echoPlayer.pause() }

        // Simulate pause
        playerListener?.onIsPlayingChanged(false)

        // Toggle to play
        radioPlaybackManager.togglePlayPause()
        verify(exactly = 1) { echoPlayer.play() }
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    fun `error during playback updates state correctly`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        // Simulate error
        val error = mockk<androidx.media3.common.PlaybackException>(relaxed = true) {
            every { localizedMessage } returns "Stream unavailable"
        }
        playerListener?.onPlayerError(error)

        // Verify error state
        val state = radioPlaybackManager.state.value
        assertEquals(RadioSignalStatus.ERROR, state.signalStatus)
        assertEquals("Stream unavailable", state.error)
        assertFalse(state.isPlaying)
        assertTrue(state.isRadioMode) // Still in radio mode
    }

    @Test
    fun `stream ended unexpectedly sets error state`() = runTest {
        // Start playing
        radioPlaybackManager.playStation(stationA)
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        // Simulate stream ended
        playerListener?.onPlaybackStateChanged(Player.STATE_ENDED)

        // Verify error state for live stream ended
        val state = radioPlaybackManager.state.value
        assertEquals(RadioSignalStatus.ERROR, state.signalStatus)
        assertNotNull(state.error)
    }

    // ============================================
    // Buffering State Tests
    // ============================================

    @Test
    fun `buffering during playback shows weak signal`() = runTest {
        // Start playing and become ready
        radioPlaybackManager.playStation(stationA)
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)
        playerListener?.onIsPlayingChanged(true)

        assertEquals(RadioSignalStatus.GOOD, radioPlaybackManager.state.value.signalStatus)

        // Start buffering (rebuffering)
        playerListener?.onPlaybackStateChanged(Player.STATE_BUFFERING)

        // Verify buffering state
        var state = radioPlaybackManager.state.value
        assertTrue(state.isBuffering)
        assertEquals(RadioSignalStatus.WEAK, state.signalStatus)

        // Recovery
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)

        // Verify recovered
        state = radioPlaybackManager.state.value
        assertFalse(state.isBuffering)
        assertEquals(RadioSignalStatus.GOOD, state.signalStatus)
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

        assertNotNull(radioPlaybackManager.state.value.metadata)

        // Clear metadata
        radioPlaybackManager.clearMetadata()

        assertNull(radioPlaybackManager.state.value.metadata)
    }

    // ============================================
    // Queue Clearing Tests
    // ============================================

    @Test
    fun `starting radio clears track queue`() = runTest {
        radioPlaybackManager.playStation(stationA)

        verify { echoPlayer.clearQueue() }
    }

    // ============================================
    // Station Properties Tests
    // ============================================

    @Test
    fun `station uses resolved URL when available`() = runTest {
        radioPlaybackManager.playStation(stationA)

        // Station A has urlResolved, should use it
        assertEquals("http://stream.a.com/resolved", stationA.streamUrl)
    }

    @Test
    fun `station uses regular URL when resolved not available`() = runTest {
        radioPlaybackManager.playStation(stationB)

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

        assertTrue(radioPlaybackManager.isRadioMode)

        radioPlaybackManager.stop()

        assertFalse(radioPlaybackManager.isRadioMode)
    }

    @Test
    fun `currentStation property reflects state`() = runTest {
        assertNull(radioPlaybackManager.currentStation)

        radioPlaybackManager.playStation(stationA)

        assertEquals(stationA, radioPlaybackManager.currentStation)

        radioPlaybackManager.playStation(stationB)

        assertEquals(stationB, radioPlaybackManager.currentStation)
    }
}
