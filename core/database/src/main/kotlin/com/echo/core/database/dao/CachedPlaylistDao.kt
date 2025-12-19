package com.echo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.echo.core.database.entity.CachedPlaylistEntity
import com.echo.core.database.entity.CachedPlaylistTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedPlaylistDao {

    @Query("SELECT * FROM cached_playlists ORDER BY name")
    fun getAllCachedPlaylists(): Flow<List<CachedPlaylistEntity>>

    @Query("SELECT * FROM cached_playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): CachedPlaylistEntity?

    @Query("SELECT * FROM cached_playlists WHERE isFullyCached = 1 ORDER BY name")
    fun getFullyCachedPlaylists(): Flow<List<CachedPlaylistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_playlists WHERE id = :playlistId)")
    suspend fun isPlaylistCached(playlistId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cached_playlists WHERE id = :playlistId)")
    fun observeIsPlaylistCached(playlistId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: CachedPlaylistEntity)

    @Update
    suspend fun update(playlist: CachedPlaylistEntity)

    @Query("UPDATE cached_playlists SET isFullyCached = :isFullyCached WHERE id = :playlistId")
    suspend fun updateFullyCached(playlistId: String, isFullyCached: Boolean)

    @Query("DELETE FROM cached_playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: String)

    @Query("DELETE FROM cached_playlists")
    suspend fun deleteAll()

    // ============ Playlist Tracks ============

    @Query("""
        SELECT trackId FROM cached_playlist_tracks
        WHERE playlistId = :playlistId
        ORDER BY `order`
    """)
    fun getPlaylistTrackIds(playlistId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: CachedPlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(tracks: List<CachedPlaylistTrackEntity>)

    @Query("DELETE FROM cached_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: String)

    @Query("DELETE FROM cached_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrack(playlistId: String, trackId: String)

    @Transaction
    suspend fun replacePlaylistTracks(playlistId: String, tracks: List<CachedPlaylistTrackEntity>) {
        deletePlaylistTracks(playlistId)
        insertPlaylistTracks(tracks)
    }
}
