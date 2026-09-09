package com.cloudcontroller.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences for persisting the server connection
 * details (IP, port, auth token) and per-control custom layout (position +
 * scale) so the user doesn't need to re-enter/re-arrange them every time.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String
        get() = prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var authToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    // When true, connect over wss:// (TLS) â€” required for tunnels such as
    // Cloudflare Tunnel / cloudflared, which terminate TLS for you and only
    // ever expose a hostname on port 443, never a raw LAN IP.
    var useTls: Boolean
        get() = prefs.getBoolean(KEY_TLS, DEFAULT_TLS)
        set(value) = prefs.edit().putBoolean(KEY_TLS, value).apply()

    fun buildWebSocketUrl(): String {
        val scheme = if (useTls) "wss" else "ws"
        val host = serverIp.trim()
        return "$scheme://$host:$serverPort"
    }

    /** Custom on-screen layout (offset in px + scale) for a given control key. */
    fun getLayout(key: String): Triple<Float, Float, Float> {
        val x = prefs.getFloat("layout_${key}_x", 0f)
        val y = prefs.getFloat("layout_${key}_y", 0f)
        val scale = prefs.getFloat("layout_${key}_scale", 1f)
        return Triple(x, y, scale)
    }

    fun setLayout(key: String, x: Float, y: Float, scale: Float) {
        prefs.edit()
            .putFloat("layout_${key}_x", x)
            .putFloat("layout_${key}_y", y)
            .putFloat("layout_${key}_scale", scale.coerceIn(0.5f, 2.0f))
            .apply()
    }

    fun resetLayout() {
        val editor = prefs.edit()
        for (k in LAYOUT_KEYS) {
            editor.remove("layout_${k}_x")
            editor.remove("layout_${k}_y")
            editor.remove("layout_${k}_scale")
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "cloudphone_controller_prefs"
        private const val KEY_IP = "server_ip"
        private const val KEY_PORT = "server_port"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_TLS = "use_tls"

        private const val DEFAULT_IP = "192.168.1.100"
        private const val DEFAULT_PORT = 8765
        private const val DEFAULT_TLS = false

        val LAYOUT_KEYS = listOf("dpad", "face", "stick", "l1", "r1", "start", "select")
    }
}
