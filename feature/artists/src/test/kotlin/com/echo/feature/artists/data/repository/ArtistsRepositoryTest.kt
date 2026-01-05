package com.echo.feature.artists.data.repository

import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.artists.data.api.ArtistsApi
import com.echo.feature.artists.data.dto.ArtistAlbumDto
import com.echo.feature.artists.data.dto.ArtistAlbumsPageDto
import com.echo.feature.artists.data.dto.ArtistDto
import com.echo.feature.artists.data.dto.ArtistStatsDto
import com.echo.feature.artists.data.dto.ArtistTopTrackDto
import com.echo.feature.artists.data.dto.ArtistTopTracksResponseDto
import com.echo.feature.artists.data.dto.ArtistsPageDto
import com.echo.feature.artists.data.dto.ArtistsSearchPageDto
import com.echo.feature.artists.data.dto.RelatedArtistDto
import com.echo.feature.artists.data.dto.RelatedArtistsResponseDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ArtistsRepositoryTest {

    private lateinit var apiClientFactory: ApiClientFactory
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var artistsApi: ArtistsApi
    private lateinit var repository: ArtistsRepository

    private val testServer = SavedServer(
        id = "server-1",
        name = "Test Server",
        url = "https://test.example.com",
        addedAt = System.currentTimeMillis()
    )

    private val testArtistDto = ArtistDto(
        id = "artist-1",
        name = "Test Artist",
        albumCount = 5,
        songCount = 50,
        biography = "A great artist"
    )

    private val testAlbumDto = ArtistAlbumDto(
        id = "album-1",
        name = "Test Album",
        year = 2023,
        trackCount = 10,
        duration = 3600,
        genres = listOf("Rock", "Pop")
    )

    private val testStatsDto = ArtistStatsDto(
        artistId = "artist-1",
        totalPlays = 10000,
        uniqueListeners = 5000
    )

    private val testTopTrackDto = ArtistTopTrackDto(
        trackId = "track-1",
        title = "Hit Song",
        albumId = "album-1",
        albumName = "Test Album",
        duration = 200,
        playCount = 5000,
        uniqueListeners = 2000
    )

    private val testRelatedArtistDto = RelatedArtistDto(
        id = "related-1",
        name = "Similar Artist",
        albumCount = 3,
        songCount = 30,
        matchScore = 85
    )

    @Before
    fun setup() {
        artistsApi = mockk(relaxed = true)

        val retrofit: Retrofit = mockk {
            every { create(ArtistsApi::class.java) } returns artistsApi
        }

        apiClientFactory = mockk {
            every { getClient(any()) } returns retrofit
        }

        serverPreferences = mockk {
            every { activeServer } returns flowOf(testServer)
        }

        repository = ArtistsRepository(apiClientFactory, serverPreferences)
    }

    @Test
    fun `getArtists returns list of artists`() = runTest {
        // Given
        val artistsPage = ArtistsPageDto(
            data = listOf(testArtistDto),
            total = 1,
            skip = 0,
            take = 50
        )
        coEvery { artistsApi.getArtists(any(), any()) } returns artistsPage

        // When
        val result = repository.getArtists()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Artist", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `getArtist returns artist by id`() = runTest {
        // Given
        coEvery { artistsApi.getArtist("artist-1") } returns testArtistDto

        // When
        val result = repository.getArtist("artist-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("artist-1", result.getOrNull()?.id)
        assertEquals("Test Artist", result.getOrNull()?.name)
    }

    @Test
    fun `getArtistWithAlbums returns complete artist data`() = runTest {
        // Given
        val albumsPage = ArtistAlbumsPageDto(data = listOf(testAlbumDto), total = 1)
        val topTracksResponse = ArtistTopTracksResponseDto(
            data = listOf(testTopTrackDto),
            artistId = "artist-1"
        )
        val relatedResponse = RelatedArtistsResponseDto(
            data = listOf(testRelatedArtistDto),
            artistId = "artist-1"
        )

        coEvery { artistsApi.getArtist("artist-1") } returns testArtistDto
        coEvery { artistsApi.getArtistAlbums("artist-1") } returns albumsPage
        coEvery { artistsApi.getArtistStats("artist-1") } returns testStatsDto
        coEvery { artistsApi.getArtistTopTracks("artist-1") } returns topTracksResponse
        coEvery { artistsApi.getRelatedArtists("artist-1") } returns relatedResponse

        // When
        val result = repository.getArtistWithAlbums("artist-1")

        // Then
        assertTrue(result.isSuccess)
        val artistWithAlbums = result.getOrNull()
        assertEquals("Test Artist", artistWithAlbums?.artist?.name)
        assertEquals(10000, artistWithAlbums?.artist?.playCount)
        assertEquals(5000, artistWithAlbums?.artist?.listenerCount)
        assertEquals(1, artistWithAlbums?.albums?.size)
        assertEquals(1, artistWithAlbums?.topTracks?.size)
        assertEquals(1, artistWithAlbums?.relatedArtists?.size)
    }

    @Test
    fun `getArtistWithAlbums succeeds even when stats fail`() = runTest {
        // Given
        val albumsPage = ArtistAlbumsPageDto(data = listOf(testAlbumDto), total = 1)
        coEvery { artistsApi.getArtist("artist-1") } returns testArtistDto
        coEvery { artistsApi.getArtistAlbums("artist-1") } returns albumsPage
        coEvery { artistsApi.getArtistStats("artist-1") } throws Exception("Stats not available")
        coEvery { artistsApi.getArtistTopTracks("artist-1") } throws Exception("Top tracks not available")
        coEvery { artistsApi.getRelatedArtists("artist-1") } throws Exception("Related not available")

        // When
        val result = repository.getArtistWithAlbums("artist-1")

        // Then
        assertTrue(result.isSuccess)
        val artistWithAlbums = result.getOrNull()
        assertEquals("Test Artist", artistWithAlbums?.artist?.name)
        assertEquals(0, artistWithAlbums?.artist?.playCount) // Default when stats fail
        assertTrue(artistWithAlbums?.topTracks?.isEmpty() == true)
        assertTrue(artistWithAlbums?.relatedArtists?.isEmpty() == true)
    }

    @Test
    fun `getArtistAlbums returns albums for artist`() = runTest {
        // Given
        val albumsPage = ArtistAlbumsPageDto(data = listOf(testAlbumDto), total = 1)
        coEvery { artistsApi.getArtist("artist-1") } returns testArtistDto
        coEvery { artistsApi.getArtistAlbums("artist-1") } returns albumsPage

        // When
        val result = repository.getArtistAlbums("artist-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Album", result.getOrNull()?.first()?.title)
    }

    @Test
    fun `searchArtists returns matching artists`() = runTest {
        // Given
        val searchPage = ArtistsSearchPageDto(data = listOf(testArtistDto), total = 1)
        coEvery { artistsApi.searchArtists("test", 20) } returns searchPage

        // When
        val result = repository.searchArtists("test")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Artist", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `repository throws when no active server`() = runTest {
        // Given
        every { serverPreferences.activeServer } returns flowOf(null)
        repository = ArtistsRepository(apiClientFactory, serverPreferences)

        // When
        val result = repository.getArtists()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `getArtists handles error`() = runTest {
        // Given
        coEvery { artistsApi.getArtists(any(), any()) } throws Exception("Network error")

        // When
        val result = repository.getArtists()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `artist image URLs are built correctly`() = runTest {
        // Given
        coEvery { artistsApi.getArtist("artist-1") } returns testArtistDto

        // When
        val result = repository.getArtist("artist-1")

        // Then
        assertTrue(result.isSuccess)
        val artist = result.getOrNull()
        assertEquals("https://test.example.com/api/images/artists/artist-1/profile", artist?.imageUrl)
        assertEquals("https://test.example.com/api/images/artists/artist-1/background", artist?.backgroundUrl)
    }
}
