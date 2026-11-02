package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.Player
import com.example.model.WeaponState
import com.example.model.WeaponType
import kotlin.math.sin

@Composable
fun WeaponViewModelRenderer(
    modifier: Modifier = Modifier,
    weapon: WeaponState,
    player: Player
) {
    Box(modifier = modifier.fillMaxSize()) {
        // If Sniper and ADS active, render full-screen sniper scope overlay
        if (player.isADS && weapon.type == WeaponType.SNIPER) {
            SniperScopeOverlay()
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenW = size.width
                val screenH = size.height

                // Base gun position (bottom right normally, or center if ADS)
                val adsProgress = if (player.isADS) 1.0f else 0.0f
                val normalX = screenW * 0.72f
                val adsX = screenW * 0.5f
                val baseX = normalX + (adsX - normalX) * adsProgress

                // Recoil kick & reload bob
                val recoilY = weapon.recoilAnim * 28f
                val recoilX = (sin(weapon.recoilAnim * Math.PI.toFloat()) * 8f)
                val reloadY = if (weapon.isReloading) {
                    sin(weapon.reloadProgress * Math.PI.toFloat()) * 90f
                } else {
                    0f
                }
                val bobX = sin(player.headBob * 0.5f) * 6f
                val bobY = sin(player.headBob) * 4f

                val gunOriginX = baseX + recoilX + bobX
                val gunOriginY = screenH - 10f + recoilY + reloadY + bobY

                drawFirstPersonWeapon(
                    drawScope = this,
                    type = weapon.type,
                    originX = gunOriginX,
                    originY = gunOriginY,
                    recoil = weapon.recoilAnim,
                    isADS = player.isADS
                )

                // Muzzle Flash
                if (weapon.recoilAnim > 0.45f) {
                    drawMuzzleFlash(
                        drawScope = this,
                        type = weapon.type,
                        originX = gunOriginX,
                        originY = gunOriginY,
                        recoil = weapon.recoilAnim,
                        isADS = player.isADS
                    )
                }

                // Laser sight beam projecting from barrel toward crosshair
                if (!player.isADS && (weapon.type == WeaponType.PISTOL || weapon.type == WeaponType.RIFLE)) {
                    val laserColor = if (weapon.type == WeaponType.PISTOL) Color(0xFFFF1744) else Color(0xFF00E5FF)
                    val barrelTipX = gunOriginX - 10f
                    val barrelTipY = gunOriginY - 140f
                    val crosshairTargetX = screenW * 0.5f
                    val crosshairTargetY = screenH * 0.5f + (player.pitch * screenH * 0.7f)
                    drawLine(
                        color = laserColor.copy(alpha = 0.45f),
                        start = Offset(barrelTipX, barrelTipY),
                        end = Offset(crosshairTargetX, crosshairTargetY),
                        strokeWidth = 1.6f
                    )
                    drawCircle(
                        color = laserColor,
                        radius = 2.5f,
                        center = Offset(crosshairTargetX, crosshairTargetY)
                    )
                }

                // Ejected Brass Shell Casing
                if (weapon.recoilAnim > 0.15f && weapon.type != WeaponType.ROCKET) {
                    val shellProgress = 1f - weapon.recoilAnim
                    val shellX = gunOriginX + 32f + shellProgress * 80f
                    val shellY = gunOriginY - 120f + (shellProgress * shellProgress * 130f)
                    drawRect(
                        color = Color(0xFFFFD54F), // Metallic brass
                        topLeft = Offset(shellX, shellY),
                        size = Size(10f, 5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SniperScopeOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val scopeRadius = (minOf(w, h) * 0.45f)

        // Dark scope vignette outer borders
        drawRect(color = Color(0xEE050B14))

        // Cutout transparent inner scope with green/cyan thermal tint
        drawCircle(
            color = Color(0x2200E5FF),
            radius = scopeRadius,
            center = Offset(centerX, centerY)
        )
        // Outer scope bezel
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = scopeRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = scopeRadius + 3f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 8f)
        )

        // Tactical Scope Crosshairs
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(centerX - scopeRadius, centerY),
            end = Offset(centerX + scopeRadius, centerY),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(centerX, centerY - scopeRadius),
            end = Offset(centerX, centerY + scopeRadius),
            strokeWidth = 2f
        )

        // Distance range ticks
        val tickOffsets = listOf(-60f, -30f, 30f, 60f)
        for (off in tickOffsets) {
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(centerX + off, centerY - 6f),
                end = Offset(centerX + off, centerY + 6f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(centerX - 6f, centerY + off),
                end = Offset(centerX + 6f, centerY + off),
                strokeWidth = 2f
            )
        }

        // Center pinpoint
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 3.5f,
            center = Offset(centerX, centerY)
        )
    }
}

