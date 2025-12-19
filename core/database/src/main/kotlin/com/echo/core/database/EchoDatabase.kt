package com.echo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.echo.core.database.dao.CachedAlbumDao
import com.echo.core.database.dao.CachedPlaylistDao
import com.echo.core.database.dao.CachedTrackDao
import com.echo.core.database.entity.CachedAlbumEntity
import com.echo.core.database.entity.CachedArtistEntity
import com.echo.core.database.entity.CachedPlaylistEntity
import com.echo.core.database.entity.CachedPlaylistTrackEntity
import com.echo.core.database.entity.CachedTrackEntity

@Database(
    entities = [
        CachedTrackEntity::class,
        CachedAlbumEntity::class,
        CachedArtistEntity::class,
        CachedPlaylistEntity::class,
        CachedPlaylistTrackEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun cachedTrackDao(): CachedTrackDao
    abstract fun cachedAlbumDao(): CachedAlbumDao
    abstract fun cachedPlaylistDao(): CachedPlaylistDao

    companion object {
        const val DATABASE_NAME = "echo_database"
    }
}
