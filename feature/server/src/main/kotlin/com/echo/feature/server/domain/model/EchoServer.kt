package com.echo.feature.server.domain.model

import java.util.UUID

data class EchoServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null
) {
    val displayUrl: String
        get() = url.removePrefix("https://").removePrefix("http://")
}

data class ServerInfo(
    val name: String,
    val version: String? = null,
    val healthy: Boolean = true
)

sealed class ServerValidationResult {
    data class Success(val serverInfo: ServerInfo) : ServerValidationResult()
    data class Error(val message: String, val exception: Throwable? = null) : ServerValidationResult()
}
