package com.echo.core.datastore.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SessionData(
    val serverId: String,
    val userId: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val streamToken: String? = null,
    val isAdmin: Boolean = false,
    val mustChangePassword: Boolean = false
)

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "echo_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private object Keys {
        const val SESSION = "session"
        const val REMEMBER_CREDENTIALS_PREFIX = "remember_creds_"
    }

    private val _session = MutableStateFlow<SessionData?>(loadSession())
    val session: StateFlow<SessionData?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null

    private fun loadSession(): SessionData? {
        val sessionJson = encryptedPrefs.getString(Keys.SESSION, null) ?: return null
        return try {
            json.decodeFromString<SessionData>(sessionJson)
        } catch (e: Exception) {
            null
        }
    }

    fun saveSession(session: SessionData) {
        encryptedPrefs.edit()
            .putString(Keys.SESSION, json.encodeToString(session))
            .apply()
        _session.value = session
    }

    fun updateTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        val current = _session.value ?: return
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
        saveSession(updated)
    }

    fun updateStreamToken(streamToken: String) {
        val current = _session.value ?: return
        saveSession(current.copy(streamToken = streamToken))
    }

    fun clearSession() {
        encryptedPrefs.edit()
            .remove(Keys.SESSION)
            .apply()
        _session.value = null
    }

    // Saved credentials (optional "remember me")
    @Serializable
    data class SavedCredentials(
        val username: String,
        val password: String
    )

    fun saveCredentials(serverId: String, username: String, password: String) {
        val creds = SavedCredentials(username, password)
        encryptedPrefs.edit()
            .putString("${Keys.REMEMBER_CREDENTIALS_PREFIX}$serverId", json.encodeToString(creds))
            .apply()
    }

    fun getCredentials(serverId: String): SavedCredentials? {
        val credsJson = encryptedPrefs.getString(
            "${Keys.REMEMBER_CREDENTIALS_PREFIX}$serverId",
            null
        ) ?: return null
        return try {
            json.decodeFromString<SavedCredentials>(credsJson)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCredentials(serverId: String) {
        encryptedPrefs.edit()
            .remove("${Keys.REMEMBER_CREDENTIALS_PREFIX}$serverId")
            .apply()
    }
}
