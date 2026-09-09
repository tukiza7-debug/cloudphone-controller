package com.cloudcontroller.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cloudcontroller.app.ConnectionState
import com.cloudcontroller.app.ControllerWebSocket
import com.cloudcontroller.app.SettingsManager
import com.cloudcontroller.app.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * Wraps a single pad control so that, while [editMode] is on, the user can
 * drag it (1 finger) to reposition and pinch (2 fingers) to resize. The
 * result is persisted per-key via [SettingsManager].
 */
@Composable
private fun EditableControl(
    settingsManager: SettingsManager,
    key: String,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val initial = remember { settingsManager.getLayout(key) }
    var offsetX by remember { mutableStateOf(initial.first) }
    var offsetY by remember { mutableStateOf(initial.second) }
    var scaleValue by remember { mutableStateOf(initial.third) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .scale(scaleValue)
            .then(
                if (editMode) {
                    Modifier
                        .border(2.dp, Color(0xFF00E5FF))
                        .pointerInput(key) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                offsetX += pan.x
                                offsetY += pan.y
                                scaleValue = (scaleValue * zoom).coerceIn(0.5f, 2.0f)
                                settingsManager.setLayout(key, offsetX, offsetY, scaleValue)
                            }
                        }
                } else Modifier
            )
    ) {
        content()
    }
}

@Composable
fun GamepadScreen(settingsManager: SettingsManager) {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var showSettings by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    var connectionKey by remember {
        mutableStateOf("${settingsManager.serverIp}:${settingsManager.serverPort}:${settingsManager.authToken}:${settingsManager.useTls}")
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

        EditableControl(settingsManager, "dpad", editMode, Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 32.dp)) {
            DPad(onDirection = { name, pressed -> socket.sendButton(name, pressed) })
        }

        EditableControl(settingsManager, "face", editMode, Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 32.dp)) {
            FaceButtons(onButton = { name, pressed -> socket.sendButton(name, pressed) })
        }

        EditableControl(settingsManager, "stick", editMode, Modifier.align(Alignment.TopStart).padding(start = 32.dp, top = 56.dp)) {
            AnalogStick(onMove = { dx, dy -> socket.sendJoystick(dx, dy) })
        }

        EditableControl(settingsManager, "l1", editMode, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp)) {
            ShoulderButton(label = "L1", buttonName = "l1", onButton = { name, pressed -> socket.sendButton(name, pressed) })
        }

        EditableControl(settingsManager, "r1", editMode, Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 8.dp)) {
            ShoulderButton(label = "R1", buttonName = "r1", onButton = { name, pressed -> socket.sendButton(name, pressed) })
        }

        EditableControl(settingsManager, "select", editMode, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, end = 98.dp)) {
            ShoulderButton(label = "SELECT", buttonName = "select", onButton = { name, pressed -> socket.sendButton(name, pressed) })
        }

        EditableControl(settingsManager, "start", editMode, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 98.dp)) {
            ShoulderButton(label = "START", buttonName = "start", onButton = { name, pressed -> socket.sendButton(name, pressed) })
        }

        ConnectionStatusIndicator(
            state = connectionState,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 96.dp)
        )

        if (editMode) {
            IconButton(
                onClick = { settingsManager.resetLayout(); editMode = false; editMode = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 0.dp, end = 64.dp)
            ) {
                Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = "Reset Layout", tint = TextPrimary)
            }
        }

        IconButton(
            onClick = { editMode = !editMode },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 0.dp, end = 40.dp)
        ) {
            Icon(
                imageVector = if (editMode) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = if (editMode) "Selesai susun atur" else "Susun atur pad",
                tint = TextPrimary
            )
        }

        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 0.dp, end = 8.dp)
        ) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = TextPrimary)
        }

        if (editMode) {
            Text(
                text = "Seret = alih pad, cubit 2 jari = saiz. Tekan âœ“ bila siap.",
                color = TextPrimary,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialIp = settingsManager.serverIp,
            initialPort = settingsManager.serverPort,
            initialToken = settingsManager.authToken,
            initialUseTls = settingsManager.useTls,
            onDismiss = { showSettings = false },
            onSave = { ip, port, token, useTls ->
                settingsManager.serverIp = ip
                settingsManager.serverPort = port
                settingsManager.authToken = token
                settingsManager.useTls = useTls
                connectionKey = "$ip:$port:$token:$useTls"
                showSettings = false
            }
        )
    }
}
