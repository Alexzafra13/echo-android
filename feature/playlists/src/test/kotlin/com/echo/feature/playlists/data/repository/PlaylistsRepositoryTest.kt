package com.echo.feature.playlists.data.repository

import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.playlists.data.api.PlaylistsApi
import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistTrackDto
import com.echo.feature.playlists.data.dto.PlaylistTracksResponseDto
import com.echo.feature.playlists.data.dto.PlaylistsPageDto
import com.echo.feature.playlists.data.dto.TrackDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class PlaylistsRepositoryTest {

    private lateinit var apiClientFactory: ApiClientFactory
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var playlistsApi: PlaylistsApi
    private lateinit var repository: PlaylistsRepository

    private val testServer = SavedServer(
        id = "server-1",
        name = "Test Server",
        url = "https://test.example.com",
        addedAt = System.currentTimeMillis()
    )

    private val testPlaylistDto = PlaylistDto(
        id = "playlist-1",
        name = "Test Playlist",
        description = "A test playlist",
        trackCount = 5,
        duration = 1200,
        isPublic = true
    )

    private val testTrackDto = TrackDto(
        id = "track-1",
        title = "Test Song",
        artistName = "Test Artist",
        albumId = "album-1",
        albumTitle = "Test Album",
        duration = 180,
        trackNumber = 1
    )

    private val testPlaylistTrackDto = PlaylistTrackDto(
        id = "pt-1",
        playlistId = "playlist-1",
        trackId = "track-1",
        order = 0,
        track = testTrackDto
    )

    @Before
    fun setup() {
        playlistsApi = mockk(relaxed = true)

        val retrofit: Retrofit = mockk {
            every { create(PlaylistsApi::class.java) } returns playlistsApi
        }

        apiClientFactory = mockk {
            every { getClient(any()) } returns retrofit
        }

        serverPreferences = mockk {
            every { activeServer } returns flowOf(testServer)
        }

        repository = PlaylistsRepository(apiClientFactory, serverPreferences)
    }

    @Test
    fun `getPlaylists returns list of playlists`() = runTest {
        // Given
        val playlistsPage = PlaylistsPageDto(
            items = listOf(testPlaylistDto),
            total = 1,
            skip = 0,
            take = 50
        )
        coEvery { playlistsApi.getPlaylists(any(), any()) } returns playlistsPage

        // When
        val result = repository.getPlaylists()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Playlist", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `getPlaylists handles error`() = runTest {
        // Given
        coEvery { playlistsApi.getPlaylists(any(), any()) } throws Exception("Network error")

        // When
        val result = repository.getPlaylists()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getPlaylist returns playlist by id`() = runTest {
        // Given
        coEvery { playlistsApi.getPlaylist("playlist-1") } returns testPlaylistDto

        // When
        val result = repository.getPlaylist("playlist-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("playlist-1", result.getOrNull()?.id)
        assertEquals("Test Playlist", result.getOrNull()?.name)
    }

    @Test
    fun `getPlaylistWithTracks returns playlist with tracks`() = runTest {
        // Given
        val tracksResponse = PlaylistTracksResponseDto(
            playlistId = "playlist-1",
            playlistName = "Test Playlist",
            tracks = listOf(testPlaylistTrackDto),
            total = 1
        )
        coEvery { playlistsApi.getPlaylist("playlist-1") } returns testPlaylistDto
        coEvery { playlistsApi.getPlaylistTracks("playlist-1") } returns tracksResponse

        // When
        val result = repository.getPlaylistWithTracks("playlist-1")

        // Then
        assertTrue(result.isSuccess)
        val playlistWithTracks = result.getOrNull()
        assertEquals("playlist-1", playlistWithTracks?.playlist?.id)
        assertEquals(1, playlistWithTracks?.tracks?.size)
        assertEquals("Test Song", playlistWithTracks?.tracks?.first()?.title)
    }

    @Test
    fun `getPlaylistTracks returns sorted tracks`() = runTest {
        // Given
        val track1 = testPlaylistTrackDto.copy(id = "pt-1", order = 1)
        val track2 = testPlaylistTrackDto.copy(id = "pt-2", order = 0)
        val tracksResponse = PlaylistTracksResponseDto(
            playlistId = "playlist-1",
            playlistName = "Test Playlist",
            tracks = listOf(track1, track2),
            total = 2
        )
        coEvery { playlistsApi.getPlaylistTracks("playlist-1") } returns tracksResponse

        // When
        val result = repository.getPlaylistTracks("playlist-1")

        // Then
        assertTrue(result.isSuccess)
        val tracks = result.getOrNull()
        assertEquals("pt-2", tracks?.first()?.id) // order 0 should be first
        assertEquals("pt-1", tracks?.last()?.id)  // order 1 should be last
    }

    @Test
    fun `createPlaylist creates and returns new playlist`() = runTest {
        // Given
        val newPlaylistDto = testPlaylistDto.copy(id = "new-playlist")
        coEvery { playlistsApi.createPlaylist(any()) } returns newPlaylistDto

        // When
        val result = repository.createPlaylist("New Playlist", "Description", true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("new-playlist", result.getOrNull()?.id)
        coVerify {
            playlistsApi.createPlaylist(
                CreatePlaylistDto("New Playlist", "Description", true)
            )
        }
    }

    @Test
    fun `updatePlaylist updates and returns playlist`() = runTest {
        // Given
        val updatedDto = testPlaylistDto.copy(name = "Updated Name")
        coEvery { playlistsApi.updatePlaylist(any(), any()) } returns updatedDto

        // When
        val result = repository.updatePlaylist("playlist-1", name = "Updated Name")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated Name", result.getOrNull()?.name)
    }

    @Test
    fun `deletePlaylist deletes successfully`() = runTest {
        // Given
        coEvery { playlistsApi.deletePlaylist(any()) } returns Unit

        // When
        val result = repository.deletePlaylist("playlist-1")

        // Then
        assertTrue(result.isSuccess)
        coVerify { playlistsApi.deletePlaylist("playlist-1") }
    }

    @Test
    fun `addTrackToPlaylist adds track successfully`() = runTest {
        // Given
        coEvery { playlistsApi.addTrackToPlaylist(any(), any()) } returns Unit

        // When
        val result = repository.addTrackToPlaylist("playlist-1", "track-1")

        // Then
        assertTrue(result.isSuccess)
        coVerify { playlistsApi.addTrackToPlaylist("playlist-1", match { it.trackId == "track-1" }) }
    }

    @Test
    fun `removeTrackFromPlaylist removes track successfully`() = runTest {
        // Given
        coEvery { playlistsApi.removeTrackFromPlaylist(any(), any()) } returns Unit

        // When
        val result = repository.removeTrackFromPlaylist("playlist-1", "track-1")

        // Then
        assertTrue(result.isSuccess)
        coVerify { playlistsApi.removeTrackFromPlaylist("playlist-1", "track-1") }
    }

    @Test
    fun `repository throws when no active server`() = runTest {
        // Given
        every { serverPreferences.activeServer } returns flowOf(null)
        repository = PlaylistsRepository(apiClientFactory, serverPreferences)

        // When
        val result = repository.getPlaylists()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
