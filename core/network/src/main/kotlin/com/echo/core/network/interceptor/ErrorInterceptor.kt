package com.echo.core.network.interceptor

import com.echo.core.common.error.AppError
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that converts HTTP error responses to AppError exceptions.
 * This allows for consistent error handling across the app.
 */
class ErrorInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response: Response

        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            throw AppError.from(e)
        }

        // Don't throw for successful responses or auth errors (handled by AuthInterceptor)
        if (response.isSuccessful || response.code == 401) {
            return response
        }

        // Parse error message from response body if available
        val errorMessage = try {
            response.peekBody(1024).string()
                .takeIf { it.isNotBlank() }
                ?.let { parseErrorMessage(it) }
        } catch (e: Exception) {
            null
        }

        // Convert HTTP error codes to AppError
        val error = when (response.code) {
            403 -> {
                // Check for mustChangePassword in the error message
                if (errorMessage?.contains("password", ignoreCase = true) == true) {
                    AppError.Auth.MustChangePassword()
                } else {
                    AppError.Api.Forbidden(errorMessage)
                }
            }
            404 -> AppError.Api.NotFound(errorMessage)
            429 -> AppError.Api.RateLimited()
            in 500..599 -> AppError.Api.ServerError(errorMessage)
            else -> AppError.Api.Unknown(response.code, errorMessage)
        }

        throw error
    }

    /**
     * Attempts to parse an error message from JSON response.
     * Expected format: { "message": "Error text" } or { "error": "Error text" }
     */
    private fun parseErrorMessage(body: String): String? {
        return try {
            // Simple regex-based parsing to avoid JSON dependency
            val messageRegex = """"(?:message|error)"\s*:\s*"([^"]+)"""".toRegex()
            messageRegex.find(body)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}
