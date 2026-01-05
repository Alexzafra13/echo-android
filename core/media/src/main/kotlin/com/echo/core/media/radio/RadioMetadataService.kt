package com.echo.core.media.radio

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.media.model.RadioMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching real-time radio metadata via Server-Sent Events (SSE).
 * Connects to the backend's /radio/metadata/stream endpoint.
 */
@Singleton
class RadioMetadataService @Inject constructor(
    private val serverPreferences: ServerPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _metadata = MutableStateFlow<RadioMetadata?>(null)
    val metadata: StateFlow<RadioMetadata?> = _metadata.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var eventSource: EventSource? = null
    private var reconnectJob: Job? = null
    private var currentStationUuid: String? = null
    private var currentStreamUrl: String? = null

    // Backoff configuration
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelayMs = 2000L
    private val maxReconnectDelayMs = 30000L

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for SSE
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Connect to the metadata stream for a radio station
     */
    fun connect(stationUuid: String, streamUrl: String) {
        // Don't reconnect if already connected to the same station
        if (currentStationUuid == stationUuid && _isConnected.value) {
            return
        }

        // Disconnect from any existing stream
        disconnect()

        currentStationUuid = stationUuid
        currentStreamUrl = streamUrl
        reconnectAttempts = 0

        scope.launch {
            connectInternal()
        }
    }

    /**
     * Disconnect from the metadata stream
     */
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        eventSource?.cancel()
        eventSource = null
        currentStationUuid = null
        currentStreamUrl = null
        _metadata.value = null
        _isConnected.value = false
        reconnectAttempts = 0
    }

    private suspend fun connectInternal() {
        val server = serverPreferences.activeServer.first() ?: return
        val stationUuid = currentStationUuid ?: return
        val streamUrl = currentStreamUrl ?: return

        val url = buildString {
            append(server.url.trimEnd('/'))
            append("/radio/metadata/stream")
            append("?stationUuid=")
            append(java.net.URLEncoder.encode(stationUuid, "UTF-8"))
            append("&streamUrl=")
            append(java.net.URLEncoder.encode(streamUrl, "UTF-8"))
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                _isConnected.value = true
                reconnectAttempts = 0
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                when (type) {
                    "metadata" -> parseMetadata(data)
                    "connected" -> {
                        // Initial connection event
                        _isConnected.value = true
                    }
                    "heartbeat" -> {
                        // Keep-alive, nothing to do
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                _isConnected.value = false
                scheduleReconnect()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                _isConnected.value = false
                scheduleReconnect()
            }
        }

        try {
            val factory = EventSources.createFactory(httpClient)
            eventSource = factory.newEventSource(request, listener)
        } catch (e: Exception) {
            _isConnected.value = false
            scheduleReconnect()
        }
    }

    private fun parseMetadata(data: String) {
        try {
            val json = JSONObject(data)
            val stationUuid = json.optString("stationUuid", currentStationUuid ?: "")

            // Only update if this is for our current station
            if (stationUuid == currentStationUuid) {
                val metadata = RadioMetadata(
                    stationUuid = stationUuid,
                    title = json.optString("title").takeIf { it.isNotBlank() },
                    artist = json.optString("artist").takeIf { it.isNotBlank() },
                    song = json.optString("song").takeIf { it.isNotBlank() },
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
                _metadata.value = metadata
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
    }

    private fun scheduleReconnect() {
        if (currentStationUuid == null || reconnectAttempts >= maxReconnectAttempts) {
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = calculateBackoffDelay()
            delay(delayMs)

            if (isActive && currentStationUuid != null) {
                reconnectAttempts++
                connectInternal()
            }
        }
    }

    private fun calculateBackoffDelay(): Long {
        val exponentialDelay = baseReconnectDelayMs * (1 shl reconnectAttempts)
        return minOf(exponentialDelay, maxReconnectDelayMs)
    }

    /**
     * Clean up resources
     */
    fun release() {
        disconnect()
    }
}
