package com.echo.core.network.api

import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.network.interceptor.AuthInterceptor
import com.echo.core.network.interceptor.ErrorInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApiClientFactory @Inject constructor(
    private val sessionPreferences: SessionPreferences,
    private val json: Json,
    @Named("isDebug") private val isDebug: Boolean
) {
    private val clients = ConcurrentHashMap<String, Retrofit>()

    fun getClient(baseUrl: String): Retrofit {
        val normalizedUrl = normalizeUrl(baseUrl)
        return clients.getOrPut(normalizedUrl) {
            createRetrofitClient(normalizedUrl)
        }
    }

    fun clearClient(baseUrl: String) {
        val normalizedUrl = normalizeUrl(baseUrl)
        clients.remove(normalizedUrl)
    }

    fun clearAllClients() {
        clients.clear()
    }

    private fun createRetrofitClient(baseUrl: String): Retrofit {
        val authInterceptor = AuthInterceptor(sessionPreferences)
        val errorInterceptor = ErrorInterceptor()

        val okHttpClientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Only add logging interceptor in debug builds
        if (isDebug) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                // Redact sensitive headers even in debug
                redactHeader("Authorization")
                redactHeader("Cookie")
            }
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl("$baseUrl/api/")
            .client(okHttpClientBuilder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()

        // Add scheme if missing
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }

        // Remove trailing slash
        return normalized.trimEnd('/')
    }

    companion object {
        fun normalizeServerUrl(url: String): String {
            var normalized = url.trim()
            if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                normalized = "https://$normalized"
            }
            return normalized.trimEnd('/')
        }
    }
}
