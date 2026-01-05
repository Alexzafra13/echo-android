package com.echo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a favorite radio station stored locally.
 */
@Entity(
    tableName = "favorite_radio_stations",
    indices = [
        Index(value = ["stationUuid"], unique = true),
        Index(value = ["addedAt"])
    ]
)
data class FavoriteRadioStationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stationUuid: String,
    val name: String,
    val url: String,
    val urlResolved: String? = null,
    val homepage: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val votes: Int? = null,
    val clickCount: Int? = null,
    val lastCheckOk: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
) {
    val displayTags: List<String>
        get() = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}
