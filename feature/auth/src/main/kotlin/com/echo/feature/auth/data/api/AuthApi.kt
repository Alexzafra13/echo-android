package com.echo.feature.auth.data.api

import com.echo.feature.auth.data.dto.AuthResponse
import com.echo.feature.auth.data.dto.ChangePasswordRequest
import com.echo.feature.auth.data.dto.LoginRequest
import com.echo.feature.auth.data.dto.RefreshRequest
import com.echo.feature.auth.data.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @GET("users/me")
    suspend fun getCurrentUser(): UserResponse

    @PUT("users/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)
}
