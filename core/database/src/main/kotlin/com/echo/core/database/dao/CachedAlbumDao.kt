package com.echo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.echo.core.database.entity.CachedAlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedAlbumDao {

    @Query("SELECT * FROM cached_albums ORDER BY title")
    fun getAllCachedAlbums(): Flow<List<CachedAlbumEntity>>

    @Query("SELECT * FROM cached_albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: String): CachedAlbumEntity?

    @Query("SELECT * FROM cached_albums WHERE id = :albumId")
    fun observeAlbumById(albumId: String): Flow<CachedAlbumEntity?>

    @Query("SELECT * FROM cached_albums WHERE artistId = :artistId ORDER BY year DESC")
    fun getAlbumsByArtist(artistId: String): Flow<List<CachedAlbumEntity>>

    @Query("SELECT * FROM cached_albums WHERE isFullyCached = 1 ORDER BY title")
    fun getFullyCachedAlbums(): Flow<List<CachedAlbumEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_albums WHERE id = :albumId)")
    suspend fun isAlbumCached(albumId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cached_albums WHERE id = :albumId AND isFullyCached = 1)")
    suspend fun isAlbumFullyCached(albumId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM cached_albums WHERE id = :albumId)")
    fun observeIsAlbumCached(albumId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: CachedAlbumEntity)

    @Update
    suspend fun update(album: CachedAlbumEntity)

    @Query("UPDATE cached_albums SET isFullyCached = :isFullyCached WHERE id = :albumId")
    suspend fun updateFullyCached(albumId: String, isFullyCached: Boolean)

    @Query("DELETE FROM cached_albums WHERE id = :albumId")
    suspend fun deleteById(albumId: String)

    @Query("DELETE FROM cached_albums")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_albums WHERE isFullyCached = 1")
    suspend fun getFullyCachedCount(): Int
}
