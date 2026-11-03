package com.example.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameViewModel
import com.example.model.ArenaId
import com.example.model.GameMode
import com.example.ui.theme.AmmoGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.GunmetalBorder
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalDark
import com.example.ui.theme.GunmetalSurface

enum class KnowledgeTab(val title: String) {
    OVERVIEW("Overview & Lore"),
    WEAPONS("Weapons Intel"),
    HOSTILES("Hostiles Guide"),
    ARENAS("Combat Arenas"),
    VERSION("Version & Engine")
}

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onStartGame: () -> Unit,
    onNavigateArmory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(GameMode.WAVE_DEFENSE) }
    var selectedArena by remember { mutableStateOf(ArenaId.NEO_TOKYO) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showDeploymentSheet by remember { mutableStateOf(false) }
    var selectedKnowledgeTab by remember { mutableStateOf<KnowledgeTab?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalDark)
    ) {
        // Dynamic Cyber Grid & Scanline Background
        AnimatedCyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Credits & Quick Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Version & System Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xBB0A0F1D))
                        .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "V1.0.4 • ONLINE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Credits Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xBB0A0F1D))
                        .border(1.dp, Color(0x55FFD600), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CREDITS: ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.credits} C", color = CyberYellow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hero Title Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1E36), Color(0xFF090E1A))
                        )
                    )
                    .border(2.dp, CyberCyan, RoundedCornerShape(22.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "APEX TRIGGER",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "3D TACTICAL CYBER OPS",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NATIVE 60FPS RAYCASTING • REAL-TIME 3D FPS",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // PRIMARY ACTION BUTTONS: Play, Settings, Exit (+ Armory)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. PLAY BUTTON
                Button(
                    onClick = {
                        viewModel.startGame(selectedMode, selectedArena)
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PLAY",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Quick Mission & Arena Selector Accordion
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66111827))
                        .border(1.dp, GunmetalBorder, RoundedCornerShape(12.dp))
                        .clickable { showDeploymentSheet = !showDeploymentSheet }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MISSION: ${selectedMode.displayName.uppercase()}",
                                color = CyberOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "ARENA: ${selectedArena.displayName}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = if (showDeploymentSheet) "HIDE OPTIONS ▲" else "CHANGE MISSION ▼",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded Mission & Arena Options
                AnimatedVisibility(
                    visible = showDeploymentSheet,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GunmetalSurface)
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("MISSION MODE", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        for (mode in GameMode.values()) {
                            val isSel = selectedMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF132238) else Color(0xFF0F172A))
                                    .border(1.dp, if (isSel) CyberCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedMode = mode }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSel) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Text("COMBAT ZONE", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (arena in ArenaId.values()) {
                                val isSel = selectedArena == arena
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFF132238) else Color(0xFF0F172A))
                                        .border(1.dp, if (isSel) CyberOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedArena = arena }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = arena.displayName.split(" ")[0],
                                        color = if (isSel) CyberOrange else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. ARMORY & UPGRADES BUTTON
                Button(
                    onClick = onNavigateArmory,
                    colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("armory_button")
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = CyberOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ARMORY & LOADOUTS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // 3. SETTINGS BUTTON
                Button(
                    onClick = { showSettingsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SETTINGS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // 4. EXIT BUTTON
                Button(
                    onClick = { showExitDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, Color(0x66FF1744), RoundedCornerShape(14.dp))
                        .testTag("exit_button")
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Exit", tint = CyberRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXIT",
                        color = CyberRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // GAME KNOWLEDGE & DOSSIER ARCHIVE (All knowledge about game and version)
            GameKnowledgeArchive(
                selectedTab = selectedKnowledgeTab,
                onSelectTab = { selectedKnowledgeTab = if (selectedKnowledgeTab == it) null else it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings Dialog Modal
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Exit Confirmation Dialog
        if (showExitDialog) {
            ExitConfirmationDialog(
                onConfirmExit = {
                    showExitDialog = false
                    (context as? Activity)?.finish()
                },
                onCancel = { showExitDialog = false }
            )
        }
    }
}

@Composable
private fun GameKnowledgeArchive(
    selectedTab: KnowledgeTab?,
    onSelectTab: (KnowledgeTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GunmetalSurface)
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TACTICAL INTEL & GAME KNOWLEDGE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        Text(
            text = "Select a dossier below to inspect complete mechanics, weapon telemetry, enemy behaviors, and engine specifications.",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Knowledge category pill buttons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (tab in KnowledgeTab.values()) {
                val isExpanded = selectedTab == tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isExpanded) Color(0xFF132238) else GunmetalCard)
                        .border(1.dp, if (isExpanded) CyberCyan else GunmetalBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("knowledge_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isExpanded) CyberCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            color = if (isExpanded) CyberCyan else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090E1A))
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        when (tab) {
                            KnowledgeTab.OVERVIEW -> KnowledgeOverview()
                            KnowledgeTab.WEAPONS -> KnowledgeWeapons()
                            KnowledgeTab.HOSTILES -> KnowledgeHostiles()
                            KnowledgeTab.ARENAS -> KnowledgeArenas()
                            KnowledgeTab.VERSION -> KnowledgeVersion()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeOverview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SETTING & LORE", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Text(
            text = "Year 2088. Deep underneath the neon megacity of Neo-Tokyo, autonomous cybernetic combat units and heavy Titan Mechs have overridden safety protocols in subterranean research facilities. You are the sole surviving Vanguard operative deployed with experimental military-grade cybernetic weaponry to purge the facility.",
            color = Color.LightGray,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Text("CORE GAMEPLAY MECHANICS", color = CyberOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Text(
            text = "• Real-Time First-Person Combat: Smooth dual-stick touch controls for movement, strafing, and 360-degree precision aiming.\n• Headshot Multipliers: Precision shots to enemy heads deal 1.5x to 2.5x critical damage.\n• Shield & Health Management: Cyber shields absorb 100% incoming kinetic damage first before health degrades. Pickups restore vital systems.\n• Dynamic Muzzle Lighting: Every gunshot illuminates the immediate corridor in real-time.",
            color = Color.LightGray,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun KnowledgeWeapons() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ARSENAL TELEMETRY", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
        WeaponFact("1. Nova-9 Tactical Pistol", "Semi-auto sidearm with 15 rounds, 28 DMG, fast 1.2s reload, and 2.0x headshot multiplier. Features laser targeting.")
        WeaponFact("2. V-7 Sentinel Assault Rifle", "Rapid-fire carbine at 550 RPM, 30-round mag, 34 DMG. Equipped with a holographic red dot optic.")
        WeaponFact("3. Devastator-12 Combat Shotgun", "Pump-action CQB powerhouse. Fires 8 buckshot pellets (16 DMG each = 128 burst). Ideal for close-quarters breach.")
        WeaponFact("4. Reaper X Plasma Sniper", "High-voltage bolt-action rifle, 120 base DMG with an extreme 2.5x headshot critical (300 DMG). Features telescopic ADS thermal scope.")
        WeaponFact("5. Havoc RPG Launcher", "Heavy explosive ordnance dealing 240 AOE damage across a 3.2m radius with intense screen shake.")
    }
}

@Composable
private fun WeaponFact(name: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(name, color = CyberYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(desc, color = Color.LightGray, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun KnowledgeHostiles() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HOSTILE COMBAT UNITS", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
        WeaponFact("Scout Drone (65 HP, High Speed)", "Agile hovering dual-rotor drones that fire rapid plasma bursts. Keep moving and counter with shotguns or rapid rifle fire.")
        WeaponFact("Cyber Enforcer (130 HP, Tactical)", "Humanoid infantry units equipped with automatic laser carbines. They strafe, take cover behind pillars, and coordinate fire.")
        WeaponFact("Titan Boss Mech IX (700 HP, Heavy Boss)", "Heavily armored quadruped war machine armed with dual shoulder missile pods. Fires twin rocket barrages. Target the central energy core with RPGs and Plasma Sniper fire.")
    }
}

@Composable
private fun KnowledgeArenas() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("COMBAT ARENAS", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
        WeaponFact("Neo-Tokyo Outpost", "Urban cyber district featuring an open central plaza surrounded by defense towers and corridors.")
        WeaponFact("Bio-Lab Facility", "Subterranean high-security research base with tight CQB corridors, ideal for close-range shotgun combat.")
        WeaponFact("Titan Colosseum", "Expansive battle arena with heavy cover barricades and sniper catwalks, optimized for boss encounters.")
    }
}

@Composable
private fun KnowledgeVersion() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("BUILD & ENGINE SPECIFICATIONS", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
        SpecRow("App Name", "Apex Trigger 3D")
        SpecRow("Game Version", "1.0.4 (Release Candidate)")
        SpecRow("Target Platform", "Android (API 24 to 36)")
        SpecRow("UI Framework", "Jetpack Compose (100% Kotlin)")
        SpecRow("Rendering Engine", "Custom 3D Raycasting with DDA & Z-Buffer")
        SpecRow("Frame Target", "60 FPS Native Software Rasterizer")
        SpecRow("Audio Synthesis", "Procedural 16-bit PCM (Android AudioTrack)")
        SpecRow("Haptic Engine", "VibrationEffect Pulse Modulation")
        SpecRow("Package Format", "Android APK (.apk)")
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun AnimatedCyberBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle perspective grid lines
        val horizonY = h * 0.45f
        for (i in 0..10) {
            val progress = i / 10f
            val y = horizonY + progress * progress * (h - horizonY)
            drawLine(
                color = Color(0x1500E5FF),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Ambient cyber glow circle
        drawCircle(
            color = Color(0x0A00E5FF),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, horizonY)
        )
    }
}

@Composable
private fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GunmetalSurface)
                .border(2.dp, CyberRed, RoundedCornerShape(18.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CyberRed,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "EXIT APEX TRIGGER?",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Are you sure you want to exit the game?",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirmExit,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_exit_button")
                ) {
                    Text("EXIT", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    viewModel: GameViewModel,
    uiState: com.example.engine.GameUiState,
    onDismiss: () -> Unit
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
                .clip(RoundedCornerShape(18.dp))
                .background(GunmetalSurface)
                .border(2.dp, CyberCyan, RoundedCornerShape(18.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMBAT SETTINGS",
                    color = CyberCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Switch(
                    checked = viewModel.soundSynth.isSoundEnabled,
                    onCheckedChange = { viewModel.soundSynth.isSoundEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0xFF0E3D4D))
                )
            }

            // Haptics Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration / Haptics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Switch(
                    checked = viewModel.soundSynth.isHapticsEnabled,
                    onCheckedChange = { viewModel.soundSynth.isHapticsEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0xFF0E3D4D))
                )
            }

            // Sensitivity Slider
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aim Sensitivity: ${(uiState.lookSensitivity * 10).toInt() / 10f}x",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = uiState.lookSensitivity,
                onValueChange = { viewModel.setSensitivity(it) },
                valueRange = 0.4f..2.5f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )

            // Invert Pitch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invert Vertical Look", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Switch(
                    checked = uiState.invertLookY,
                    onCheckedChange = { viewModel.toggleInvertLook() },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberOrange, checkedTrackColor = Color(0xFF4E2600))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE & CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
