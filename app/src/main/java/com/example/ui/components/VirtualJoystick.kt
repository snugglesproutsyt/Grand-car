package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onMove: (forward: Float, strafe: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val radius = (size.toPx() / 2f)
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val delta = offset - center
                        val dist = delta.getDistance()
                        val clampedDist = dist.coerceAtMost(radius * 0.8f)
                        val angle = delta / if (dist == 0f) 1f else dist
                        thumbOffset = angle * clampedDist
                        // forward: negative Y is forward, strafe: positive X is right
                        onMove(-thumbOffset.y / (radius * 0.8f), thumbOffset.x / (radius * 0.8f))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = newOffset.getDistance()
                        val maxR = radius * 0.8f
                        thumbOffset = if (dist > maxR) (newOffset / dist) * maxR else newOffset
                        onMove(-thumbOffset.y / maxR, thumbOffset.x / maxR)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() / 2f * 0.85f

            // Outer ring base
            drawCircle(
                color = Color(0x3300E5FF),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x8800E5FF),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner directional ticks
            val tickLen = 8.dp.toPx()
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(center.x, center.y - outerRadius),
                end = Offset(center.x, center.y - outerRadius + tickLen),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(center.x, center.y + outerRadius),
                end = Offset(center.x, center.y + outerRadius - tickLen),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(center.x - outerRadius, center.y),
                end = Offset(center.x - outerRadius + tickLen, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(center.x + outerRadius, center.y),
                end = Offset(center.x + outerRadius - tickLen, center.y),
                strokeWidth = 2.dp.toPx()
            )

            // Thumb head
            val thumbCenter = center + thumbOffset
            val thumbRadius = 24.dp.toPx()

            drawCircle(
                color = Color(0x5500E5FF),
                radius = thumbRadius * 1.3f,
                center = thumbCenter
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius * 0.45f,
                center = thumbCenter
            )
        }
    }
}
