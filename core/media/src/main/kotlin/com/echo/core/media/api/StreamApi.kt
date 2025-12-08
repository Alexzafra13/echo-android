package com.echo.core.media.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class StreamTokenResponse(
    val token: String,
    val expiresAt: String
)

interface StreamApi {
    @POST("stream-token/generate")
    suspend fun generateStreamToken(): StreamTokenResponse

    @GET("stream-token")
    suspend fun getStreamToken(): StreamTokenResponse?

    @POST("stream-token/revoke")
    suspend fun revokeStreamToken()
}
