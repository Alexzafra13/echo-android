package com.echo.feature.settings.data.api

import com.echo.feature.settings.data.dto.LogEntryDto
import com.echo.feature.settings.data.dto.LogStatsDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AdminApi {

    @GET("/api/logs")
    suspend fun getLogs(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 50,
        @Query("level") level: String? = null,
        @Query("category") category: String? = null
    ): List<LogEntryDto>

    @GET("/api/logs/stats")
    suspend fun getLogStats(): LogStatsDto

    @GET("/api/logs/categories")
    suspend fun getLogCategories(): List<String>

    @GET("/api/logs/levels")
    suspend fun getLogLevels(): List<String>
}
