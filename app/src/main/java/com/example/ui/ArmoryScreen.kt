package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameViewModel
import com.example.model.WeaponType
import com.example.model.WeaponUpgrade
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
fun ArmoryScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    var selectedWeaponType by remember { mutableStateOf(WeaponType.RIFLE) }
    val upg = viewModel.weaponUpgrades[selectedWeaponType] ?: WeaponUpgrade()

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
            // Header Row: Back button, Title, Credits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GunmetalSurface)
                        .border(1.dp, GunmetalBorder, CircleShape)
                        .testTag("armory_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberCyan
                    )
                }

                Text(
                    text = "CYBER ARMORY",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )

                // Credits Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x990A0F1D))
                        .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${uiState.credits} C", color = CyberYellow, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weapon Selector Horizontal Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (type in WeaponType.values()) {
                    val isSelected = selectedWeaponType == type
                    val isUnlocked = viewModel.weaponUpgrades[type]?.isUnlocked == true

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyberCyan else GunmetalSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberCyan else GunmetalBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedWeaponType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (type) {
                                    WeaponType.PISTOL -> "P-9"
                                    WeaponType.RIFLE -> "V-7"
                                    WeaponType.SHOTGUN -> "SG-12"
                                    WeaponType.SNIPER -> "REAPER"
                                    WeaponType.ROCKET -> "HAVOC"
                                },
                                color = if (isSelected) Color.Black else if (isUnlocked) Color.White else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Weapon Showcase Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GunmetalSurface)
                    .border(2.dp, if (upg.isUnlocked) CyberCyan else Color.DarkGray, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedWeaponType.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                    Text(
                        text = selectedWeaponType.category,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stylized 2D Weapon Graphic
                    WeaponGraphicPreview(selectedWeaponType)

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats bars
                    val currentDmg = upg.getDamage(selectedWeaponType.baseDamage)
                    val currentClip = upg.getClip(selectedWeaponType.baseClip)
                    val currentReload = upg.getReloadTime(selectedWeaponType.reloadTimeMs)

                    StatProgress(
                        label = "Damage Per Shot",
                        display = "${currentDmg.toInt()}",
                        progress = (currentDmg / 260f).coerceIn(0.1f, 1f),
                        barColor = CyberRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatProgress(
                        label = "Magazine Size",
                        display = "$currentClip rounds",
                        progress = (currentClip / 45f).coerceIn(0.1f, 1f),
                        barColor = AmmoGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatProgress(
                        label = "Reload Speed",
                        display = "${(currentReload / 100) / 10f}s",
                        progress = (1f - (currentReload / 3200f)).coerceIn(0.1f, 1f),
                        barColor = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatProgress(
                        label = "Fire Rate",
                        display = "${(60000L / selectedWeaponType.fireRateMs)} RPM",
                        progress = (1f - (selectedWeaponType.fireRateMs / 1500f)).coerceIn(0.1f, 1f),
                        barColor = CyberGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Unlock or Upgrade section
            if (!upg.isUnlocked) {
                // Unlock Action Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GunmetalCard)
                        .border(1.dp, CyberOrange, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WEAPON LOCKED",
                        color = CyberOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Unlock ${selectedWeaponType.displayName} for battle deployments.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.unlockWeapon(selectedWeaponType) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_${selectedWeaponType.id}")
                    ) {
                        Text(
                            text = "UNLOCK FOR ${selectedWeaponType.unlockCost} CREDITS",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Upgrade Tiers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GunmetalCard)
                        .border(1.dp, GunmetalBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PERFORMANCE ENHANCEMENTS",
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )

                    // Upgrade Damage
                    val dmgCost = 150 + (upg.damageLevel * 100)
                    UpgradeRow(
                        title = "Power Overcharge (+12% Damage)",
                        currentLevel = upg.damageLevel,
                        cost = dmgCost,
                        canAfford = uiState.credits >= dmgCost,
                        onUpgrade = { viewModel.upgradeWeaponDamage(selectedWeaponType) },
                        testTag = "upgrade_dmg"
                    )

                    // Upgrade Clip
                    val clipCost = 120 + (upg.clipLevel * 80)
                    UpgradeRow(
                        title = "Expanded Mag (+20% Capacity)",
                        currentLevel = upg.clipLevel,
                        cost = clipCost,
                        canAfford = uiState.credits >= clipCost,
                        onUpgrade = { viewModel.upgradeWeaponClip(selectedWeaponType) },
                        testTag = "upgrade_clip"
                    )

                    // Upgrade Reload
                    val reloadCost = 100 + (upg.reloadLevel * 80)
                    UpgradeRow(
                        title = "Quick Release Mag (-12% Reload)",
                        currentLevel = upg.reloadLevel,
                        cost = reloadCost,
                        canAfford = uiState.credits >= reloadCost,
                        onUpgrade = { viewModel.upgradeWeaponReload(selectedWeaponType) },
                        testTag = "upgrade_reload"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatProgress(
    label: String,
    display: String,
    progress: Float,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = display, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun UpgradeRow(
    title: String,
    currentLevel: Int,
    cost: Int,
    canAfford: Boolean,
    onUpgrade: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "Tier $currentLevel", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onUpgrade,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canAfford) CyberYellow else Color.DarkGray
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag(testTag)
        ) {
            Text(
                text = "$cost C",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WeaponGraphicPreview(type: WeaponType) {
    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090D18))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 220.dp, height = 80.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            when (type) {
                WeaponType.PISTOL -> {
                    // Sleek tactical handgun
                    drawRect(Color(0xFF334155), Offset(cx - 50f, cy - 15f), Size(100f, 25f))
                    drawRect(Color(0xFF1E293B), Offset(cx - 30f, cy + 10f), Size(26f, 32f))
                    drawLine(CyberCyan, Offset(cx - 45f, cy - 10f), Offset(cx + 45f, cy - 10f), 3f)
                }
                WeaponType.RIFLE -> {
                    // Futuristic assault rifle
                    drawRect(Color(0xFF334155), Offset(cx - 85f, cy - 12f), Size(170f, 22f))
                    drawRect(Color(0xFF1E293B), Offset(cx - 30f, cy + 10f), Size(28f, 35f))
                    drawRect(Color(0xFF0F172A), Offset(cx + 10f, cy + 10f), Size(22f, 30f)) // mag
                    drawRect(CyberCyan, Offset(cx - 20f, cy - 25f), Size(40f, 13f)) // holo sight
                    drawLine(CyberCyan, Offset(cx - 80f, cy), Offset(cx + 80f, cy), 3f)
                }
                WeaponType.SHOTGUN -> {
                    // Heavy combat shotgun
                    drawRect(Color(0xFF475569), Offset(cx - 80f, cy - 16f), Size(160f, 28f))
                    drawRect(CyberOrange, Offset(cx - 15f, cy + 12f), Size(55f, 18f)) // pump
                    drawRect(Color(0xFF1E293B), Offset(cx - 65f, cy + 12f), Size(30f, 25f)) // stock
                }
                WeaponType.SNIPER -> {
                    // Railgun sniper
                    drawRect(Color(0xFF1E293B), Offset(cx - 95f, cy - 8f), Size(190f, 16f))
                    drawRect(CyberCyan, Offset(cx - 30f, cy - 22f), Size(65f, 14f)) // scope
                    for (i in 0 until 4) {
                        drawCircle(CyberCyan, 5f, Offset(cx - 60f + i * 25f, cy))
                    }
                }
                WeaponType.ROCKET -> {
                    // Quad rocket launcher
                    drawRect(Color(0xFF334155), Offset(cx - 75f, cy - 22f), Size(150f, 44f))
                    drawCircle(CyberOrange, 14f, Offset(cx + 80f, cy)) // missile tip
                    drawLine(CyberYellow, Offset(cx - 60f, cy + 15f), Offset(cx + 60f, cy + 15f), 6f)
                }
            }
        }
    }
}
