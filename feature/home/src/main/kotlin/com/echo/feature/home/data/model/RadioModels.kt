package com.echo.feature.home.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Radio station from our backend (favorites)
 */
@Serializable
data class RadioStation(
    val id: String? = null,
    val userId: String? = null,
    val stationUuid: String? = null,
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
    val lastCheckOk: Boolean? = null,
    val source: String? = null, // "radio-browser" or "custom"
    val isFavorite: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val displayTags: List<String>
        get() = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}

/**
 * Radio Browser API station response
 */
@Serializable
data class RadioBrowserStation(
    val stationuuid: String,
    val name: String,
    val url: String,
    @SerialName("url_resolved") val urlResolved: String = "",
    val homepage: String = "",
    val favicon: String = "",
    val tags: String = "",
    val country: String = "",
    val countrycode: String = "",
    val state: String = "",
    val language: String = "",
    val languagecodes: String = "",
    val votes: Int = 0,
    val lastchangetime: String = "",
    @SerialName("lastchangetime_iso8601") val lastchangetimeIso8601: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: Int = 0,
    val lastcheckok: Int = 0,
    val lastchecktime: String = "",
    @SerialName("lastchecktime_iso8601") val lastchecktimeIso8601: String = "",
    val lastcheckoktime: String = "",
    @SerialName("lastcheckoktime_iso8601") val lastcheckoktimeIso8601: String = "",
    val lastlocalchecktime: String = "",
    @SerialName("lastlocalchecktime_iso8601") val lastlocalchecktimeIso8601: String = "",
    val clicktimestamp: String = "",
    @SerialName("clicktimestamp_iso8601") val clicktimestampIso8601: String = "",
    val clickcount: Int = 0,
    val clicktrend: Int = 0,
    @SerialName("ssl_error") val sslError: Int = 0,
    @SerialName("geo_lat") val geoLat: Double? = null,
    @SerialName("geo_long") val geoLong: Double? = null,
    @SerialName("has_extended_info") val hasExtendedInfo: Boolean = false
) {
    val displayTags: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val isOnline: Boolean
        get() = lastcheckok == 1
}

/**
 * Radio Browser API tag response
 */
@Serializable
data class RadioBrowserTag(
    val name: String,
    val stationcount: Int
)

/**
 * Radio Browser API country response
 */
@Serializable
data class RadioBrowserCountry(
    val name: String,
    @SerialName("iso_3166_1") val isoCode: String,
    val stationcount: Int
)

/**
 * DTO for saving a station from Radio Browser API
 */
@Serializable
data class SaveApiStationDto(
    val stationuuid: String,
    val name: String,
    val url: String,
    @SerialName("url_resolved") val urlResolved: String? = null,
    val homepage: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    val countrycode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val votes: Int? = null,
    val clickcount: Int? = null,
    val lastcheckok: Boolean? = null
)

/**
 * DTO for creating a custom radio station
 */
@Serializable
data class CreateCustomStationDto(
    val name: String,
    val url: String,
    val homepage: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null
)

/**
 * Convert RadioBrowserStation to SaveApiStationDto
 */
fun RadioBrowserStation.toSaveDto(): SaveApiStationDto = SaveApiStationDto(
    stationuuid = stationuuid,
    name = name,
    url = url,
    urlResolved = urlResolved,
    homepage = homepage.ifEmpty { null },
    favicon = favicon.ifEmpty { null },
    country = country.ifEmpty { null },
    countrycode = countrycode.ifEmpty { null },
    state = state.ifEmpty { null },
    language = language.ifEmpty { null },
    tags = tags.ifEmpty { null },
    codec = codec.ifEmpty { null },
    bitrate = if (bitrate > 0) bitrate else null,
    votes = if (votes > 0) votes else null,
    clickcount = if (clickcount > 0) clickcount else null,
    lastcheckok = lastcheckok == 1
)
