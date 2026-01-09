package com.echo.feature.settings.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.settings.data.api.AdminApi
import com.echo.feature.settings.data.dto.LogEntryDto
import com.echo.feature.settings.presentation.AdminLog
import com.echo.feature.settings.presentation.LogLevel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private suspend fun getApi(): AdminApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(AdminApi::class.java)
    }

    private fun LogEntryDto.toDomain(): AdminLog {
        return AdminLog(
            id = id,
            level = when (level.uppercase()) {
                "ERROR", "CRITICAL" -> LogLevel.ERROR
                "WARNING" -> LogLevel.WARNING
                else -> LogLevel.INFO
            },
            category = category,
            message = message,
            timestamp = formatTimestamp(timestamp)
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            // ISO 8601 format to readable format
            timestamp.replace("T", " ").substringBefore(".")
        } catch (e: Exception) {
            timestamp
        }
    }

    suspend fun getLogs(
        skip: Int = 0,
        take: Int = 50,
        level: String? = null,
        category: String? = null
    ): Result<List<AdminLog>> = runCatching {
        getApi().getLogs(skip, take, level, category).map { it.toDomain() }
    }

    suspend fun getLogStats(): Result<LogStats> = runCatching {
        val stats = getApi().getLogStats()
        LogStats(
            total = stats.total,
            errorCount = stats.byLevel["ERROR"] ?: 0,
            warningCount = stats.byLevel["WARNING"] ?: 0,
            infoCount = stats.byLevel["INFO"] ?: 0
        )
    }

    suspend fun getLogCategories(): Result<List<String>> = runCatching {
        getApi().getLogCategories()
    }

    suspend fun getLogLevels(): Result<List<String>> = runCatching {
        getApi().getLogLevels()
    }
}

data class LogStats(
    val total: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int
)
