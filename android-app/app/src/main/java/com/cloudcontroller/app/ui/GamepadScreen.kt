package com.cloudcontroller.app.ui

import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudcontroller.app.ConnectionState
import com.cloudcontroller.app.ControllerWebSocket
import com.cloudcontroller.app.SettingsManager
import com.cloudcontroller.app.ui.theme.TextPrimary

@Composable
fun GamepadScreen(settingsManager: SettingsManager) {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var showSettings by remember { mutableStateOf(false) }

    // Recreated whenever ip/port/token changes (tracked via this key string).
    var connectionKey by remember {
        mutableStateOf("${settingsManager.serverIp}:${settingsManager.serverPort}:${settingsManager.authToken}")
    }

    val socket = remember(connectionKey) {
        ControllerWebSocket(
            serverUrl = settingsManager.buildWebSocketUrl(),
            authToken = settingsManager.authToken,
            onStateChanged = { connectionState = it }
        )
    }

    DisposableEffect(socket) {
        socket.connect()
        onDispose { socket.disconnect() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // D-Pad, bottom-left
        DPad(
            onDirection = { name, pressed -> socket.sendButton(name, pressed) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 32.dp)
        )

        // Face buttons, bottom-right
        FaceButtons(
            onButton = { name, pressed -> socket.sendButton(name, pressed) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 32.dp)
        )

        // Analog stick, top-left
        AnalogStick(
            onMove = { dx, dy -> socket.sendJoystick(dx, dy) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 32.dp, top = 56.dp)
        )

        // Shoulder buttons, top corners
        ShoulderButton(
            label = "L1",
            buttonName = "l1",
            onButton = { name, pressed -> socket.sendButton(name, pressed) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
        )
        ShoulderButton(
            label = "R1",
            buttonName = "r1",
            onButton = { name, pressed -> socket.sendButton(name, pressed) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 8.dp)
        )

        // Start/Select, bottom-center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            ShoulderButton(
                label = "SELECT",
                buttonName = "select",
                onButton = { name, pressed -> socket.sendButton(name, pressed) },
                modifier = Modifier.padding(end = 8.dp)
            )
            ShoulderButton(
                label = "START",
                buttonName = "start",
                onButton = { name, pressed -> socket.sendButton(name, pressed) },
                modifier = Modifier.padding(start = 98.dp)
            )
        }

        // Connection status dot, top-right corner
        ConnectionStatusIndicator(
            state = connectionState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 64.dp)
        )

        // Settings icon, top-right corner (next to status dot)
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 8.dp)
        ) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = TextPrimary)
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialIp = settingsManager.serverIp,
            initialPort = settingsManager.serverPort,
            initialToken = settingsManager.authToken,
            onDismiss = { showSettings = false },
            onSave = { ip, port, token ->
                settingsManager.serverIp = ip
                settingsManager.serverPort = port
                settingsManager.authToken = token
                connectionKey = "$ip:$port:$token"
                showSettings = false
            }
        )
    }
}
