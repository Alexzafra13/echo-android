package com.echo.feature.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogEntryDto(
    val id: String,
    val level: String,
    val category: String,
    val message: String,
    val timestamp: String,
    val userId: String? = null,
    val entityId: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class LogStatsDto(
    val total: Int,
    val byLevel: Map<String, Int>,
    val byCategory: Map<String, Int>
)
