package com.echo.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echo.core.database.entity.FavoriteRadioStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRadioStationDao {

    // ============ Queries ============

    @Query("SELECT * FROM favorite_radio_stations ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteRadioStationEntity>>

    @Query("SELECT * FROM favorite_radio_stations ORDER BY addedAt DESC")
    suspend fun getAllFavoritesList(): List<FavoriteRadioStationEntity>

    @Query("SELECT * FROM favorite_radio_stations WHERE id = :id")
    suspend fun getById(id: Long): FavoriteRadioStationEntity?

    @Query("SELECT * FROM favorite_radio_stations WHERE stationUuid = :stationUuid")
    suspend fun getByStationUuid(stationUuid: String): FavoriteRadioStationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_radio_stations WHERE stationUuid = :stationUuid)")
    suspend fun isFavorite(stationUuid: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_radio_stations WHERE stationUuid = :stationUuid)")
    fun observeIsFavorite(stationUuid: String): Flow<Boolean>

    @Query("SELECT stationUuid FROM favorite_radio_stations")
    fun observeAllFavoriteIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorite_radio_stations")
    suspend fun getFavoriteCount(): Int

    // ============ Insert ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: FavoriteRadioStationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<FavoriteRadioStationEntity>)

    // ============ Delete ============

    @Delete
    suspend fun delete(station: FavoriteRadioStationEntity)

    @Query("DELETE FROM favorite_radio_stations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM favorite_radio_stations WHERE stationUuid = :stationUuid")
    suspend fun deleteByStationUuid(stationUuid: String)

    @Query("DELETE FROM favorite_radio_stations")
    suspend fun deleteAll()
}
