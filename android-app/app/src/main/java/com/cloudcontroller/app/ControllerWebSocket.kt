package com.cloudcontroller.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * Manages the WebSocket connection to the CloudPhone Controller Termux server.
 * Sends the auth token as the first message after the socket opens, and
 * auto-reconnects with exponential backoff (max 5 attempts) if the
 * connection drops unexpectedly.
 */
class ControllerWebSocket(
    private val serverUrl: String,
    private val authToken: String,
    private val onStateChanged: (ConnectionState) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket: no read timeout
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var reconnectAttempts = 0
    private var manuallyClosed = false

    private val maxReconnectAttempts = 5

    fun connect() {
        manuallyClosed = false
        openSocket()
    }

    private fun openSocket() {
        setState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket opened, sending auth token")
                reconnectAttempts = 0
                val authPayload = JSONObject().apply { put("token", authToken) }
                webSocket.send(authPayload.toString())
                setState(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message from server: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code / $reason")
                setState(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                setState(ConnectionState.ERROR)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (manuallyClosed) return
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "Max reconnect attempts reached ($maxReconnectAttempts), giving up.")
            return
        }
        val delaySeconds = min(2.0.pow(reconnectAttempts.toDouble()).toLong(), 30L)
        reconnectAttempts++
        Log.i(TAG, "Reconnect attempt $reconnectAttempts in ${delaySeconds}s")
        mainHandler.postDelayed({ openSocket() }, delaySeconds * 1000)
    }

    fun sendButton(name: String, pressed: Boolean) {
        val payload = JSONObject().apply {
            put("type", "button")
            put("name", name)
            put("pressed", pressed)
        }
        send(payload)
    }

    fun sendJoystick(dx: Float, dy: Float) {
        val payload = JSONObject().apply {
            put("type", "joystick")
            put("dx", dx)
            put("dy", dy)
        }
        send(payload)
    }

    fun sendTap(x: Int, y: Int) {
        val payload = JSONObject().apply {
            put("type", "tap")
            put("x", x)
            put("y", y)
        }
        send(payload)
    }

    fun sendSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        val payload = JSONObject().apply {
            put("type", "swipe")
            put("x1", x1)
            put("y1", y1)
            put("x2", x2)
            put("y2", y2)
            put("duration", durationMs)
        }
        send(payload)
    }

    fun sendText(value: String) {
        val payload = JSONObject().apply {
            put("type", "text")
            put("value", value)
        }
        send(payload)
    }

    private fun send(payload: JSONObject) {
        val socket = webSocket
        if (socket == null) {
            Log.w(TAG, "Tried to send while socket is null: $payload")
            return
        }
        socket.send(payload.toString())
    }

    fun disconnect() {
        manuallyClosed = true
        mainHandler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Client closing")
        webSocket = null
        setState(ConnectionState.DISCONNECTED)
    }

    private fun setState(state: ConnectionState) {
        mainHandler.post { onStateChanged(state) }
    }

    companion object {
        private const val TAG = "ControllerWebSocket"
    }
}
