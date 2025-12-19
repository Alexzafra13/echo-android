package com.echo.core.common.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Unified error types for the application.
 * These errors can be easily mapped to user-friendly messages.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Network-related errors
     */
    sealed class Network(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class NoConnection(cause: Throwable? = null) : Network("No hay conexión a internet", cause)
        class Timeout(cause: Throwable? = null) : Network("La conexión ha tardado demasiado", cause)
        class ServerUnreachable(cause: Throwable? = null) : Network("No se puede conectar al servidor", cause)
        class SSLError(cause: Throwable? = null) : Network("Error de certificado SSL", cause)
        class Unknown(message: String? = null, cause: Throwable? = null) : Network(message ?: "Error de red desconocido", cause)
    }

    /**
     * API/HTTP errors
     */
    sealed class Api(message: String, val code: Int, cause: Throwable? = null) : AppError(message, cause) {
        class Unauthorized(cause: Throwable? = null) : Api("Sesión expirada. Por favor, inicia sesión de nuevo", 401, cause)
        class Forbidden(message: String? = null, cause: Throwable? = null) : Api(message ?: "No tienes permisos para realizar esta acción", 403, cause)
        class NotFound(message: String? = null, cause: Throwable? = null) : Api(message ?: "Recurso no encontrado", 404, cause)
        class ServerError(message: String? = null, cause: Throwable? = null) : Api(message ?: "Error del servidor", 500, cause)
        class RateLimited(cause: Throwable? = null) : Api("Demasiadas solicitudes. Espera un momento", 429, cause)
        class Unknown(code: Int, message: String? = null, cause: Throwable? = null) : Api(message ?: "Error de API: $code", code, cause)
    }

    /**
     * Authentication errors
     */
    sealed class Auth(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class InvalidCredentials(cause: Throwable? = null) : Auth("Usuario o contraseña incorrectos", cause)
        class SessionExpired(cause: Throwable? = null) : Auth("Tu sesión ha expirado", cause)
        class MustChangePassword(cause: Throwable? = null) : Auth("Debes cambiar tu contraseña", cause)
        class NoSession(cause: Throwable? = null) : Auth("No hay sesión activa", cause)
    }

    /**
     * Media/Player errors
     */
    sealed class Media(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class PlaybackFailed(message: String? = null, cause: Throwable? = null) : Media(message ?: "Error al reproducir", cause)
        class StreamUnavailable(cause: Throwable? = null) : Media("Stream no disponible", cause)
        class InvalidFormat(cause: Throwable? = null) : Media("Formato de audio no soportado", cause)
    }

    /**
     * Data/Storage errors
     */
    sealed class Data(message: String, cause: Throwable? = null) : AppError(message, cause) {
        class ParseError(cause: Throwable? = null) : Data("Error al procesar los datos", cause)
        class CacheError(cause: Throwable? = null) : Data("Error de caché", cause)
        class DatabaseError(cause: Throwable? = null) : Data("Error de base de datos", cause)
    }

    /**
     * Generic unknown error
     */
    class Unknown(message: String? = null, cause: Throwable? = null) : AppError(message ?: "Ha ocurrido un error inesperado", cause)

    companion object {
        /**
         * Maps a generic Throwable to an appropriate AppError
         */
        fun from(throwable: Throwable): AppError {
            return when (throwable) {
                is AppError -> throwable
                is UnknownHostException -> Network.NoConnection(throwable)
                is SocketTimeoutException -> Network.Timeout(throwable)
                is SSLException -> Network.SSLError(throwable)
                is IOException -> Network.Unknown(throwable.message, throwable)
                else -> Unknown(throwable.message, throwable)
            }
        }

        /**
         * Creates an API error from HTTP status code
         */
        fun fromHttpCode(code: Int, message: String? = null, cause: Throwable? = null): Api {
            return when (code) {
                401 -> Api.Unauthorized(cause)
                403 -> Api.Forbidden(message, cause)
                404 -> Api.NotFound(message, cause)
                429 -> Api.RateLimited(cause)
                in 500..599 -> Api.ServerError(message, cause)
                else -> Api.Unknown(code, message, cause)
            }
        }
    }
}

/**
 * Extension to get a user-friendly error message
 */
val Throwable.userMessage: String
    get() = when (this) {
        is AppError -> this.message
        else -> AppError.from(this).message
    }
