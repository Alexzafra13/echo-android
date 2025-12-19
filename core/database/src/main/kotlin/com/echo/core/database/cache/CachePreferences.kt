package com.echo.core.database.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cache_preferences"
)

@Singleton
class CachePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MAX_CACHE_SIZE = longPreferencesKey("max_cache_size") // in bytes
        val AUTO_CACHE_ON_WIFI = booleanPreferencesKey("auto_cache_on_wifi")
        val CACHE_COVERS = booleanPreferencesKey("cache_covers")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
    }

    companion object {
        // Default cache sizes in bytes
        const val SIZE_1_GB = 1L * 1024 * 1024 * 1024
        const val SIZE_2_GB = 2L * 1024 * 1024 * 1024
        const val SIZE_5_GB = 5L * 1024 * 1024 * 1024
        const val SIZE_10_GB = 10L * 1024 * 1024 * 1024
        const val SIZE_20_GB = 20L * 1024 * 1024 * 1024
        const val SIZE_UNLIMITED = Long.MAX_VALUE

        val PRESET_SIZES = listOf(
            SIZE_1_GB to "1 GB",
            SIZE_2_GB to "2 GB",
            SIZE_5_GB to "5 GB",
            SIZE_10_GB to "10 GB",
            SIZE_20_GB to "20 GB",
            SIZE_UNLIMITED to "Sin límite"
        )

        const val DEFAULT_MAX_CACHE_SIZE = SIZE_2_GB
    }

    /**
     * Maximum cache size in bytes.
     */
    val maxCacheSize: Flow<Long> = context.cacheDataStore.data.map { prefs ->
        prefs[Keys.MAX_CACHE_SIZE] ?: DEFAULT_MAX_CACHE_SIZE
    }

    /**
     * Whether to automatically cache played tracks when on WiFi.
     */
    val autoCacheOnWifi: Flow<Boolean> = context.cacheDataStore.data.map { prefs ->
        prefs[Keys.AUTO_CACHE_ON_WIFI] ?: true
    }

    /**
     * Whether to cache album covers along with tracks.
     */
    val cacheCovers: Flow<Boolean> = context.cacheDataStore.data.map { prefs ->
        prefs[Keys.CACHE_COVERS] ?: true
    }

    /**
     * Whether caching is enabled at all.
     */
    val cacheEnabled: Flow<Boolean> = context.cacheDataStore.data.map { prefs ->
        prefs[Keys.CACHE_ENABLED] ?: true
    }

    suspend fun setMaxCacheSize(sizeInBytes: Long) {
        context.cacheDataStore.edit { prefs ->
            prefs[Keys.MAX_CACHE_SIZE] = sizeInBytes
        }
    }

    suspend fun setAutoCacheOnWifi(enabled: Boolean) {
        context.cacheDataStore.edit { prefs ->
            prefs[Keys.AUTO_CACHE_ON_WIFI] = enabled
        }
    }

    suspend fun setCacheCovers(enabled: Boolean) {
        context.cacheDataStore.edit { prefs ->
            prefs[Keys.CACHE_COVERS] = enabled
        }
    }

    suspend fun setCacheEnabled(enabled: Boolean) {
        context.cacheDataStore.edit { prefs ->
            prefs[Keys.CACHE_ENABLED] = enabled
        }
    }
}

/**
 * Helper to format bytes to human-readable string.
 */
fun Long.toHumanReadableSize(): String {
    if (this == Long.MAX_VALUE) return "Sin límite"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$this bytes"
    }
}