private fun drawFirstPersonWeapon(
    drawScope: DrawScope,
    type: WeaponType,
    originX: Float,
    originY: Float,
    recoil: Float,
    isADS: Boolean
) {
    when (type) {
        WeaponType.PISTOL -> drawPistol(drawScope, originX, originY, isADS)
        WeaponType.RIFLE -> drawAssaultRifle(drawScope, originX, originY, isADS)
        WeaponType.SHOTGUN -> drawShotgun(drawScope, originX, originY, isADS)
        WeaponType.SNIPER -> drawSniperRifle(drawScope, originX, originY, isADS)
        WeaponType.ROCKET -> drawRocketLauncher(drawScope, originX, originY, isADS)
    }
}

private fun drawPistol(drawScope: DrawScope, x: Float, y: Float, isADS: Boolean) {
    val scale = if (isADS) 1.25f else 1.0f
    val w = 55f * scale
    val h = 140f * scale

    // Barrel / slide
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        topLeft = Offset(x - w * 0.5f, y - h),
        size = Size(w, h * 0.7f)
    )
    // Cyan slide serrations
    drawScope.drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(x - w * 0.35f, y - h + 15f),
        end = Offset(x + w * 0.35f, y - h + 15f),
        strokeWidth = 3f
    )
    // Grip
    drawScope.drawRect(
        color = Color(0xFF111827),
        topLeft = Offset(x - w * 0.35f, y - h * 0.35f),
        size = Size(w * 0.7f, h * 0.5f)
    )
    // Iron sight notch
    drawScope.drawRect(
        color = Color(0xFFFF5252),
        topLeft = Offset(x - 3f, y - h - 6f),
        size = Size(6f, 6f)
    )
}

private fun drawAssaultRifle(drawScope: DrawScope, x: Float, y: Float, isADS: Boolean) {
    val scale = if (isADS) 1.35f else 1.05f
    val w = 70f * scale
    val h = 190f * scale

    // Main Upper Receiver & Barrel
    drawScope.drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A))
        ),
        topLeft = Offset(x - w * 0.5f, y - h),
        size = Size(w, h * 0.75f)
    )

    // Holographic Sight Frame
    val sightW = w * 0.65f
    val sightH = 38f * scale
    drawScope.drawRect(
        color = Color(0xFF0A0F1D),
        topLeft = Offset(x - sightW * 0.5f, y - h - sightH + 10f),
        size = Size(sightW, sightH)
    )
    // Holographic cyan reticle dot
    drawScope.drawCircle(
        color = Color(0xFF00E5FF),
        radius = 4f,
        center = Offset(x, y - h - sightH * 0.5f + 10f)
    )
    drawScope.drawCircle(
        color = Color(0x6600E5FF),
        radius = 9f,
        center = Offset(x, y - h - sightH * 0.5f + 10f),
        style = Stroke(width = 1.8f)
    )

    // Cyber Carbon Guard Stripes
    drawScope.drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(x - w * 0.45f, y - h + 45f),
        end = Offset(x - w * 0.45f, y - h * 0.3f),
        strokeWidth = 3f
    )

    // Curved magazine
    drawScope.drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x - w * 0.35f, y - h * 0.3f),
        size = Size(w * 0.7f, h * 0.45f)
    )
}

