package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameStateStatus
import com.example.engine.GameViewModel
import com.example.engine.RaycastEngine
import com.example.model.GameMode
import com.example.model.WeaponType
import com.example.ui.components.Minimap
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.WeaponViewModelRenderer
import com.example.ui.theme.AmmoGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ShieldBlue

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateArmory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = uiState.player
    val activeWeapon = viewModel.getActiveWeapon()
    val raycastEngine = remember { RaycastEngine() }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalDark)
    ) {
        // 1. 3D Raycasting Viewport Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            raycastEngine.render(
                drawScope = this,
                player = player,
                map = viewModel.activeMap,
                enemies = viewModel.enemies,
                pickups = viewModel.pickups,
                projectiles = viewModel.projectiles,
                particles = viewModel.particles,
                combatTexts = viewModel.combatTexts,
                textMeasurer = textMeasurer
            )
        }

        // 2. Right-side Aim Look Pad (Drag anywhere on right side of screen to look/aim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 180.dp) // Leave left side for movement joystick
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            viewModel.updateLook(dragAmount.x, dragAmount.y)
                        }
                    )
                }
        )

        // 3. First-Person Animated Weapon Viewmodel
        WeaponViewModelRenderer(
            weapon = activeWeapon,
            player = player
        )

        // 4. Damage & Shield Vignette flashes
        if (player.hurtFlashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberRed.copy(alpha = player.hurtFlashAlpha * 0.45f))
            )
        }
        if (player.shieldFlashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ShieldBlue.copy(alpha = player.shieldFlashAlpha * 0.35f))
            )
        }

        // Helmet Visor Diegetic HUD Brackets
        HelmetVisorOverlay(modifier = Modifier.fillMaxSize())

        // Directional Damage Threat Arc
        if (player.hitIndicatorAlpha > 0f) {
            DirectionalDamageArc(
                modifier = Modifier.fillMaxSize(),
                alpha = player.hitIndicatorAlpha
            )
        }

        // 5. Crosshair & Hitmarker
        Crosshair(
            modifier = Modifier.align(Alignment.Center),
            isHitmarker = uiState.crosshairHitmarker,
            isADS = player.isADS,
            isSprinting = player.isSprinting
        )

        // 6. Top Bar HUD (Minimap, Wave/Objective, Score, Health & Shield, Pause)
        TopHudBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            viewModel = viewModel,
            uiState = uiState
        )

        // 7. Kill Feed Overlay (Top Right)
        KillFeedOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 16.dp),
            killFeed = uiState.killFeed
        )

        // 8. Wave Intermission Banner
        if (uiState.waveIntermissionSeconds > 0) {
            WaveIntermissionBanner(
                modifier = Modifier.align(Alignment.Center),
                seconds = uiState.waveIntermissionSeconds,
                wave = uiState.currentWave + 1
            )
        }

        // 9. Bottom Tactical Controls & HUD
        BottomControlsBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            viewModel = viewModel,
            uiState = uiState
        )

        // 10. Pause Menu Dialog
        if (uiState.status == GameStateStatus.PAUSED) {
            PauseMenuDialog(
                viewModel = viewModel,
                uiState = uiState,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.startGame(uiState.gameMode, uiState.arenaId) },
                onArmory = {
                    viewModel.returnToMenu()
                    onNavigateArmory()
                },
                onExit = { viewModel.returnToMenu() }
            )
        }

        // 11. Game Over / Victory Summary Screens
        if (uiState.status == GameStateStatus.GAME_OVER || uiState.status == GameStateStatus.VICTORY) {
            MatchSummaryDialog(
                uiState = uiState,
                isVictory = uiState.status == GameStateStatus.VICTORY,
                onPlayAgain = { viewModel.startGame(uiState.gameMode, uiState.arenaId) },
                onArmory = {
                    viewModel.returnToMenu()
                    onNavigateArmory()
                },
                onExit = { viewModel.returnToMenu() }
            )
        }
    }
}

