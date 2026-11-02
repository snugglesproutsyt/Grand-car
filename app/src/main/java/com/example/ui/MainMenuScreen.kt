package com.example.ui

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onStartGame: () -> Unit,
    onNavigateArmory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMode by remember { mutableStateOf(GameMode.WAVE_DEFENSE) }
    var selectedArena by remember { mutableStateOf(ArenaId.NEO_TOKYO) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Credits & Settings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Credits Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x990A0F1D))
                        .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CREDITS: ", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.credits} C", color = CyberYellow, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }

                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GunmetalSurface)
                        .border(1.dp, GunmetalBorder, CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Logo / Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F1E36), Color(0xFF0A0F1D))
                        )
                    )
                    .border(2.dp, CyberCyan, RoundedCornerShape(20.dp))
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "APEX TRIGGER",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "TACTICAL 3D CYBER OPS",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Choose Game Mode
            Text(
                text = "SELECT MISSION TYPE",
                color = CyberCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            for (mode in GameMode.values()) {
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFF132238) else GunmetalSurface)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CyberCyan else GunmetalBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedMode = mode }
                        .padding(14.dp)
                        .testTag("mode_${mode.name}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyberCyan else Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mode.description,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: Choose Arena Map
            Text(
                text = "SELECT COMBAT ZONE",
                color = CyberCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (arena in ArenaId.values()) {
                    val isSelected = selectedArena == arena
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF132238) else GunmetalSurface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyberOrange else GunmetalBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedArena = arena }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                            .testTag("arena_${arena.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = arena.displayName,
                            color = if (isSelected) CyberOrange else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Deploy Button
            Button(
                onClick = {
                    viewModel.startGame(selectedMode, selectedArena)
                    onStartGame()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("deploy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEPLOY TO BATTLE",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Armory & Weapons Shop Button
            Button(
                onClick = onNavigateArmory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GunmetalCard
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("armory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = CyberOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ARMORY & UPGRADES",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Settings Dialog Modal
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = viewModel,
                uiState = uiState,
                onDismiss = { showSettingsDialog = false }
            )
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
            Text(
                text = "COMBAT SETTINGS",
                color = CyberCyan,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Bold)
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
                Text("Vibration / Haptics", color = Color.White, fontWeight = FontWeight.Bold)
                Switch(
                    checked = viewModel.soundSynth.isHapticsEnabled,
                    onCheckedChange = { viewModel.soundSynth.isHapticsEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0xFF0E3D4D))
                )
            }

            // Sensitivity Slider
            Spacer(modifier = Modifier.height(10.dp))
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
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )

            // Invert Pitch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invert Vertical Look", color = Color.White, fontWeight = FontWeight.Bold)
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