private fun drawShotgun(drawScope: DrawScope, x: Float, y: Float, isADS: Boolean) {
    val scale = if (isADS) 1.3f else 1.05f
    val w = 85f * scale
    val h = 175f * scale

    // Heavy Double/Ribbed Barrel
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        topLeft = Offset(x - w * 0.45f, y - h),
        size = Size(w * 0.9f, h * 0.65f)
    )
    // Heat shield vent slots
    val ventY = y - h + 25f
    for (i in 0 until 4) {
        drawScope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(x - w * 0.35f, ventY + i * 16f),
            size = Size(w * 0.7f, 6f)
        )
    }

    // Heavy Pump Slide (tactical ribbed polymer)
    drawScope.drawRect(
        color = Color(0xFFFF6D00),
        topLeft = Offset(x - w * 0.5f, y - h * 0.5f),
        size = Size(w, h * 0.28f)
    )

    // Front bead sight
    drawScope.drawCircle(
        color = Color(0xFFFFD600),
        radius = 5f,
        center = Offset(x, y - h - 3f)
    )
}

private fun drawSniperRifle(drawScope: DrawScope, x: Float, y: Float, isADS: Boolean) {
    val w = 65f
    val h = 210f

    // Long Rail Barrel
    drawScope.drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(x - w * 0.35f, y - h),
        size = Size(w * 0.7f, h * 0.8f)
    )
    // Energy Acceleration Coils
    for (i in 0 until 5) {
        drawScope.drawRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(x - w * 0.45f, y - h + 18f + i * 22f),
            size = Size(w * 0.9f, 6f)
        )
    }

    // Telescopic Scope Tube on top
    drawScope.drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x - w * 0.4f, y - h * 0.65f),
        size = Size(w * 0.8f, 50f)
    )
}

private fun drawRocketLauncher(drawScope: DrawScope, x: Float, y: Float, isADS: Boolean) {
    val scale = if (isADS) 1.25f else 1.0f
    val w = 110f * scale
    val h = 185f * scale

    // Heavy Quad-Tube Chassis
    drawScope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        topLeft = Offset(x - w * 0.5f, y - h),
        size = Size(w, h * 0.75f)
    )

    // Loaded Warhead Tip
    drawScope.drawCircle(
        color = Color(0xFFFF5722),
        radius = w * 0.26f,
        center = Offset(x, y - h + 15f)
    )

    // Warning hazard stripes on side
    drawScope.drawLine(
        color = Color(0xFFFFD600),
        start = Offset(x - w * 0.4f, y - h * 0.45f),
        end = Offset(x + w * 0.4f, y - h * 0.45f),
        strokeWidth = 6f
    )
}

private fun drawMuzzleFlash(
    drawScope: DrawScope,
    type: WeaponType,
    originX: Float,
    originY: Float,
    recoil: Float,
    isADS: Boolean
) {
    val muzzleY = originY - 180f
    val flashRadius = (45f + recoil * 35f).coerceIn(30f, 95f)

    // Core bright star
    drawScope.drawCircle(
        color = Color.White,
        radius = flashRadius * 0.4f,
        center = Offset(originX, muzzleY)
    )
    // Outer flame glow
    val flameColor = when (type) {
        WeaponType.SNIPER -> Color(0xFF00E5FF)
        WeaponType.ROCKET -> Color(0xFFFF3D00)
        else -> Color(0xFFFFAB00)
    }
    drawScope.drawCircle(
        color = flameColor.copy(alpha = 0.8f),
        radius = flashRadius,
        center = Offset(originX, muzzleY)
    )

    // Flash spikes
    val spikeLen = flashRadius * 1.5f
    val path = Path().apply {
        moveTo(originX, muzzleY - spikeLen)
        lineTo(originX + flashRadius * 0.3f, muzzleY)
        lineTo(originX + spikeLen, muzzleY)
        lineTo(originX + flashRadius * 0.3f, muzzleY + flashRadius * 0.3f)
        lineTo(originX, muzzleY + spikeLen)
        lineTo(originX - flashRadius * 0.3f, muzzleY + flashRadius * 0.3f)
        lineTo(originX - spikeLen, muzzleY)
        lineTo(originX - flashRadius * 0.3f, muzzleY)
        close()
    }
    drawScope.drawPath(path, flameColor.copy(alpha = 0.65f))
}
