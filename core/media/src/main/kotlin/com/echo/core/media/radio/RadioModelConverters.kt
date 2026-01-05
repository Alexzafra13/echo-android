package com.echo.core.media.radio

import com.echo.core.media.model.PlayableRadioStation

/**
 * Data class representing minimal radio station data for conversion.
 * This allows conversion from various source models.
 */
data class RadioStationData(
    val id: String?,
    val stationUuid: String?,
    val name: String,
    val url: String,
    val urlResolved: String?,
    val favicon: String?,
    val country: String?,
    val countryCode: String?,
    val tags: String?,
    val codec: String?,
    val bitrate: Int?,
    val isOnline: Boolean = true
)

/**
 * Convert RadioStationData to PlayableRadioStation
 */
fun RadioStationData.toPlayable(): PlayableRadioStation {
    return PlayableRadioStation(
        id = id ?: stationUuid ?: url.hashCode().toString(),
        stationUuid = stationUuid,
        name = name,
        url = url,
        urlResolved = urlResolved,
        favicon = favicon,
        country = country,
        countryCode = countryCode,
        tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        codec = codec,
        bitrate = bitrate,
        isOnline = isOnline
    )
}

/**
 * Builder pattern for creating PlayableRadioStation from various sources
 */
object PlayableRadioStationBuilder {

    /**
     * Create from Radio Browser API station data
     */
    fun fromRadioBrowser(
        stationuuid: String,
        name: String,
        url: String,
        urlResolved: String = "",
        favicon: String = "",
        country: String = "",
        countrycode: String = "",
        tags: String = "",
        codec: String = "",
        bitrate: Int = 0,
        lastcheckok: Int = 1
    ): PlayableRadioStation {
        return PlayableRadioStation(
            id = stationuuid,
            stationUuid = stationuuid,
            name = name,
            url = url,
            urlResolved = urlResolved.ifEmpty { null },
            favicon = favicon.ifEmpty { null },
            country = country.ifEmpty { null },
            countryCode = countrycode.ifEmpty { null },
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            codec = codec.ifEmpty { null },
            bitrate = if (bitrate > 0) bitrate else null,
            isOnline = lastcheckok == 1
        )
    }

    /**
     * Create from user's favorite station data
     */
    fun fromFavorite(
        id: String?,
        stationUuid: String?,
        name: String,
        url: String,
        urlResolved: String? = null,
        favicon: String? = null,
        country: String? = null,
        countryCode: String? = null,
        tags: String? = null,
        codec: String? = null,
        bitrate: Int? = null,
        lastCheckOk: Boolean? = true
    ): PlayableRadioStation {
        return PlayableRadioStation(
            id = id ?: stationUuid ?: url.hashCode().toString(),
            stationUuid = stationUuid,
            name = name,
            url = url,
            urlResolved = urlResolved,
            favicon = favicon,
            country = country,
            countryCode = countryCode,
            tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            codec = codec,
            bitrate = bitrate,
            isOnline = lastCheckOk ?: true
        )
    }
}
