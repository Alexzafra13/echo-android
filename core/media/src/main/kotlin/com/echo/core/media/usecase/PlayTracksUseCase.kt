package com.echo.core.media.usecase

import com.echo.core.media.model.PlayableTrack
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.radio.RadioPlaybackManager
import com.echo.core.media.repository.StreamRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for playing tracks with proper stream URL handling
 */
@Singleton
class PlayTracksUseCase @Inject constructor(
    private val player: EchoPlayer,
    private val streamRepository: StreamRepository,
    private val radioPlaybackManager: RadioPlaybackManager
) {
    /**
     * Play a single track
     */
    suspend fun playTrack(
        trackId: String,
        title: String,
        artist: String,
        albumId: String,
        albumTitle: String,
        duration: Long,
        trackNumber: Int,
        coverUrl: String?
    ) {
        // Stop radio if playing before switching to music
        stopRadioIfPlaying()

        val streamUrl = streamRepository.getStreamUrl(trackId)
        val playableTrack = PlayableTrack(
            id = trackId,
            title = title,
            artist = artist,
            albumId = albumId,
            albumTitle = albumTitle,
            duration = duration,
            trackNumber = trackNumber,
            coverUrl = coverUrl,
            streamUrl = streamUrl
        )
        player.playTrack(playableTrack)
    }

    /**
     * Play multiple tracks starting from a specific index
     */
    suspend fun playTracks(
        tracks: List<TrackInfo>,
        startIndex: Int = 0
    ) {
        // Stop radio if playing before switching to music
        stopRadioIfPlaying()

        val playableTracks = tracks.map { track ->
            val streamUrl = streamRepository.getStreamUrl(track.id)
            PlayableTrack(
                id = track.id,
                title = track.title,
                artist = track.artist,
                albumId = track.albumId,
                albumTitle = track.albumTitle,
                duration = track.duration,
                trackNumber = track.trackNumber,
                coverUrl = track.coverUrl,
                streamUrl = streamUrl
            )
        }
        player.playTracks(playableTracks, startIndex)
    }

    /**
     * Add a track to the queue
     */
    suspend fun addToQueue(
        trackId: String,
        title: String,
        artist: String,
        albumId: String,
        albumTitle: String,
        duration: Long,
        trackNumber: Int,
        coverUrl: String?
    ) {
        val streamUrl = streamRepository.getStreamUrl(trackId)
        val playableTrack = PlayableTrack(
            id = trackId,
            title = title,
            artist = artist,
            albumId = albumId,
            albumTitle = albumTitle,
            duration = duration,
            trackNumber = trackNumber,
            coverUrl = coverUrl,
            streamUrl = streamUrl
        )
        player.addToQueue(playableTrack)
    }

    /**
     * Play a track next (after current track)
     */
    suspend fun playNext(
        trackId: String,
        title: String,
        artist: String,
        albumId: String,
        albumTitle: String,
        duration: Long,
        trackNumber: Int,
        coverUrl: String?
    ) {
        val streamUrl = streamRepository.getStreamUrl(trackId)
        val playableTrack = PlayableTrack(
            id = trackId,
            title = title,
            artist = artist,
            albumId = albumId,
            albumTitle = albumTitle,
            duration = duration,
            trackNumber = trackNumber,
            coverUrl = coverUrl,
            streamUrl = streamUrl
        )
        player.playNext(playableTrack)
    }

    /**
     * Stop radio playback if currently in radio mode
     */
    private fun stopRadioIfPlaying() {
        if (radioPlaybackManager.state.value.isRadioMode) {
            radioPlaybackManager.stop()
        }
    }
}

/**
 * Simplified track info for the use case
 */
data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String,
    val albumTitle: String,
    val duration: Long,
    val trackNumber: Int,
    val coverUrl: String?
)
