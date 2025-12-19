package com.echo.core.database.cache

import android.content.Context
import com.echo.core.database.dao.CachedAlbumDao
import com.echo.core.database.dao.CachedTrackDao
import com.echo.core.database.entity.CachedAlbumEntity
import com.echo.core.database.entity.CachedTrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current state of a download operation.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val trackId: String,
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val trackId: String) : DownloadState()
    data class Failed(val trackId: String, val error: String) : DownloadState()
}

/**
 * Manages offline caching of tracks, albums, and playlists.
 * Handles downloading, storage, and cache eviction.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cachedTrackDao: CachedTrackDao,
    private val cachedAlbumDao: CachedAlbumDao,
    private val cachePreferences: CachePreferences
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "audio_cache").apply { mkdirs() }
    }

    private val coverCacheDir: File by lazy {
        File(context.filesDir, "cover_cache").apply { mkdirs() }
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    /**
     * Current cache usage vs limit.
     */
    val cacheStatus: Flow<CacheStatus> = combine(
        cachedTrackDao.observeTotalCacheSize(),
        cachePreferences.maxCacheSize
    ) { used, max ->
        CacheStatus(
            usedBytes = used,
            maxBytes = max,
            usedPercentage = if (max == Long.MAX_VALUE) 0f else (used.toFloat() / max.toFloat())
        )
    }

    /**
     * Check if a track is cached and get its local path.
     */
    suspend fun getCachedTrackPath(trackId: String): String? {
        return cachedTrackDao.getTrackById(trackId)?.filePath?.let { path ->
            if (File(path).exists()) path else null
        }
    }

    /**
     * Check if track is currently being downloaded.
     */
    fun isDownloading(trackId: String): Boolean {
        return trackId in _activeDownloads.value
    }

    /**
     * Observe if a specific track is cached.
     */
    fun observeIsTrackCached(trackId: String): Flow<Boolean> {
        return cachedTrackDao.observeIsTrackCached(trackId)
    }

    /**
     * Cache a track from an input stream.
     * Call this when downloading from the API.
     */
    suspend fun cacheTrack(
        trackId: String,
        title: String,
        artistId: String?,
        artistName: String?,
        albumId: String?,
        albumName: String?,
        trackNumber: Int?,
        discNumber: Int?,
        duration: Int?,
        format: String?,
        inputStream: InputStream,
        totalBytes: Long,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if we need to evict old tracks first
            ensureCacheSpace(totalBytes)

            _activeDownloads.value = _activeDownloads.value + trackId
            _downloadState.value = DownloadState.Downloading(trackId, 0f, 0, totalBytes)

            // Create file for track
            val extension = format ?: "mp3"
            val trackFile = File(cacheDir, "$trackId.$extension")

            // Write to file with progress
            var bytesWritten = 0L
            trackFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesWritten += read
                    val progress = bytesWritten.toFloat() / totalBytes.toFloat()
                    _downloadState.value = DownloadState.Downloading(trackId, progress, bytesWritten, totalBytes)
                    onProgress(progress)
                }
            }

            // Save to database
            val entity = CachedTrackEntity(
                id = trackId,
                title = title,
                artistId = artistId,
                artistName = artistName,
                albumId = albumId,
                albumName = albumName,
                trackNumber = trackNumber,
                discNumber = discNumber,
                duration = duration,
                year = null,
                filePath = trackFile.absolutePath,
                fileSize = bytesWritten,
                bitRate = null,
                format = format,
                coverPath = null
            )
            cachedTrackDao.insert(entity)

            _downloadState.value = DownloadState.Completed(trackId)
            _activeDownloads.value = _activeDownloads.value - trackId

            Result.success(trackFile.absolutePath)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Failed(trackId, e.message ?: "Download failed")
            _activeDownloads.value = _activeDownloads.value - trackId
            Result.failure(e)
        }
    }

    /**
     * Remove a specific track from cache.
     */
    suspend fun removeTrack(trackId: String) = withContext(Dispatchers.IO) {
        val track = cachedTrackDao.getTrackById(trackId) ?: return@withContext

        // Delete files
        track.filePath.let { File(it).delete() }
        track.coverPath?.let { File(it).delete() }

        // Delete from database
        cachedTrackDao.deleteById(trackId)
    }

    /**
     * Remove an entire album from cache.
     */
    suspend fun removeAlbum(albumId: String) = withContext(Dispatchers.IO) {
        // Get all tracks for this album
        val tracks = cachedTrackDao.getTracksByAlbum(albumId).first()

        // Delete files and database entries
        tracks.forEach { track ->
            File(track.filePath).delete()
            track.coverPath?.let { File(it).delete() }
        }
        cachedTrackDao.deleteByAlbum(albumId)
        cachedAlbumDao.deleteById(albumId)
    }

    /**
     * Clear all cached data.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        // Delete all files
        cacheDir.listFiles()?.forEach { it.delete() }
        coverCacheDir.listFiles()?.forEach { it.delete() }

        // Clear database
        cachedTrackDao.deleteAll()
        cachedAlbumDao.deleteAll()
    }

    /**
     * Ensure there's enough space in the cache for new content.
     * Evicts old tracks if necessary.
     */
    private suspend fun ensureCacheSpace(neededBytes: Long) {
        val maxSize = cachePreferences.maxCacheSize.first()
        if (maxSize == Long.MAX_VALUE) return // No limit

        val currentSize = cachedTrackDao.getTotalCacheSize()
        val availableSpace = maxSize - currentSize

        if (neededBytes <= availableSpace) return

        // Need to evict tracks
        val spaceToFree = neededBytes - availableSpace
        evictTracks(spaceToFree)
    }

    /**
     * Evict tracks until we free up the specified amount of space.
     */
    private suspend fun evictTracks(bytesToFree: Long) = withContext(Dispatchers.IO) {
        var freedBytes = 0L

        // Get oldest tracks (LRU)
        val tracksToEvict = cachedTrackDao.getTracksToEvict(50) // Get up to 50 at a time

        for (track in tracksToEvict) {
            if (freedBytes >= bytesToFree) break

            // Delete files
            val trackFile = File(track.filePath)
            val fileSize = trackFile.length()
            trackFile.delete()
            track.coverPath?.let { File(it).delete() }

            // Delete from database
            cachedTrackDao.deleteById(track.id)

            freedBytes += fileSize
        }
    }

    /**
     * Mark a track as played (updates LRU timestamp).
     */
    suspend fun markTrackPlayed(trackId: String) {
        cachedTrackDao.updateLastPlayed(trackId)
    }

    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats {
        return CacheStats(
            totalTracks = cachedTrackDao.getCachedTrackCount(),
            totalAlbums = cachedAlbumDao.getFullyCachedCount(),
            totalSizeBytes = cachedTrackDao.getTotalCacheSize(),
            maxSizeBytes = cachePreferences.maxCacheSize.first()
        )
    }
}

data class CacheStatus(
    val usedBytes: Long,
    val maxBytes: Long,
    val usedPercentage: Float
) {
    val formattedUsed: String get() = usedBytes.toHumanReadableSize()
    val formattedMax: String get() = maxBytes.toHumanReadableSize()
    val isNearLimit: Boolean get() = usedPercentage > 0.9f
}

data class CacheStats(
    val totalTracks: Int,
    val totalAlbums: Int,
    val totalSizeBytes: Long,
    val maxSizeBytes: Long
) {
    val formattedSize: String get() = totalSizeBytes.toHumanReadableSize()
    val formattedMax: String get() = maxSizeBytes.toHumanReadableSize()
}
