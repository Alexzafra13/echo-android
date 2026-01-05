package com.echo.core.media.radio

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory interface for creating MediaItem instances.
 * This abstraction allows for easier testing without Android dependencies.
 */
interface MediaItemFactory {
    /**
     * Create a MediaItem from a radio station
     */
    fun createFromStation(station: PlayableRadioStation): MediaItem

    /**
     * Create updated MediaMetadata with current metadata
     */
    fun createMetadata(station: PlayableRadioStation, metadata: RadioMetadata?): MediaMetadata
}

/**
 * Default implementation of MediaItemFactory
 */
@Singleton
class DefaultMediaItemFactory @Inject constructor() : MediaItemFactory {

    override fun createFromStation(station: PlayableRadioStation): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.country ?: "Radio")
            .setStation(station.name)
            .setGenre(station.tags.firstOrNull())
            .setArtworkUri(station.favicon?.let { Uri.parse(it) })
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId("radio:${station.stationUuid ?: station.id}")
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .build()
            )
            .build()
    }

    override fun createMetadata(station: PlayableRadioStation, metadata: RadioMetadata?): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setStation(station.name)
            .setArtworkUri(station.favicon?.let { Uri.parse(it) })

        if (metadata?.hasMetadata == true) {
            builder
                .setTitle(metadata.title ?: metadata.song ?: station.name)
                .setArtist(metadata.artist ?: station.country ?: "Radio")
        } else {
            builder
                .setTitle(station.name)
                .setArtist(station.country ?: "Radio")
        }

        return builder.build()
    }
}
