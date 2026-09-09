package com.cloudcontroller.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences for persisting the server connection
 * details (IP, port, auth token) so the user doesn't need to re-enter them
 * every time they open the app.
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

    fun buildWebSocketUrl(): String = "ws://$serverIp:$serverPort"

    companion object {
        private const val PREFS_NAME = "cloudphone_controller_prefs"
        private const val KEY_IP = "server_ip"
        private const val KEY_PORT = "server_port"
        private const val KEY_TOKEN = "auth_token"

        private const val DEFAULT_IP = "192.168.1.100"
        private const val DEFAULT_PORT = 8765
    }
}
