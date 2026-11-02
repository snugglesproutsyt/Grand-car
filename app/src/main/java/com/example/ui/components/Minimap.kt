package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ArenaMap
import com.example.model.EnemyBot
import com.example.model.Pickup
import com.example.model.Player
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Minimap(
    modifier: Modifier = Modifier,
    player: Player,
    map: ArenaMap,
    enemies: List<EnemyBot>,
    pickups: List<Pickup>,
    size: Dp = 95.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xCC070B14))
            .border(2.dp, Color(0xFF00E5FF), CircleShape)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radarRadius = this.size.width / 2f
            val center = Offset(radarRadius, radarRadius)

            // Radar scale: units per pixel
            val scale = (radarRadius / 10f)

            // Draw radar range rings
            drawCircle(
                color = Color(0x3300E5FF),
                radius = radarRadius * 0.65f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color(0x2200E5FF),
                radius = radarRadius * 0.35f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Crosshair lines
            drawLine(
                color = Color(0x2200E5FF),
                start = Offset(center.x, 0f),
                end = Offset(center.x, this.size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x2200E5FF),
                start = Offset(0f, center.y),
                end = Offset(this.size.width, center.y),
                strokeWidth = 1f
            )

            // Draw nearby walls
            val px = player.x
            val py = player.y

            for (y in maxOf(0, (py - 8).toInt())..minOf(map.height - 1, (py + 8).toInt())) {
                for (x in maxOf(0, (px - 8).toInt())..minOf(map.width - 1, (px + 8).toInt())) {
                    if (map.getTile(x, y) > 0) {
                        val relX = (x + 0.5f - px) * scale
                        val relY = (y + 0.5f - py) * scale
                        val drawX = center.x + relX
                        val drawY = center.y + relY
                        val distSq = relX * relX + relY * relY
                        if (distSq < radarRadius * radarRadius * 0.85f) {
                            drawCircle(
                                color = Color(0x55334155),
                                radius = scale * 0.45f,
                                center = Offset(drawX, drawY)
                            )
                        }
                    }
                }
            }

            // Draw Pickups (cyan/amber dots)
            for (p in pickups) {
                val relX = (p.x - px) * scale
                val relY = (p.y - py) * scale
                val distSq = relX * relX + relY * relY
                if (distSq < radarRadius * radarRadius * 0.85f) {
                    drawCircle(
                        color = p.type.color,
                        radius = 3.5f,
                        center = Offset(center.x + relX, center.y + relY)
                    )
                }
            }

            // Draw Enemies (red glowing dots)
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val relX = (enemy.x - px) * scale
                val relY = (enemy.y - py) * scale
                val distSq = relX * relX + relY * relY
                if (distSq < radarRadius * radarRadius * 0.85f) {
                    drawCircle(
                        color = Color(0xFFFF1744),
                        radius = 4.5f,
                        center = Offset(center.x + relX, center.y + relY)
                    )
                }
            }

            // Draw Player in center with FOV view cone
            val coneDist = radarRadius * 0.55f
            val leftAngle = player.angle - 0.5f
            val rightAngle = player.angle + 0.5f

            val conePath = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + cos(leftAngle) * coneDist, center.y + sin(leftAngle) * coneDist)
                lineTo(center.x + cos(rightAngle) * coneDist, center.y + sin(rightAngle) * coneDist)
                close()
            }
            drawPath(conePath, Color(0x3300E5FF))

            // Player point
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 4.5f,
                center = center
            )
            // Forward pointer line
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x + cos(player.angle) * 14f, center.y + sin(player.angle) * 14f),
                strokeWidth = 2.5f
            )
        }
    }
}
