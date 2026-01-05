package com.echo.core.media.radio

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RadioPlaybackManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var echoPlayer: EchoPlayer
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var metadataService: RadioMetadataService
    private lateinit var metadataFlow: MutableStateFlow<RadioMetadata?>
    private lateinit var radioPlaybackManager: RadioPlaybackManager

    // Store captured listener
    private var playerListener: Player.Listener? = null

    private val testStation = PlayableRadioStation(
        id = "1",
        stationUuid = "test-uuid-123",
        name = "Test Radio",
        url = "http://stream.test.com/radio",
        urlResolved = "http://stream.test.com/radio/resolved",
        favicon = "http://test.com/icon.png",
        country = "Spain",
        countryCode = "ES",
        tags = listOf("pop", "rock"),
        codec = "MP3",
        bitrate = 128,
        isOnline = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        exoPlayer = mockk(relaxed = true) {
            every { addListener(any()) } answers {
                playerListener = firstArg()
            }
        }

        echoPlayer = mockk(relaxed = true) {
            every { exoPlayer } returns this@RadioPlaybackManagerTest.exoPlayer
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
        playerListener = null
    }

    @Test
    fun `initial state is not playing`() = runTest {
        val state = radioPlaybackManager.state.value
        assertFalse(state.isRadioMode)
        assertFalse(state.isPlaying)
        assertNull(state.currentStation)
        assertEquals(RadioSignalStatus.UNKNOWN, state.signalStatus)
    }

    @Test
    fun `playStation starts radio mode and sets station`() = runTest {
        // When
        radioPlaybackManager.playStation(testStation)

        // Then
        val state = radioPlaybackManager.state.value
        assertTrue(state.isRadioMode)
        assertEquals(testStation, state.currentStation)
        assertTrue(state.isBuffering)
        assertEquals(RadioSignalStatus.WEAK, state.signalStatus)

        // Verify player interactions
        verify { exoPlayer.setMediaItem(any()) }
        verify { exoPlayer.prepare() }
        verify { exoPlayer.play() }
    }

    @Test
    fun `playStation connects metadata service with station UUID`() = runTest {
        // When
        radioPlaybackManager.playStation(testStation)

        // Then
        verify { metadataService.connect("test-uuid-123", testStation.streamUrl) }
    }

    @Test
    fun `playStation clears previous queue`() = runTest {
        // When
        radioPlaybackManager.playStation(testStation)

        // Then
        verify { echoPlayer.clearQueue() }
    }

    @Test
    fun `stop ends radio mode and clears state`() = runTest {
        // Given - start playing
        radioPlaybackManager.playStation(testStation)

        // When
        radioPlaybackManager.stop()

        // Then
        val state = radioPlaybackManager.state.value
        assertFalse(state.isRadioMode)
        assertFalse(state.isPlaying)
        assertNull(state.currentStation)

        verify { metadataService.disconnect() }
        verify { echoPlayer.stop() }
    }

    @Test
    fun `togglePlayPause pauses when playing`() = runTest {
        // Given - start playing and simulate playing state
        radioPlaybackManager.playStation(testStation)

        // Simulate player is playing
        playerListener?.onIsPlayingChanged(true)

        // When
        radioPlaybackManager.togglePlayPause()

        // Then
        verify { echoPlayer.pause() }
    }

    @Test
    fun `togglePlayPause resumes when paused`() = runTest {
        // Given - start playing but paused
        radioPlaybackManager.playStation(testStation)

        // Simulate player is not playing
        playerListener?.onIsPlayingChanged(false)

        // When
        radioPlaybackManager.togglePlayPause()

        // Then - verify play is called at least twice (once in playStation, once in togglePlayPause)
        verify(atLeast = 2) { echoPlayer.play() }
    }

    @Test
    fun `player buffering updates signal status to WEAK`() = runTest {
        // Given
        radioPlaybackManager.playStation(testStation)

        // When
        playerListener?.onPlaybackStateChanged(Player.STATE_BUFFERING)

        // Then
        val state = radioPlaybackManager.state.value
        assertTrue(state.isBuffering)
        assertEquals(RadioSignalStatus.WEAK, state.signalStatus)
    }

    @Test
    fun `player ready updates signal status to GOOD`() = runTest {
        // Given
        radioPlaybackManager.playStation(testStation)

        // When
        playerListener?.onPlaybackStateChanged(Player.STATE_READY)

        // Then
        val state = radioPlaybackManager.state.value
        assertFalse(state.isBuffering)
        assertEquals(RadioSignalStatus.GOOD, state.signalStatus)
    }

    @Test
    fun `player error updates signal status to ERROR`() = runTest {
        // Given
        radioPlaybackManager.playStation(testStation)

        val error = mockk<androidx.media3.common.PlaybackException>(relaxed = true) {
            every { localizedMessage } returns "Connection failed"
        }

        // When
        playerListener?.onPlayerError(error)

        // Then
        val state = radioPlaybackManager.state.value
        assertEquals(RadioSignalStatus.ERROR, state.signalStatus)
        assertEquals("Connection failed", state.error)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `metadata updates are reflected in state`() = runTest {
        // Given
        radioPlaybackManager.playStation(testStation)

        val metadata = RadioMetadata(
            stationUuid = "test-uuid-123",
            title = "Current Song",
            artist = "Test Artist",
            song = null
        )

        // When
        radioPlaybackManager.updateMetadata(metadata)

        // Then
        val state = radioPlaybackManager.state.value
        assertEquals(metadata, state.metadata)
    }

    @Test
    fun `clearMetadata removes current metadata`() = runTest {
        // Given - set metadata first
        radioPlaybackManager.playStation(testStation)

        radioPlaybackManager.updateMetadata(
            RadioMetadata("test-uuid-123", "Song", "Artist", null)
        )

        // When
        radioPlaybackManager.clearMetadata()

        // Then
        val state = radioPlaybackManager.state.value
        assertNull(state.metadata)
    }

    @Test
    fun `isRadioMode returns correct value`() = runTest {
        // Initially false
        assertFalse(radioPlaybackManager.isRadioMode)

        // After playing
        radioPlaybackManager.playStation(testStation)
        assertTrue(radioPlaybackManager.isRadioMode)

        // After stopping
        radioPlaybackManager.stop()
        assertFalse(radioPlaybackManager.isRadioMode)
    }

    @Test
    fun `currentStation returns current station`() = runTest {
        // Initially null
        assertNull(radioPlaybackManager.currentStation)

        // After playing
        radioPlaybackManager.playStation(testStation)
        assertEquals(testStation, radioPlaybackManager.currentStation)
    }

    @Test
    fun `playing new station disconnects previous metadata stream`() = runTest {
        // Given - play first station
        radioPlaybackManager.playStation(testStation)

        val secondStation = testStation.copy(
            id = "2",
            stationUuid = "second-uuid",
            name = "Second Radio"
        )

        // When - play second station
        radioPlaybackManager.playStation(secondStation)

        // Then - disconnect should be called twice (once for each playStation call)
        verify(exactly = 2) { metadataService.disconnect() }
    }
}
