package com.echo.core.network.sse

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a Server-Sent Event
 */
data class SseEvent(
    val id: String? = null,
    val event: String? = null,
    val data: String,
    val retry: Long? = null
)

/**
 * SSE connection state
 */
sealed class SseConnectionState {
    data object Connecting : SseConnectionState()
    data object Connected : SseConnectionState()
    data class Error(val throwable: Throwable) : SseConnectionState()
    data object Closed : SseConnectionState()
}

/**
 * OkHttp-based SSE client for Android.
 * Provides a Flow-based API for consuming Server-Sent Events.
 */
@Singleton
class SseClient @Inject constructor() {

    private val sseHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE streams
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Connect to an SSE endpoint and receive events as a Flow.
     *
     * @param url The SSE endpoint URL
     * @param headers Optional headers to include in the request
     * @return A Flow of SseEvent objects
     */
    fun connect(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Flow<SseEvent> = callbackFlow {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val request = requestBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE connection opened: $url")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                Log.d(TAG, "SSE event received: type=$type, data=$data")
                if (isActive) {
                    trySend(SseEvent(id = id, event = type, data = data))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE connection closed")
                channel.close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                Log.e(TAG, "SSE connection failed: ${t?.message}", t)
                channel.close(t ?: IOException("SSE connection failed"))
            }
        }

        val eventSource = EventSources.createFactory(sseHttpClient)
            .newEventSource(request, listener)

        awaitClose {
            Log.d(TAG, "Closing SSE connection")
            eventSource.cancel()
        }
    }

    /**
     * Connect to SSE with automatic reconnection and exponential backoff.
     *
     * @param url The SSE endpoint URL
     * @param headers Optional headers
     * @param maxReconnectAttempts Maximum number of reconnection attempts (-1 for infinite)
     * @param initialDelayMs Initial delay before first reconnection attempt
     * @param maxDelayMs Maximum delay between reconnection attempts
     * @param onConnectionState Callback for connection state changes
     */
    fun connectWithReconnect(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxReconnectAttempts: Int = -1,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 30000,
        onConnectionState: (SseConnectionState) -> Unit = {}
    ): Flow<SseEvent> = callbackFlow {
        var reconnectAttempts = 0
        var currentDelay = initialDelayMs
        var shouldReconnect = true
        var currentSource: EventSource? = null

        fun createEventSource(): EventSource {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val request = requestBuilder.build()

            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d(TAG, "SSE connection opened: $url")
                    reconnectAttempts = 0
                    currentDelay = initialDelayMs
                    onConnectionState(SseConnectionState.Connected)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (isActive) {
                        trySend(SseEvent(id = id, event = type, data = data))
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d(TAG, "SSE connection closed")
                    onConnectionState(SseConnectionState.Closed)
                    if (!shouldReconnect) {
                        channel.close()
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Log.e(TAG, "SSE connection failed: ${t?.message}")
                    onConnectionState(SseConnectionState.Error(t ?: IOException("Unknown error")))

                    if (!shouldReconnect) {
                        channel.close(t)
                        return
                    }

                    // Check reconnection limits
                    if (maxReconnectAttempts != -1 && reconnectAttempts >= maxReconnectAttempts) {
                        Log.w(TAG, "Max reconnection attempts reached")
                        channel.close(t)
                        return
                    }

                    // Schedule reconnection with exponential backoff
                    Log.d(TAG, "Scheduling reconnection in ${currentDelay}ms")
                    Thread.sleep(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
                    reconnectAttempts++

                    if (isActive && shouldReconnect) {
                        onConnectionState(SseConnectionState.Connecting)
                        currentSource = createEventSource()
                    }
                }
            }

            return EventSources.createFactory(sseHttpClient)
                .newEventSource(request, listener)
        }

        onConnectionState(SseConnectionState.Connecting)
        currentSource = createEventSource()

        awaitClose {
            Log.d(TAG, "Closing SSE connection with reconnect")
            shouldReconnect = false
            currentSource?.cancel()
        }
    }

    companion object {
        private const val TAG = "SseClient"
    }
}
