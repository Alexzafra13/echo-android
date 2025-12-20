package com.echo.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.echo.core.media.player.EchoPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background service for music playback using Media3.
 * Handles media session and system notifications automatically.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var echoPlayer: EchoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Create session activity intent for notification tap
        val sessionActivityPendingIntent = createSessionActivityPendingIntent()

        // Build the media session
        mediaSession = MediaSession.Builder(this, echoPlayer.exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    /**
     * Creates a PendingIntent that launches the main activity when
     * the notification is tapped.
     */
    private fun createSessionActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent()

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            flags
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            // Stop the service if not actively playing
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            // Don't release the player as it's a singleton managed by Hilt
            // Just release the media session
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Custom MediaSession callback for handling media control requests.
     */
    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): com.google.common.util.concurrent.ListenableFuture<MutableList<MediaItem>> {
            // Allow adding media items from external controllers
            return com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return com.google.common.util.concurrent.Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            )
        }
    }
}
