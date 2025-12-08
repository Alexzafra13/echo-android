package com.echo.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val name: String? = null,
    val isAdmin: Boolean = false,
    val hasAvatar: Boolean = false,
    val mustChangePassword: Boolean = false,
    val createdAt: String? = null
)
