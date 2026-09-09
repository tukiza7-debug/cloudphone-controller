@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.cloudcontroller.app.ui

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.cloudcontroller.app.ui.theme.ButtonFace
import com.cloudcontroller.app.ui.theme.ButtonFacePressed
import com.cloudcontroller.app.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REPEAT_INITIAL_DELAY_MS = 350L
private const val REPEAT_INTERVAL_MS = 90L

/**
 * A single D-Pad direction button. Uses pointerInteropFilter to separate
 * ACTION_DOWN/ACTION_UP so we can send `pressed=true` / `pressed=false`
 * distinctly, and fires repeated presses while held down.
 */
@Composable
private fun DPadButton(
    label: String,
    modifier: Modifier = Modifier,
    scope: CoroutineScope,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(56.dp)
            .background(
                if (pressed) ButtonFacePressed else ButtonFace,
                RoundedCornerShape(8.dp)
            )
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressed = true
                        onPress()
                        repeatJob?.cancel()
                        repeatJob = scope.launch {
                            delay(REPEAT_INITIAL_DELAY_MS)
                            while (true) {
                                onPress()
                                delay(REPEAT_INTERVAL_MS)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        pressed = false
                        repeatJob?.cancel()
                        repeatJob = null
                        onRelease()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = TextPrimary)
    }
}

@Composable
fun DPad(
    onDirection: (name: String, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScopeCompat()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DPadButton(
            label = "▲",
            scope = scope,
            onPress = { onDirection("dpad_up", true) },
            onRelease = { onDirection("dpad_up", false) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            DPadButton(
                label = "◀",
                scope = scope,
                onPress = { onDirection("dpad_left", true) },
                onRelease = { onDirection("dpad_left", false) }
            )
            DPadButton(
                label = "▶",
                scope = scope,
                onPress = { onDirection("dpad_right", true) },
                onRelease = { onDirection("dpad_right", false) }
            )
        }
        DPadButton(
            label = "▼",
            scope = scope,
            onPress = { onDirection("dpad_down", true) },
            onRelease = { onDirection("dpad_down", false) }
        )
    }
}

@Composable
private fun rememberCoroutineScopeCompat(): CoroutineScope =
    androidx.compose.runtime.rememberCoroutineScope()
