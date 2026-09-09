package com.cloudcontroller.app.ui

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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

@Composable
fun ShoulderButton(
    label: String,
    buttonName: String,
    onButton: (name: String, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(90.dp)
            .height(44.dp)
            .background(if (pressed) ButtonFacePressed else ButtonFace, RoundedCornerShape(10.dp))
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressed = true
                        onButton(buttonName, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        pressed = false
                        onButton(buttonName, false)
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
