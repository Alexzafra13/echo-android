package com.echo.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.echo.core.database.entity.CachedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedTrackDao {

    // ============ Queries ============

    @Query("SELECT * FROM cached_tracks ORDER BY cachedAt DESC")
    fun getAllCachedTracks(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): CachedTrackEntity?

    @Query("SELECT * FROM cached_tracks WHERE id = :trackId")
    fun observeTrackById(trackId: String): Flow<CachedTrackEntity?>

    @Query("SELECT * FROM cached_tracks WHERE albumId = :albumId ORDER BY discNumber, trackNumber")
    fun getTracksByAlbum(albumId: String): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE artistId = :artistId ORDER BY title")
    fun getTracksByArtist(artistId: String): Flow<List<CachedTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_tracks WHERE id = :trackId)")
    suspend fun isTrackCached(trackId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cached_tracks WHERE id = :trackId)")
    fun observeIsTrackCached(trackId: String): Flow<Boolean>

    // ============ Cache Size ============

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM cached_tracks")
    suspend fun getTotalCacheSize(): Long

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM cached_tracks")
    fun observeTotalCacheSize(): Flow<Long>

    @Query("SELECT COUNT(*) FROM cached_tracks")
    suspend fun getCachedTrackCount(): Int

    // ============ Insert/Update ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: CachedTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<CachedTrackEntity>)

    @Update
    suspend fun update(track: CachedTrackEntity)

    @Query("UPDATE cached_tracks SET lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE id = :trackId")
    suspend fun updateLastPlayed(trackId: String, timestamp: Long = System.currentTimeMillis())

    // ============ Delete ============

    @Delete
    suspend fun delete(track: CachedTrackEntity)

    @Query("DELETE FROM cached_tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM cached_tracks WHERE albumId = :albumId")
    suspend fun deleteByAlbum(albumId: String)

    @Query("DELETE FROM cached_tracks")
    suspend fun deleteAll()

    // ============ LRU Cache Management ============

    /**
     * Get oldest cached tracks for eviction (least recently played first).
     * Returns tracks that haven't been played recently.
     */
    @Query("""
        SELECT * FROM cached_tracks
        ORDER BY
            CASE WHEN lastPlayedAt IS NULL THEN cachedAt ELSE lastPlayedAt END ASC
        LIMIT :limit
    """)
    suspend fun getOldestTracks(limit: Int): List<CachedTrackEntity>

    /**
     * Delete oldest tracks until cache size is under limit.
     * Returns the IDs of deleted tracks so we can delete the files.
     */
    @Query("""
        SELECT id, filePath, coverPath FROM cached_tracks
        ORDER BY
            CASE WHEN lastPlayedAt IS NULL THEN cachedAt ELSE lastPlayedAt END ASC
        LIMIT :count
    """)
    suspend fun getTracksToEvict(count: Int): List<TrackFileInfo>
}

/**
 * Lightweight data class for file deletion during cache eviction.
 */
data class TrackFileInfo(
    val id: String,
    val filePath: String,
    val coverPath: String?
)