@Composable
private fun Crosshair(
    modifier: Modifier = Modifier,
    isHitmarker: Boolean,
    isADS: Boolean,
    isSprinting: Boolean
) {
    val spread = if (isADS) 6.dp else if (isSprinting) 24.dp else 12.dp

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val spreadPx = spread.toPx()
            val lineLen = 8.dp.toPx()
            val color = if (isHitmarker) CyberRed else Color(0xCC00E5FF)
            val strokeW = if (isHitmarker) 2.5.dp.toPx() else 1.8.dp.toPx()

            // 4 reticle ticks
            drawLine(color, Offset(center.x, center.y - spreadPx - lineLen), Offset(center.x, center.y - spreadPx), strokeW)
            drawLine(color, Offset(center.x, center.y + spreadPx), Offset(center.x, center.y + spreadPx + lineLen), strokeW)
            drawLine(color, Offset(center.x - spreadPx - lineLen, center.y), Offset(center.x - spreadPx, center.y), strokeW)
            drawLine(color, Offset(center.x + spreadPx, center.y), Offset(center.x + spreadPx + lineLen, center.y), strokeW)

            // Center red dot
            drawCircle(
                color = if (isHitmarker) CyberRed else CyberCyan,
                radius = 2.dp.toPx(),
                center = center
            )

            // Hitmarker 'X'
            if (isHitmarker) {
                val hitLen = 9.dp.toPx()
                drawLine(CyberRed, Offset(center.x - hitLen, center.y - hitLen), Offset(center.x + hitLen, center.y + hitLen), 3.dp.toPx())
                drawLine(CyberRed, Offset(center.x + hitLen, center.y - hitLen), Offset(center.x - hitLen, center.y + hitLen), 3.dp.toPx())
            }
        }
    }
}

