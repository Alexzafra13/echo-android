package com.echo.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a track that has been cached for offline playback.
 */
@Entity(
    tableName = "cached_tracks",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["cachedAt"])
    ]
)
data class CachedTrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artistId: String?,
    val artistName: String?,
    val albumId: String?,
    val albumName: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Int?, // in seconds
    val year: Int?,
    val filePath: String, // Local path to cached audio file
    val fileSize: Long, // Size in bytes
    val bitRate: Int?, // Audio bitrate
    val format: String?, // Audio format (mp3, flac, etc)
    val coverPath: String?, // Local path to cached cover image
    val cachedAt: Long = System.currentTimeMillis(), // When it was cached
    val lastPlayedAt: Long? = null, // For LRU cache eviction
    val playCount: Int = 0 // Local play count
)

/**
 * Represents an album with cached metadata (not necessarily all tracks cached).
 */
@Entity(
    tableName = "cached_albums",
    indices = [Index(value = ["artistId"])]
)
data class CachedAlbumEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artistId: String?,
    val artistName: String?,
    val year: Int?,
    val trackCount: Int?,
    val duration: Int?, // Total duration in seconds
    val genres: String?, // Comma-separated genres
    val coverPath: String?, // Local path to cached cover
    val cachedAt: Long = System.currentTimeMillis(),
    val isFullyCached: Boolean = false // All tracks downloaded
)

/**
 * Represents a cached artist.
 */
@Entity(
    tableName = "cached_artists"
)
data class CachedArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val biography: String?,
    val imagePath: String?, // Local path to profile image
    val backgroundPath: String?, // Local path to background image
    val albumCount: Int?,
    val trackCount: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a cached playlist.
 */
@Entity(
    tableName = "cached_playlists"
)
data class CachedPlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val trackCount: Int,
    val duration: Int?,
    val isPublic: Boolean,
    val coverPath: String?,
    val cachedAt: Long = System.currentTimeMillis(),
    val isFullyCached: Boolean = false
)

/**
 * Join table for playlist tracks.
 */
@Entity(
    tableName = "cached_playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = CachedPlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"])]
)
data class CachedPlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val order: Int,
    val addedAt: Long = System.currentTimeMillis()
)
