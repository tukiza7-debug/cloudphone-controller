package com.cloudcontroller.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    initialIp: String,
    initialPort: Int,
    initialToken: String,
    onDismiss: () -> Unit,
    onSave: (ip: String, port: Int, token: String) -> Unit
) {
    var ip by remember { mutableStateOf(initialIp) }
    var portText by remember { mutableStateOf(initialPort.toString()) }
    var token by remember { mutableStateOf(initialToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection Settings") },
        text = {
            Column {
                TextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Server IP") },
                    modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit) },
                    label = { Text("Port") },
                    modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Auth Token") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val port = portText.toIntOrNull() ?: initialPort
                onSave(ip.trim(), port, token)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