@Composable
private fun TopHudBar(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel,
    uiState: com.example.engine.GameUiState
) {
    val player = uiState.player

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Tactical Minimap Radar
        Minimap(
            player = player,
            map = viewModel.activeMap,
            enemies = viewModel.enemies,
            pickups = viewModel.pickups,
            size = 84.dp
        )

        // Center: Mode Objective & Wave Status + Score
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC070B14))
                .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            val objectiveText = when (uiState.gameMode) {
                GameMode.WAVE_DEFENSE -> "WAVE ${uiState.currentWave} / ${uiState.totalWaves}"
                GameMode.DEATHMATCH -> "KILLS: ${uiState.botKills} / ${uiState.deathmatchTargetKills}"
                GameMode.FIRING_RANGE -> "TARGET PRACTICE"
            }
            Text(
                text = objectiveText,
                color = CyberCyan,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )

            if (uiState.gameMode == GameMode.WAVE_DEFENSE) {
                Text(
                    text = "${uiState.waveEnemiesLeft} HOSTILES REMAINING",
                    color = CyberOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SCORE ${uiState.totalScore}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                if (player.combo > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "x${player.combo}",
                        color = CyberYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Right: Health & Shield Meters + Pause Button
        Row(verticalAlignment = Alignment.Top) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC070B14))
                    .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Shield Meter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SHD ${player.shield.toInt()}",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(player.shield / player.maxShield)
                                .height(6.dp)
                                .background(ShieldBlue)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Health Meter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HP ${player.health.toInt()}",
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(player.health / player.maxHealth)
                                .height(6.dp)
                                .background(CyberGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pause Button
            IconButton(
                onClick = { viewModel.pauseGame() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA070B14))
                    .border(1.dp, Color(0x6600E5FF), CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Game",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun KillFeedOverlay(
    modifier: Modifier = Modifier,
    killFeed: List<com.example.engine.KillNotification>
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        for (item in killFeed.takeLast(3)) {
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xBB0A0F1D))
                    .border(1.dp, if (item.isHeadshot) CyberYellow else Color(0x3300E5FF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.text,
                    color = if (item.isHeadshot) CyberYellow else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun WaveIntermissionBanner(
    modifier: Modifier = Modifier,
    seconds: Int,
    wave: Int
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE0B132B))
            .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WAVE COMPLETED!",
            color = CyberGreen,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "INCOMING WAVE $wave IN $seconds",
            color = CyberYellow,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun BottomControlsBar(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel,
    uiState: com.example.engine.GameUiState
) {
    val player = uiState.player
    val weapon = viewModel.getActiveWeapon()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left: Virtual Movement Joystick & Stance Toggles
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Sprint Button
                Button(
                    onClick = { viewModel.toggleSprint() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isSprinting) CyberCyan else Color(0x880B132B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("sprint_button")
                ) {
                    Text(
                        text = if (player.isSprinting) "SPRINT ON" else "SPRINT",
                        color = if (player.isSprinting) Color.Black else CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Crouch Button
                Button(
                    onClick = { viewModel.toggleCrouch() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isCrouching) CyberOrange else Color(0x880B132B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("crouch_button")
                ) {
                    Text(
                        text = if (player.isCrouching) "CROUCH" else "STAND",
                        color = if (player.isCrouching) Color.Black else CyberOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Virtual Joystick
            VirtualJoystick(
                size = 120.dp,
                onMove = { f, s -> viewModel.updateMovement(f, s) }
            )
        }

        // Center: Weapon Switcher Row
        WeaponSwitcherRow(
            viewModel = viewModel,
            player = player
        )

        // Right: ADS, Reload & Big Fire Trigger Button
        Column(horizontalAlignment = Alignment.End) {
            // Ammo Counter & Reload button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC070B14))
                    .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${weapon.currentClip}",
                        color = if (weapon.currentClip <= 3) CyberRed else AmmoGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "/ ${weapon.reserveAmmo}",
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Reload Button
                IconButton(
                    onClick = { viewModel.triggerReload() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x880B132B))
                        .testTag("reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = if (weapon.isReloading) CyberCyan else AmmoGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // ADS (Scope/Aim Down Sights) Toggle
                Button(
                    onClick = { viewModel.toggleADS() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isADS) CyberCyan else Color(0xAA0B132B)
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("ads_button")
                ) {
                    Text(
                        text = "ADS",
                        color = if (player.isADS) Color.Black else CyberCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                // Big Primary Fire Button with Press-and-Hold
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF3D00), Color(0xFFD50000))
                            )
                        )
                        .border(3.dp, CyberOrange, CircleShape)
                        .testTag("fire_button")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.setFiring(true)
                                    tryAwaitRelease()
                                    viewModel.setFiring(false)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FIRE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeaponSwitcherRow(
    viewModel: GameViewModel,
    player: com.example.model.Player
) {
    val weapons = viewModel.availableWeapons

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC070B14))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        weapons.forEachIndexed { idx, type ->
            val upg = viewModel.weaponUpgrades[type]
            val isUnlocked = upg?.isUnlocked == true
            val isSelected = player.selectedWeaponIndex == idx

            val label = when (type) {
                WeaponType.PISTOL -> "P-9"
                WeaponType.RIFLE -> "V-7"
                WeaponType.SHOTGUN -> "SG"
                WeaponType.SNIPER -> "SNP"
                WeaponType.ROCKET -> "RPG"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) CyberCyan else if (isUnlocked) Color(0x661E293B) else Color(0x330F172A)
                    )
                    .clickable(enabled = isUnlocked) {
                        viewModel.selectWeapon(idx)
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("weapon_select_$label"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.Black else if (isUnlocked) Color.White else Color(0xFF475569),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PauseMenuDialog(
    viewModel: GameViewModel,
    uiState: com.example.engine.GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onArmory: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GunmetalSurface)
                .border(2.dp, CyberCyan, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "COMBAT PAUSED",
                color = CyberCyan,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sensitivity Slider
            Text(
                text = "Aim Sensitivity: ${(uiState.lookSensitivity * 10).toInt() / 10f}x",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = uiState.lookSensitivity,
                onValueChange = { viewModel.setSensitivity(it) },
                valueRange = 0.4f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Invert look toggle
            Button(
                onClick = { viewModel.toggleInvertLook() },
                colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.invertLookY) "Invert Pitch: ON" else "Invert Pitch: OFF",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resume_button")
            ) {
                Text("RESUME BATTLE", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RESTART MATCH", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onArmory,
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ARMORY & UPGRADES", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RETURN TO MAIN MENU", color = CyberRed)
            }
        }
    }
}

@Composable
private fun MatchSummaryDialog(
    uiState: com.example.engine.GameUiState,
    isVictory: Boolean,
    onPlayAgain: () -> Unit,
    onArmory: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (uiState.shotsFired > 0) {
        ((uiState.shotsHit.toFloat() / uiState.shotsFired) * 100).toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GunmetalSurface)
                .border(2.dp, if (isVictory) CyberGreen else CyberRed, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isVictory) "VICTORY ACHIEVED" else "MISSION FAILED",
                color = if (isVictory) CyberGreen else CyberRed,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GunmetalCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatRow("Total Score", "${uiState.totalScore}")
                StatRow("Hostiles Eliminated", "${uiState.player.kills}")
                StatRow("Headshots", "${uiState.player.headshots}")
                StatRow("Shot Accuracy", "$accuracy%")
                StatRow("Credits Balance", "${uiState.credits} C", highlightColor = CyberYellow)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = if (isVictory) CyberGreen else CyberCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("play_again_button")
            ) {
                Text(
                    text = if (isVictory) "PLAY NEXT ARENA" else "DEPLOY AGAIN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onArmory,
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_armory_button")
            ) {
                Text("UPGRADE ARMORY", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MAIN MENU", color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, highlightColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = highlightColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun HelmetVisorOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bracketSize = 24.dp.toPx()
        val bracketColor = Color(0x3300E5FF)
        val strokeW = 1.5.dp.toPx()

        // Top-left bracket
        drawLine(bracketColor, Offset(16.dp.toPx(), 40.dp.toPx()), Offset(16.dp.toPx() + bracketSize, 40.dp.toPx()), strokeW)
        drawLine(bracketColor, Offset(16.dp.toPx(), 40.dp.toPx()), Offset(16.dp.toPx(), 40.dp.toPx() + bracketSize), strokeW)

        // Top-right bracket
        drawLine(bracketColor, Offset(w - 16.dp.toPx(), 40.dp.toPx()), Offset(w - 16.dp.toPx() - bracketSize, 40.dp.toPx()), strokeW)
        drawLine(bracketColor, Offset(w - 16.dp.toPx(), 40.dp.toPx()), Offset(w - 16.dp.toPx(), 40.dp.toPx() + bracketSize), strokeW)

        // Bottom-left bracket
        drawLine(bracketColor, Offset(16.dp.toPx(), h - 40.dp.toPx()), Offset(16.dp.toPx() + bracketSize, h - 40.dp.toPx()), strokeW)
        drawLine(bracketColor, Offset(16.dp.toPx(), h - 40.dp.toPx()), Offset(16.dp.toPx(), h - 40.dp.toPx() - bracketSize), strokeW)

        // Bottom-right bracket
        drawLine(bracketColor, Offset(w - 16.dp.toPx(), h - 40.dp.toPx()), Offset(w - 16.dp.toPx() - bracketSize, h - 40.dp.toPx()), strokeW)
        drawLine(bracketColor, Offset(w - 16.dp.toPx(), h - 40.dp.toPx()), Offset(w - 16.dp.toPx(), h - 40.dp.toPx() - bracketSize), strokeW)
    }
}

@Composable
private fun DirectionalDamageArc(modifier: Modifier = Modifier, alpha: Float) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = 70.dp.toPx()

        // Threat Arc indicator around reticle
        drawArc(
            color = CyberRed.copy(alpha = alpha.coerceIn(0f, 0.8f)),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

