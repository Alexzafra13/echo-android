package com.echo.feature.server.domain.usecase

import com.echo.feature.server.domain.model.ServerInfo
import com.echo.feature.server.domain.model.ServerValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLException

class ValidateServerUseCase @Inject constructor(
    private val json: Json
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend operator fun invoke(serverUrl: String): ServerValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = normalizeUrl(serverUrl)

                // Try health endpoint
                val healthResponse = httpClient.newCall(
                    Request.Builder()
                        .url("$normalizedUrl/api/health")
                        .get()
                        .build()
                ).execute()

                if (!healthResponse.isSuccessful) {
                    return@withContext ServerValidationResult.Error(
                        "El servidor respondió con error: ${healthResponse.code}"
                    )
                }

                val body = healthResponse.body?.string()
                val serverInfo = parseHealthResponse(body, normalizedUrl)

                ServerValidationResult.Success(serverInfo)

            } catch (e: UnknownHostException) {
                ServerValidationResult.Error(
                    "No se puede encontrar el servidor. Verifica la dirección.",
                    e
                )
            } catch (e: ConnectException) {
                ServerValidationResult.Error(
                    "No se puede conectar al servidor. Verifica que esté encendido.",
                    e
                )
            } catch (e: SocketTimeoutException) {
                ServerValidationResult.Error(
                    "El servidor no responde. Tiempo de espera agotado.",
                    e
                )
            } catch (e: SSLException) {
                ServerValidationResult.Error(
                    "Error de certificado SSL. El servidor puede no ser seguro.",
                    e
                )
            } catch (e: Exception) {
                ServerValidationResult.Error(
                    "Error al conectar: ${e.localizedMessage ?: "Error desconocido"}",
                    e
                )
            }
        }
    }

    @Serializable
    private data class HealthResponse(
        val status: String? = null,
        val version: String? = null,
        val name: String? = null
    )

    private fun parseHealthResponse(body: String?, url: String): ServerInfo {
        if (body == null) {
            return ServerInfo(
                name = extractServerName(url),
                healthy = true
            )
        }

        return try {
            val health = json.decodeFromString<HealthResponse>(body)
            ServerInfo(
                name = health.name ?: extractServerName(url),
                version = health.version,
                healthy = health.status == "ok" || health.status == "healthy" || true
            )
        } catch (e: Exception) {
            ServerInfo(
                name = extractServerName(url),
                healthy = true
            )
        }
    }

    private fun extractServerName(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")
            .substringBefore("/")
            .let { host ->
                if (host.contains(".")) {
                    host.substringBefore(".")
                        .replaceFirstChar { it.uppercase() }
                } else {
                    "Echo Server"
                }
            }
    }

    companion object {
        fun normalizeUrl(url: String): String {
            var normalized = url.trim()

            // Add scheme if missing
            if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                normalized = "https://$normalized"
            }

            // Remove trailing slash
            return normalized.trimEnd('/')
        }
    }
}
