package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.ArenaMap
import com.example.model.EnemyBot
import com.example.model.EnemyType
import com.example.model.FloatingCombatText
import com.example.model.Particle
import com.example.model.Pickup
import com.example.model.PickupType
import com.example.model.Player
import com.example.model.Projectile
import com.example.model.ProjectileType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RaycastEngine {

    companion object {
        const val NUM_RAYS = 140 // High performance, crisp 3D resolution
        const val FOV = 0.85f    // ~75 degree field of view
        const val MAX_DEPTH = 18f
    }

    private val zBuffer = FloatArray(NUM_RAYS)

    fun render(
        drawScope: DrawScope,
        player: Player,
        map: ArenaMap,
        enemies: List<EnemyBot>,
        pickups: List<Pickup>,
        projectiles: List<Projectile>,
        particles: List<Particle>,
        combatTexts: List<FloatingCombatText>,
        textMeasurer: TextMeasurer
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return

        val columnWidth = width / NUM_RAYS
        val pitchOffset = (player.pitch * height * 0.7f) + (player.headBob * 12f)
        val horizonY = height * 0.5f + pitchOffset

        // 1. Draw Sci-Fi Skybox & Floor Gradients
        drawSkyAndFloor(drawScope, width, height, horizonY, player)

        // 2. Raycast Walls & Fill Z-Buffer
        renderWalls(drawScope, player, map, width, height, horizonY, columnWidth)

        // 3. Render 3D Sprites (Pickups, Enemies, Projectiles, Particles)
        renderSprites(
            drawScope = drawScope,
            player = player,
            width = width,
            height = height,
            horizonY = horizonY,
            columnWidth = columnWidth,
            enemies = enemies,
            pickups = pickups,
            projectiles = projectiles,
            particles = particles,
            combatTexts = combatTexts,
            textMeasurer = textMeasurer
        )
    }

    private fun drawSkyAndFloor(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        horizonY: Float,
        player: Player
    ) {
        val muzzleGlow = player.muzzleLight * 0.25f

        // 1. Skybox: Deep cyberpunk atmosphere with volumetric horizon glow
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF020610),
                    Color(0xFF091224),
                    Color(0xFF0C1930)
                ),
                startY = 0f,
                endY = horizonY.coerceAtLeast(1f)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY.coerceAtLeast(0f))
        )

        // Distant Cyber City Skyline Silhouettes on Horizon
        val skylineY = horizonY - 26f
        if (skylineY > 0f) {
            val numBuildings = 14
            val bWidth = width / numBuildings
            for (i in 0 until numBuildings) {
                val bHeight = ((i * 7 + 3) % 5 + 2) * 4f
                drawScope.drawRect(
                    color = Color(0x3300E5FF),
                    topLeft = Offset(i * bWidth + 2f, horizonY - bHeight),
                    size = Size(bWidth - 4f, bHeight)
                )
                // Beacon light on top of tall buildings
                if (i % 3 == 0) {
                    drawScope.drawCircle(
                        color = Color(0xFFFF1744),
                        radius = 1.8f,
                        center = Offset(i * bWidth + bWidth * 0.5f, horizonY - bHeight - 2f)
                    )
                }
            }
        }

        // 2. Floor: Metallic arena floor with perspective grid lines & dynamic muzzle light reflection
        val floorBase = Color(0xFF080D18)
        val floorLit = Color(
            red = (floorBase.red + muzzleGlow * 1.2f).coerceIn(0f, 1f),
            green = (floorBase.green + muzzleGlow * 0.8f).coerceIn(0f, 1f),
            blue = (floorBase.blue + muzzleGlow * 0.4f).coerceIn(0f, 1f)
        )

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    floorLit,
                    Color(0xFF050811),
                    Color(0xFF020306)
                ),
                startY = horizonY,
                endY = height
            ),
            topLeft = Offset(0f, horizonY.coerceAtLeast(0f)),
            size = Size(width, (height - horizonY).coerceAtLeast(0f))
        )

        // Perspective Floor Grid Lines
        val floorHeight = (height - horizonY).coerceAtLeast(1f)
        val numLines = 6
        for (i in 1..numLines) {
            val p = (i.toFloat() / (numLines + 1))
            val lineY = horizonY + p * p * floorHeight
            val alpha = (p * 0.25f + muzzleGlow * 0.3f).coerceIn(0f, 0.6f)
            drawScope.drawLine(
                color = Color(0xFF00E5FF).copy(alpha = alpha),
                start = Offset(0f, lineY),
                end = Offset(width, lineY),
                strokeWidth = 1.2f
            )
        }

        // Horizon tactical glow line
        drawScope.drawLine(
            color = Color(0x5500E5FF),
            start = Offset(0f, horizonY),
            end = Offset(width, horizonY),
            strokeWidth = 1.5f
        )
    }

    private fun renderWalls(
        drawScope: DrawScope,
        player: Player,
        map: ArenaMap,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float
    ) {
        val dirX = player.dirX
        val dirY = player.dirY
        // Camera plane perpendicular to direction
        val planeX = -dirY * FOV
        val planeY = dirX * FOV

        for (x in 0 until NUM_RAYS) {
            val cameraX = 2f * x / NUM_RAYS - 1f
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = player.x.toInt()
            var mapY = player.y.toInt()

            val deltaDistX = if (rayDirX == 0f) 1e30f else abs(1f / rayDirX)
            val deltaDistY = if (rayDirY == 0f) 1e30f else abs(1f / rayDirY)

            val stepX: Int
            var sideDistX: Float
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (player.x - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1f - player.x) * deltaDistX
            }

            val stepY: Int
            var sideDistY: Float
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (player.y - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1f - player.y) * deltaDistY
            }

            var hit = false
            var side = 0 // 0 for X-wall (North/South), 1 for Y-wall (East/West)
            var wallType = 1
            var steps = 0

            while (!hit && steps < 30) {
                steps++
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                val tile = map.getTile(mapX, mapY)
                if (tile > 0) {
                    hit = true
                    wallType = tile
                }
            }

            val perpWallDist = if (side == 0) {
                (mapX - player.x + (1 - stepX) / 2f) / rayDirX
            } else {
                (mapY - player.y + (1 - stepY) / 2f) / rayDirY
            }.coerceAtLeast(0.05f)

            zBuffer[x] = perpWallDist

            // Calculate wall line height
            val lineHeight = (height / perpWallDist)
            val drawStart = horizonY - lineHeight * 0.5f
            val drawEnd = horizonY + lineHeight * 0.5f

            // Wall texture coordinate (0f to 1f)
            val wallX = if (side == 0) {
                player.y + perpWallDist * rayDirY
            } else {
                player.x + perpWallDist * rayDirX
            }
            val texX = wallX - wallX.toInt().toFloat()

            // Wall color with depth fog & dynamic muzzle flash illumination
            val fogFactor = (1f - (perpWallDist / MAX_DEPTH).coerceIn(0f, 1f))
            val baseColor = getWallColor(wallType, side, texX)

            // Dynamic Muzzle Flash Real-Time Illumination
            val muzzleGlow = player.muzzleLight * (1f - (perpWallDist / 8f)).coerceIn(0f, 1f)

            val finalColor = Color(
                red = (baseColor.red * fogFactor + muzzleGlow * 0.45f).coerceIn(0f, 1f),
                green = (baseColor.green * fogFactor + muzzleGlow * 0.32f).coerceIn(0f, 1f),
                blue = (baseColor.blue * fogFactor + muzzleGlow * 0.15f).coerceIn(0f, 1f),
                alpha = 1f
            )

            // Draw vertical column slice
            val colLeft = x * columnWidth
            val sliceHeight = (drawEnd - drawStart).coerceAtLeast(1f)
            drawScope.drawRect(
                color = finalColor,
                topLeft = Offset(colLeft, drawStart),
                size = Size(columnWidth + 0.5f, sliceHeight)
            )

            // Ambient Occlusion shadow at top and bottom of wall slice
            val aoHeight = (sliceHeight * 0.08f).coerceIn(2f, 18f)
            drawScope.drawRect(
                color = Color(0x66000000),
                topLeft = Offset(colLeft, drawStart),
                size = Size(columnWidth + 0.5f, aoHeight)
            )
            drawScope.drawRect(
                color = Color(0x66000000),
                topLeft = Offset(colLeft, drawEnd - aoHeight),
                size = Size(columnWidth + 0.5f, aoHeight)
            )

            // Sci-fi wall horizontal accent line (e.g. glowing neon trim)
            if (fogFactor > 0.22f) {
                val trimY = drawStart + sliceHeight * 0.35f
                val trimColor = getWallTrimColor(wallType, fogFactor + muzzleGlow * 0.4f)
                drawScope.drawRect(
                    color = trimColor,
                    topLeft = Offset(colLeft, trimY),
                    size = Size(columnWidth + 0.5f, (sliceHeight * 0.05f).coerceIn(1.5f, 6f))
                )
            }
        }
    }

    private fun getWallColor(wallType: Int, side: Int, texX: Float): Color {
        val shade = if (side == 1) 0.78f else 1.0f
        // Texture groove simulation: subtle vertical panel divisions
        val groove = if (texX < 0.04f || texX > 0.96f || abs(texX - 0.5f) < 0.03f) 0.72f else 1.0f
        val factor = shade * groove

        return when (wallType) {
            1 -> Color( // Cyber Steel Wall (heavy alloy with metallic cyan tint)
                (0.18f * factor).coerceIn(0f, 1f),
                (0.24f * factor).coerceIn(0f, 1f),
                (0.35f * factor).coerceIn(0f, 1f)
            )
            2 -> Color( // Neon Circuit Wall (dark carbon blue)
                (0.08f * factor).coerceIn(0f, 1f),
                (0.15f * factor).coerceIn(0f, 1f),
                (0.28f * factor).coerceIn(0f, 1f)
            )
            3 -> Color( // Hazard Security Barrier (amber/hazard orange stripes)
                if ((texX * 6).toInt() % 2 == 0) (0.85f * factor).coerceIn(0f, 1f) else (0.15f * factor).coerceIn(0f, 1f),
                if ((texX * 6).toInt() % 2 == 0) (0.55f * factor).coerceIn(0f, 1f) else (0.15f * factor).coerceIn(0f, 1f),
                (0.05f * factor).coerceIn(0f, 1f)
            )
            4 -> Color( // Bio-Containment Mesh (neon emerald green)
                (0.06f * factor).coerceIn(0f, 1f),
                (0.45f * factor).coerceIn(0f, 1f),
                (0.28f * factor).coerceIn(0f, 1f)
            )
            5 -> Color( // Red Sector Blast Fortress (reinforced crimson steel)
                (0.55f * factor).coerceIn(0f, 1f),
                (0.12f * factor).coerceIn(0f, 1f),
                (0.16f * factor).coerceIn(0f, 1f)
            )
            else -> Color(0.2f * factor, 0.2f * factor, 0.25f * factor)
        }
    }

    private fun getWallTrimColor(wallType: Int, fogFactor: Float): Color {
        return when (wallType) {
            1 -> Color(0f, 0.89f * fogFactor, 1f * fogFactor, 0.85f) // Glowing cyan
            2 -> Color(0f, 0.75f * fogFactor, 1f * fogFactor, 0.9f) // Bright electric blue
            3 -> Color(1f * fogFactor, 0.85f * fogFactor, 0f, 0.9f) // Warning hazard yellow
            4 -> Color(0f, 1f * fogFactor, 0.45f * fogFactor, 0.85f) // Glowing radioactive green
            5 -> Color(1f * fogFactor, 0.15f * fogFactor, 0.25f * fogFactor, 0.9f) // Warning red strobe
            else -> Color(0f, 0.8f * fogFactor, 1f * fogFactor, 0.7f)
        }
    }

    private data class DrawableSprite(
        val x: Float,
        val y: Float,
        val distance: Float,
        val drawAction: () -> Unit
    )

    private fun renderSprites(
        drawScope: DrawScope,
        player: Player,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float,
        enemies: List<EnemyBot>,
        pickups: List<Pickup>,
        projectiles: List<Projectile>,
        particles: List<Particle>,
        combatTexts: List<FloatingCombatText>,
        textMeasurer: TextMeasurer
    ) {
        val dirX = player.dirX
        val dirY = player.dirY
        val planeX = -dirY * FOV
        val planeY = dirX * FOV
        val invDet = 1.0f / (planeX * dirY - dirX * planeY)

        val spriteList = mutableListOf<DrawableSprite>()

        // 1. Pickups
        for (pickup in pickups) {
            val dx = pickup.x - player.x
            val dy = pickup.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > MAX_DEPTH) continue

            spriteList.add(
                DrawableSprite(pickup.x, pickup.y, dist) {
                    projectAndDrawPickup(
                        drawScope = drawScope,
                        pickup = pickup,
                        player = player,
                        width = width,
                        height = height,
                        horizonY = horizonY,
                        columnWidth = columnWidth,
                        planeX = planeX,
                        planeY = planeY,
                        dirX = dirX,
                        dirY = dirY,
                        invDet = invDet
                    )
                }
            )
        }

        // 2. Enemies
        for (enemy in enemies) {
            val dx = enemy.x - player.x
            val dy = enemy.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > MAX_DEPTH) continue

            spriteList.add(
                DrawableSprite(enemy.x, enemy.y, dist) {
                    projectAndDrawEnemy(
                        drawScope = drawScope,
                        enemy = enemy,
                        player = player,
                        width = width,
                        height = height,
                        horizonY = horizonY,
                        columnWidth = columnWidth,
                        planeX = planeX,
                        planeY = planeY,
                        dirX = dirX,
                        dirY = dirY,
                        invDet = invDet
                    )
                }
            )
        }

        // 3. Projectiles (Rockets, Plasma, Lasers)
        for (proj in projectiles) {
            val dx = proj.x - player.x
            val dy = proj.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > MAX_DEPTH) continue

            spriteList.add(
                DrawableSprite(proj.x, proj.y, dist) {
                    projectAndDrawProjectile(
                        drawScope = drawScope,
                        proj = proj,
                        width = width,
                        height = height,
                        horizonY = horizonY,
                        columnWidth = columnWidth,
                        planeX = planeX,
                        planeY = planeY,
                        dirX = dirX,
                        dirY = dirY,
                        invDet = invDet,
                        player = player
                    )
                }
            )
        }

        // 4. Floating 3D Combat Text (Damage numbers, HEADSHOT!)
        for (text in combatTexts) {
            val dx = text.x - player.x
            val dy = text.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > MAX_DEPTH) continue

            spriteList.add(
                DrawableSprite(text.x, text.y, dist) {
                    projectAndDrawCombatText(
                        drawScope = drawScope,
                        text = text,
                        width = width,
                        height = height,
                        horizonY = horizonY,
                        planeX = planeX,
                        planeY = planeY,
                        dirX = dirX,
                        dirY = dirY,
                        invDet = invDet,
                        player = player,
                        textMeasurer = textMeasurer
                    )
                }
            )
        }

        // 5. Particles
        for (p in particles) {
            val dx = p.x - player.x
            val dy = p.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > MAX_DEPTH) continue

            spriteList.add(
                DrawableSprite(p.x, p.y, dist) {
                    projectAndDrawParticle(
                        drawScope = drawScope,
                        p = p,
                        width = width,
                        height = height,
                        horizonY = horizonY,
                        columnWidth = columnWidth,
                        planeX = planeX,
                        planeY = planeY,
                        dirX = dirX,
                        dirY = dirY,
                        invDet = invDet,
                        player = player
                    )
                }
            )
        }

        // Sort sprites by distance: farthest first (Painter's algorithm)
        spriteList.sortByDescending { it.distance }
        for (sprite in spriteList) {
            sprite.drawAction()
        }
    }

    private fun projectAndDrawEnemy(
        drawScope: DrawScope,
        enemy: EnemyBot,
        player: Player,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float,
        planeX: Float,
        planeY: Float,
        dirX: Float,
        dirY: Float,
        invDet: Float
    ) {
        val spriteX = enemy.x - player.x
        val spriteY = enemy.y - player.y

        val transformX = invDet * (dirY * spriteX - dirX * spriteY)
        val transformY = invDet * (-planeY * spriteX + planeX * spriteY) // depth

        if (transformY <= 0.25f) return

        val screenX = ((1f + transformX / transformY) * 0.5f * width)
        val scale = when (enemy.type) {
            EnemyType.TITAN_BOSS -> 1.7f
            EnemyType.SCOUT_DRONE -> 0.75f
            EnemyType.CYBER_ENFORCER -> 1.0f
        }
        val spriteHeight = abs(height / transformY) * scale
        val spriteWidth = spriteHeight * 0.65f

        val startCol = ((screenX - spriteWidth * 0.5f) / columnWidth).toInt().coerceIn(0, NUM_RAYS - 1)
        val endCol = ((screenX + spriteWidth * 0.5f) / columnWidth).toInt().coerceIn(0, NUM_RAYS - 1)

        // Check if visible through Z-buffer
        var isVisible = false
        for (c in startCol..endCol) {
            if (transformY < zBuffer[c]) {
                isVisible = true
                break
            }
        }
        if (!isVisible) return

        val spriteTop = when (enemy.type) {
            EnemyType.SCOUT_DRONE -> horizonY - spriteHeight * 0.8f // Drones hover higher
            else -> horizonY - spriteHeight * 0.5f
        }
        val spriteLeft = screenX - spriteWidth * 0.5f

        val hitFlash = enemy.hitFlashTimer > 0f
        val bodyColor = if (hitFlash) Color.White else when (enemy.type) {
            EnemyType.SCOUT_DRONE -> Color(0xFF00E5FF)
            EnemyType.CYBER_ENFORCER -> Color(0xFFFF6D00)
            EnemyType.TITAN_BOSS -> Color(0xFFFF1744)
        }

        when (enemy.type) {
            EnemyType.SCOUT_DRONE -> drawScoutDrone(drawScope, spriteLeft, spriteTop, spriteWidth, spriteHeight, bodyColor, enemy)
            EnemyType.CYBER_ENFORCER -> drawCyberEnforcer(drawScope, spriteLeft, spriteTop, spriteWidth, spriteHeight, bodyColor, enemy)
            EnemyType.TITAN_BOSS -> drawTitanBoss(drawScope, spriteLeft, spriteTop, spriteWidth, spriteHeight, bodyColor, enemy)
        }

        // Draw Health Bar above head
        if (!enemy.isDead && transformY < 12f) {
            val barWidth = (spriteWidth * 0.9f).coerceIn(30f, 140f)
            val barHeight = (spriteHeight * 0.05f).coerceIn(5f, 10f)
            val barLeft = screenX - barWidth * 0.5f
            val barTop = spriteTop - barHeight - 8f

            val healthPct = (enemy.health / enemy.type.maxHealth).coerceIn(0f, 1f)
            // Background
            drawScope.drawRect(
                color = Color(0xAA000000),
                topLeft = Offset(barLeft - 1f, barTop - 1f),
                size = Size(barWidth + 2f, barHeight + 2f)
            )
            // Bar Fill
            val healthColor = if (healthPct > 0.5f) Color(0xFF00E676) else if (healthPct > 0.25f) Color(0xFFFFAB00) else Color(0xFFFF1744)
            drawScope.drawRect(
                color = healthColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth * healthPct, barHeight)
            )
        }
    }

    private fun drawScoutDrone(
        drawScope: DrawScope,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        bodyColor: Color,
        enemy: EnemyBot
    ) {
        val centerX = left + w * 0.5f
        val centerY = top + h * 0.5f
        val radius = w * 0.4f

        // Drone core body
        drawScope.drawCircle(
            color = bodyColor,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        // Cyber eye
        drawScope.drawCircle(
            color = Color(0xFFFF1744),
            radius = radius * 0.35f,
            center = Offset(centerX, centerY)
        )
        // Drone wings/rotors
        val wingSpan = w * 0.65f
        drawScope.drawLine(
            color = Color(0xFF90CAF9),
            start = Offset(centerX - wingSpan, centerY - radius * 0.5f),
            end = Offset(centerX + wingSpan, centerY - radius * 0.5f),
            strokeWidth = (h * 0.08f).coerceAtLeast(2f)
        )
        // Thruster glow
        drawScope.drawCircle(
            color = Color(0x9900E5FF),
            radius = radius * 0.45f,
            center = Offset(centerX, centerY + radius * 0.85f)
        )
    }

    private fun drawCyberEnforcer(
        drawScope: DrawScope,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        bodyColor: Color,
        enemy: EnemyBot
    ) {
        val centerX = left + w * 0.5f
        val headRadius = w * 0.22f
        val headY = top + headRadius + 4f

        // Head helmet
        drawScope.drawCircle(
            color = bodyColor,
            radius = headRadius,
            center = Offset(centerX, headY)
        )
        // Visor slit
        drawScope.drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(centerX - headRadius * 0.7f, headY),
            end = Offset(centerX + headRadius * 0.7f, headY),
            strokeWidth = (h * 0.04f).coerceAtLeast(2f)
        )

        // Armored Torso
        val torsoTop = headY + headRadius * 0.8f
        val torsoHeight = h * 0.45f
        val torsoWidth = w * 0.7f
        drawScope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(centerX - torsoWidth * 0.5f, torsoTop),
            size = Size(torsoWidth, torsoHeight)
        )
        // Chest plate accent
        drawScope.drawRect(
            color = bodyColor,
            topLeft = Offset(centerX - torsoWidth * 0.35f, torsoTop + torsoHeight * 0.2f),
            size = Size(torsoWidth * 0.7f, torsoHeight * 0.35f)
        )

        // Weapon in hands
        val weaponX = centerX + torsoWidth * 0.2f
        val weaponY = torsoTop + torsoHeight * 0.45f
        drawScope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(weaponX, weaponY),
            size = Size(w * 0.4f, h * 0.08f)
        )

        // Legs
        val legTop = torsoTop + torsoHeight
        val legWidth = w * 0.24f
        val legHeight = h * 0.32f
        val walkOffset = sin(enemy.walkCycle) * (w * 0.12f)
        // Left leg
        drawScope.drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(centerX - torsoWidth * 0.4f + walkOffset, legTop),
            size = Size(legWidth, legHeight)
        )
        // Right leg
        drawScope.drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(centerX + torsoWidth * 0.4f - legWidth - walkOffset, legTop),
            size = Size(legWidth, legHeight)
        )
    }

    private fun drawTitanBoss(
        drawScope: DrawScope,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        bodyColor: Color,
        enemy: EnemyBot
    ) {
        val centerX = left + w * 0.5f
        val centerY = top + h * 0.45f

        // Heavy Mech chassis
        drawScope.drawRect(
            color = Color(0xFF1A1A24),
            topLeft = Offset(left + w * 0.1f, top + h * 0.2f),
            size = Size(w * 0.8f, h * 0.45f)
        )
        // Heavy armor plating
        drawScope.drawRect(
            color = bodyColor,
            topLeft = Offset(left + w * 0.2f, top + h * 0.28f),
            size = Size(w * 0.6f, h * 0.25f)
        )
        // Central power core
        drawScope.drawCircle(
            color = Color(0xFFFFD600),
            radius = w * 0.14f,
            center = Offset(centerX, centerY)
        )

        // Dual Shoulder Missile Launchers
        drawScope.drawRect(
            color = Color(0xFF374151),
            topLeft = Offset(left, top + h * 0.12f),
            size = Size(w * 0.25f, h * 0.25f)
        )
        drawScope.drawRect(
            color = Color(0xFF374151),
            topLeft = Offset(left + w * 0.75f, top + h * 0.12f),
            size = Size(w * 0.25f, h * 0.25f)
        )

        // Heavy hydraulic legs
        val legW = w * 0.22f
        val legH = h * 0.38f
        drawScope.drawRect(
            color = Color(0xFF111827),
            topLeft = Offset(left + w * 0.15f, top + h * 0.62f),
            size = Size(legW, legH)
        )
        drawScope.drawRect(
            color = Color(0xFF111827),
            topLeft = Offset(left + w * 0.63f, top + h * 0.62f),
            size = Size(legW, legH)
        )
    }

    private fun projectAndDrawPickup(
        drawScope: DrawScope,
        pickup: Pickup,
        player: Player,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float,
        planeX: Float,
        planeY: Float,
        dirX: Float,
        dirY: Float,
        invDet: Float
    ) {
        val spriteX = pickup.x - player.x
        val spriteY = pickup.y - player.y

        val transformX = invDet * (dirY * spriteX - dirX * spriteY)
        val transformY = invDet * (-planeY * spriteX + planeX * spriteY)

        if (transformY <= 0.25f) return

        val screenX = ((1f + transformX / transformY) * 0.5f * width)
        val spriteSize = abs(height / transformY) * 0.45f

        val col = (screenX / columnWidth).toInt()
        if (col in 0 until NUM_RAYS && transformY >= zBuffer[col]) return

        val bobOffset = sin(pickup.bobPhase) * (spriteSize * 0.2f)
        val centerY = horizonY + bobOffset
        val centerX = screenX
        val radius = (spriteSize * 0.45f).coerceIn(8f, 75f)

        // Glowing outer circle
        drawScope.drawCircle(
            color = pickup.type.color.copy(alpha = 0.35f),
            radius = radius * 1.4f,
            center = Offset(centerX, centerY)
        )
        // Solid core
        drawScope.drawCircle(
            color = pickup.type.color,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        // Inner symbol
        when (pickup.type) {
            PickupType.MEDKIT -> {
                // Cross
                val crossW = radius * 0.6f
                val crossT = radius * 0.22f
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - crossW * 0.5f, centerY - crossT * 0.5f),
                    size = Size(crossW, crossT)
                )
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - crossT * 0.5f, centerY - crossW * 0.5f),
                    size = Size(crossT, crossW)
                )
            }
            PickupType.SHIELD -> {
                // Diamond
                val path = Path().apply {
                    moveTo(centerX, centerY - radius * 0.6f)
                    lineTo(centerX + radius * 0.6f, centerY)
                    lineTo(centerX, centerY + radius * 0.6f)
                    lineTo(centerX - radius * 0.6f, centerY)
                    close()
                }
                drawScope.drawPath(path, Color.White)
            }
            PickupType.AMMO -> {
                // Bullet cartridge
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - radius * 0.25f, centerY - radius * 0.55f),
                    size = Size(radius * 0.5f, radius * 1.1f)
                )
            }
            PickupType.DAMAGE_BOOST -> {
                // Bolt
                drawScope.drawCircle(
                    color = Color.White,
                    radius = radius * 0.35f,
                    center = Offset(centerX, centerY)
                )
            }
            PickupType.SPEED_STIM -> {
                // Star/flash
                drawScope.drawLine(
                    color = Color.White,
                    start = Offset(centerX - radius * 0.5f, centerY),
                    end = Offset(centerX + radius * 0.5f, centerY),
                    strokeWidth = radius * 0.25f
                )
            }
        }
    }

    private fun projectAndDrawProjectile(
        drawScope: DrawScope,
        proj: Projectile,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float,
        planeX: Float,
        planeY: Float,
        dirX: Float,
        dirY: Float,
        invDet: Float,
        player: Player
    ) {
        val spriteX = proj.x - player.x
        val spriteY = proj.y - player.y

        val transformX = invDet * (dirY * spriteX - dirX * spriteY)
        val transformY = invDet * (-planeY * spriteX + planeX * spriteY)

        if (transformY <= 0.15f) return

        val screenX = ((1f + transformX / transformY) * 0.5f * width)
        val col = (screenX / columnWidth).toInt()
        if (col in 0 until NUM_RAYS && transformY >= zBuffer[col]) return

        val size = (abs(height / transformY) * 0.18f).coerceIn(4f, 40f)
        val centerY = horizonY

        when (proj.type) {
            ProjectileType.ROCKET -> {
                // Rocket warhead & smoke flare
                drawScope.drawCircle(
                    color = Color(0xFFFF5722),
                    radius = size * 1.2f,
                    center = Offset(screenX, centerY)
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = size * 0.6f,
                    center = Offset(screenX, centerY)
                )
            }
            ProjectileType.PLASMA -> {
                drawScope.drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = size,
                    center = Offset(screenX, centerY)
                )
            }
            ProjectileType.LASER_BEAM, ProjectileType.BULLET -> {
                drawScope.drawCircle(
                    color = Color(0xFFFFD600),
                    radius = (size * 0.7f).coerceAtLeast(3f),
                    center = Offset(screenX, centerY)
                )
            }
        }
    }

    private fun projectAndDrawParticle(
        drawScope: DrawScope,
        p: Particle,
        width: Float,
        height: Float,
        horizonY: Float,
        columnWidth: Float,
        planeX: Float,
        planeY: Float,
        dirX: Float,
        dirY: Float,
        invDet: Float,
        player: Player
    ) {
        val spriteX = p.x - player.x
        val spriteY = p.y - player.y

        val transformX = invDet * (dirY * spriteX - dirX * spriteY)
        val transformY = invDet * (-planeY * spriteX + planeX * spriteY)

        if (transformY <= 0.1f) return

        val screenX = ((1f + transformX / transformY) * 0.5f * width)
        val col = (screenX / columnWidth).toInt()
        if (col in 0 until NUM_RAYS && transformY >= zBuffer[col]) return

        val particleScale = abs(height / transformY)
        val screenY = horizonY - (p.z * particleScale * 0.5f)
        val lifeRatio = (p.life / p.maxLife).coerceIn(0f, 1f)
        val rad = (p.size * particleScale * 0.05f * lifeRatio).coerceIn(2f, 25f)

        drawScope.drawCircle(
            color = p.color.copy(alpha = lifeRatio),
            radius = rad,
            center = Offset(screenX, screenY)
        )
    }

    private fun projectAndDrawCombatText(
        drawScope: DrawScope,
        text: FloatingCombatText,
        width: Float,
        height: Float,
        horizonY: Float,
        planeX: Float,
        planeY: Float,
        dirX: Float,
        dirY: Float,
        invDet: Float,
        player: Player,
        textMeasurer: TextMeasurer
    ) {
        val spriteX = text.x - player.x
        val spriteY = text.y - player.y

        val transformX = invDet * (dirY * spriteX - dirX * spriteY)
        val transformY = invDet * (-planeY * spriteX + planeX * spriteY)

        if (transformY <= 0.3f) return

        val screenX = ((1f + transformX / transformY) * 0.5f * width)
        val scale = abs(height / transformY)
        val screenY = horizonY - (text.z * scale * 0.7f) - ((1f - text.life) * 45f)

        val fontSize = if (text.isHeadshot) 20.sp else 16.sp
        val alpha = text.life.coerceIn(0f, 1f)

        val measured = textMeasurer.measure(
            text = text.text,
            style = TextStyle(
                color = text.color.copy(alpha = alpha),
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        )
        drawScope.drawText(
            textLayoutResult = measured,
            topLeft = Offset(screenX - measured.size.width * 0.5f, screenY)
        )
    }
}
