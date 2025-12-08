package com.echo.core.network.interceptor

import com.echo.core.datastore.preferences.SessionPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionPreferences: SessionPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for certain endpoints
        val path = originalRequest.url.encodedPath
        if (shouldSkipAuth(path)) {
            return chain.proceed(originalRequest)
        }

        val session = sessionPreferences.session.value
        val token = session?.accessToken

        val request = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }

    private fun shouldSkipAuth(path: String): Boolean {
        return path.contains("/auth/login") ||
                path.contains("/health") ||
                path.contains("/setup/")
    }
}
