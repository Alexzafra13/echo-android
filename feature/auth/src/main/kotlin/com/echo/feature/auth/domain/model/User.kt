package com.echo.feature.auth.domain.model

data class User(
    val id: String,
    val username: String,
    val name: String? = null,
    val isAdmin: Boolean = false,
    val hasAvatar: Boolean = false,
    val mustChangePassword: Boolean = false
)

sealed class LoginResult {
    data class Success(val user: User, val mustChangePassword: Boolean) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
