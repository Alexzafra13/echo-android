package com.echo.core.datastore.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverDataStore: DataStore<Preferences> by preferencesDataStore(name = "server_prefs")

@Serializable
data class SavedServer(
    val id: String,
    val name: String,
    val url: String,
    val addedAt: Long,
    val lastConnectedAt: Long? = null
)

@Singleton
class ServerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val dataStore = context.serverDataStore

    private object Keys {
        val SERVERS = stringPreferencesKey("servers")
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
    }

    val servers: Flow<List<SavedServer>> = dataStore.data.map { prefs ->
        val serversJson = prefs[Keys.SERVERS] ?: "[]"
        try {
            json.decodeFromString<List<SavedServer>>(serversJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val activeServerId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SERVER_ID]
    }

    val activeServer: Flow<SavedServer?> = dataStore.data.map { prefs ->
        val activeId = prefs[Keys.ACTIVE_SERVER_ID] ?: return@map null
        val serversJson = prefs[Keys.SERVERS] ?: "[]"
        try {
            val servers = json.decodeFromString<List<SavedServer>>(serversJson)
            servers.find { it.id == activeId }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addServer(server: SavedServer) {
        dataStore.edit { prefs ->
            val serversJson = prefs[Keys.SERVERS] ?: "[]"
            val servers = try {
                json.decodeFromString<List<SavedServer>>(serversJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove if exists (update)
            servers.removeAll { it.id == server.id }
            servers.add(server)

            prefs[Keys.SERVERS] = json.encodeToString(servers)
        }
    }

    suspend fun removeServer(serverId: String) {
        dataStore.edit { prefs ->
            val serversJson = prefs[Keys.SERVERS] ?: "[]"
            val servers = try {
                json.decodeFromString<List<SavedServer>>(serversJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            servers.removeAll { it.id == serverId }
            prefs[Keys.SERVERS] = json.encodeToString(servers)

            // Clear active if it was removed
            if (prefs[Keys.ACTIVE_SERVER_ID] == serverId) {
                prefs.remove(Keys.ACTIVE_SERVER_ID)
            }
        }
    }

    suspend fun setActiveServer(serverId: String?) {
        dataStore.edit { prefs ->
            if (serverId != null) {
                prefs[Keys.ACTIVE_SERVER_ID] = serverId
            } else {
                prefs.remove(Keys.ACTIVE_SERVER_ID)
            }
        }
    }

    suspend fun updateLastConnected(serverId: String) {
        dataStore.edit { prefs ->
            val serversJson = prefs[Keys.SERVERS] ?: "[]"
            val servers = try {
                json.decodeFromString<List<SavedServer>>(serversJson).toMutableList()
            } catch (e: Exception) {
                return@edit
            }

            val index = servers.indexOfFirst { it.id == serverId }
            if (index >= 0) {
                servers[index] = servers[index].copy(lastConnectedAt = System.currentTimeMillis())
                prefs[Keys.SERVERS] = json.encodeToString(servers)
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
