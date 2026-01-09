package com.echo.automotive

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.echo.core.media.player.EchoPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MediaBrowserService for Android Auto integration.
 *
 * This service exposes the Echo music library to Android Auto,
 * allowing users to browse and play music from their car's infotainment system.
 *
 * The service reuses the existing core:media EchoPlayer for playback
 * and feature modules for accessing albums, artists, and playlists data.
 *
 * TODO: Implement full functionality when ready:
 * - Load albums from AlbumsRepository
 * - Load artists from ArtistsRepository
 * - Load playlists from PlaylistsRepository
 * - Handle media playback through EchoPlayer
 */
@AndroidEntryPoint
class EchoAutoService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var player: EchoPlayer

    private lateinit var mediaSession: MediaSessionCompat

    companion object {
        private const val ROOT_ID = "echo_root"
        private const val ALBUMS_ID = "albums"
        private const val ARTISTS_ID = "artists"
        private const val PLAYLISTS_ID = "playlists"
        private const val RECENT_ID = "recent"

        // Empty root for unauthenticated clients
        private const val EMPTY_ROOT_ID = "empty_root"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, "EchoAutoService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
            )

            setCallback(MediaSessionCallback())
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    /**
     * Returns the root node for browsing the media library.
     * Android Auto calls this to determine what content is available.
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // TODO: Validate client package for security
        // For now, allow all clients
        return BrowserRoot(ROOT_ID, null)
    }

    /**
     * Loads children of a given media item.
     * This is called when the user navigates through the media browser.
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        // Detach result to load asynchronously
        result.detach()

        when (parentId) {
            ROOT_ID -> {
                // Return root menu items
                result.sendResult(getRootMenuItems())
            }
            ALBUMS_ID -> {
                // TODO: Load albums from AlbumsRepository
                // albumsRepository.getAlbums().map { it.toMediaItem() }
                result.sendResult(mutableListOf())
            }
            ARTISTS_ID -> {
                // TODO: Load artists from ArtistsRepository
                // artistsRepository.getArtists().map { it.toMediaItem() }
                result.sendResult(mutableListOf())
            }
            PLAYLISTS_ID -> {
                // TODO: Load playlists from PlaylistsRepository
                // playlistsRepository.getPlaylists().map { it.toMediaItem() }
                result.sendResult(mutableListOf())
            }
            RECENT_ID -> {
                // TODO: Load recently played from history
                result.sendResult(mutableListOf())
            }
            else -> {
                // Check if it's an album/artist/playlist ID and load tracks
                // TODO: Parse parentId and load appropriate content
                result.sendResult(mutableListOf())
            }
        }
    }

    /**
     * Returns the root menu items shown in Android Auto.
     */
    private fun getRootMenuItems(): MutableList<MediaBrowserCompat.MediaItem> {
        return mutableListOf(
            createBrowsableMediaItem(
                id = ALBUMS_ID,
                title = getString(R.string.title_albums),
                subtitle = null
            ),
            createBrowsableMediaItem(
                id = ARTISTS_ID,
                title = getString(R.string.title_artists),
                subtitle = null
            ),
            createBrowsableMediaItem(
                id = PLAYLISTS_ID,
                title = getString(R.string.title_playlists),
                subtitle = null
            ),
            createBrowsableMediaItem(
                id = RECENT_ID,
                title = getString(R.string.title_recent),
                subtitle = null
            )
        )
    }

    /**
     * Creates a browsable media item (folder/category).
     */
    private fun createBrowsableMediaItem(
        id: String,
        title: String,
        subtitle: String?
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()

        return MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }

    /**
     * Creates a playable media item (track).
     */
    @Suppress("unused") // Will be used when implementing full functionality
    private fun createPlayableMediaItem(
        id: String,
        title: String,
        artist: String,
        albumArtUri: String? = null
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(artist)
            // .setIconUri(albumArtUri?.toUri()) // TODO: Add album art
            .build()

        return MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
    }

    /**
     * Handles media session callbacks for playback control.
     */
    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            player.play()
        }

        override fun onPause() {
            player.pause()
        }

        override fun onStop() {
            player.stop()
        }

        override fun onSkipToNext() {
            player.seekToNext()
        }

        override fun onSkipToPrevious() {
            player.seekToPrevious()
        }

        override fun onSeekTo(pos: Long) {
            player.seekTo(pos)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            // TODO: Load and play the track with the given mediaId
            // This is called when user selects a track in Android Auto
        }
    }
}
