package com.cloudcontroller.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cloudcontroller.app.ui.theme.ButtonFace
import com.cloudcontroller.app.ui.theme.ButtonFacePressed
import kotlin.math.hypot
import kotlin.math.min

/**
 * Draggable analog stick. Reports continuous dx/dy in range [-1.0, 1.0] relative
 * to the boundary radius while dragging, and resets to (0,0) on release.
 */
@Composable
fun AnalogStick(
    onMove: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier,
    boundaryDiameter: androidx.compose.ui.unit.Dp = 140.dp,
    knobDiameter: androidx.compose.ui.unit.Dp = 60.dp
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    val boundaryRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (boundaryDiameter / 2).toPx()
    }

    Box(
        modifier = modifier
            .size(boundaryDiameter)
            .background(ButtonFace, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        dragging = false
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = knobOffset + dragAmount
                        val distance = hypot(proposed.x, proposed.y)
                        val clamped = if (distance > boundaryRadiusPx) {
                            val scale = boundaryRadiusPx / distance
                            Offset(proposed.x * scale, proposed.y * scale)
                        } else {
                            proposed
                        }
                        knobOffset = clamped
                        val dx = (clamped.x / boundaryRadiusPx).coerceIn(-1f, 1f)
                        val dy = (clamped.y / boundaryRadiusPx).coerceIn(-1f, 1f)
                        onMove(dx, dy)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .background(
                    if (dragging) ButtonFacePressed else ButtonFacePressed.copy(alpha = 0.85f),
                    CircleShape
                )
                .let { base ->
                    // Manual offset via graphicsLayer-free approach: use offset modifier
                    base
                }
                .androidxOffsetPx(knobOffset)
        )
    }
}

/**
 * Small helper to apply a pixel Offset via Modifier.offset, since offset{} needs
 * an IntOffset in px and our drag math works in float px.
 */
private fun Modifier.androidxOffsetPx(offsetPx: Offset): Modifier = this.then(
    Modifier.offset {
        androidx.compose.ui.unit.IntOffset(offsetPx.x.toInt(), offsetPx.y.toInt())
    }
)
