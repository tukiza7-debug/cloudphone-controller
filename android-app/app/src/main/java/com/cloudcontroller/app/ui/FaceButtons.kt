package com.cloudcontroller.app.ui

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

@Composable
private fun FaceButton(
    label: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(58.dp)
            .background(if (pressed) ButtonFacePressed else ButtonFace, CircleShape)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressed = true
                        onPress()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        pressed = false
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

/**
 * A/B/X/Y arranged in the classic diamond layout: Y top, X left, B right, A bottom.
 */
@Composable
fun FaceButtons(
    onButton: (name: String, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val spread = 42.dp

    Box(modifier = modifier.size(150.dp)) {
        FaceButton(
            label = "Y",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -spread),
            onPress = { onButton("btn_y", true) },
            onRelease = { onButton("btn_y", false) }
        )
        FaceButton(
            label = "X",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -spread),
            onPress = { onButton("btn_x", true) },
            onRelease = { onButton("btn_x", false) }
        )
        FaceButton(
            label = "B",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = spread),
            onPress = { onButton("btn_b", true) },
            onRelease = { onButton("btn_b", false) }
        )
        FaceButton(
            label = "A",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = spread),
            onPress = { onButton("btn_a", true) },
            onRelease = { onButton("btn_a", false) }
        )
    }
}
