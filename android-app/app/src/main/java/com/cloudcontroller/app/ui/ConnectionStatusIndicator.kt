package com.cloudcontroller.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudcontroller.app.ConnectionState
import com.cloudcontroller.app.ui.theme.AccentAmber
import com.cloudcontroller.app.ui.theme.AccentGreen
import com.cloudcontroller.app.ui.theme.AccentRed
import com.cloudcontroller.app.ui.theme.TextSecondary

@Composable
fun ConnectionStatusIndicator(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ConnectionState.CONNECTED -> AccentGreen
        ConnectionState.CONNECTING -> AccentAmber
        ConnectionState.DISCONNECTED -> TextSecondary
        ConnectionState.ERROR -> AccentRed
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(14.dp)
            .background(color, CircleShape)
    )
}
