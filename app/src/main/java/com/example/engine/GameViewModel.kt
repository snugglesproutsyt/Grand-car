package com.example.engine

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynth
import com.example.model.ArenaId
import com.example.model.ArenaMap
import com.example.model.EnemyBot
import com.example.model.EnemyState
import com.example.model.EnemyType
import com.example.model.FloatingCombatText
import com.example.model.GameMode
import com.example.model.MapRegistry
import com.example.model.Particle
import com.example.model.Pickup
import com.example.model.PickupType
import com.example.model.Player
import com.example.model.Projectile
import com.example.model.ProjectileType
import com.example.model.WeaponState
import com.example.model.WeaponType
import com.example.model.WeaponUpgrade
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameStateStatus {
    MENU,
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER
}

data class KillNotification(
    val id: Long,
    val text: String,
    val isHeadshot: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameUiState(
    val status: GameStateStatus = GameStateStatus.MENU,
    val gameMode: GameMode = GameMode.WAVE_DEFENSE,
    val arenaId: ArenaId = ArenaId.NEO_TOKYO,
    val player: Player = Player(),
    val currentWave: Int = 1,
    val totalWaves: Int = 10,
    val waveIntermissionSeconds: Int = 0,
    val waveEnemiesLeft: Int = 0,
    val deathmatchTargetKills: Int = 25,
    val botKills: Int = 0,
    val shotsFired: Int = 0,
    val shotsHit: Int = 0,
    val killFeed: List<KillNotification> = emptyList(),
    val crosshairHitmarker: Boolean = false,
    val credits: Int = 250,
    val totalScore: Int = 0,
    val lookSensitivity: Float = 1.0f,
    val invertLookY: Boolean = false,
    val isFiring: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("apex_trigger_prefs", Context.MODE_PRIVATE)
    val soundSynth = SoundSynth(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Upgrades dictionary
    val weaponUpgrades = mutableMapOf<WeaponType, WeaponUpgrade>()

    // Current Weapons inventory
    val availableWeapons = listOf(
        WeaponType.PISTOL,
        WeaponType.RIFLE,
        WeaponType.SHOTGUN,
        WeaponType.SNIPER,
        WeaponType.ROCKET
    )
    val weaponStates = mutableMapOf<WeaponType, WeaponState>()

    var activeMap: ArenaMap = MapRegistry.neoTokyo
        private set

    val enemies = mutableListOf<EnemyBot>()
    val pickups = mutableListOf<Pickup>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()
    val combatTexts = mutableListOf<FloatingCombatText>()

    private var gameLoopJob: Job? = null
    private var lastTickTime: Long = System.currentTimeMillis()
    private var entityIdCounter = 1
    private var hitmarkerTimer = 0f

    init {
        loadUpgrades()
        initWeapons()
    }

    private fun loadUpgrades() {
        val savedCredits = prefs.getInt("player_credits", 350)
        _uiState.update { it.copy(credits = savedCredits) }

        for (type in WeaponType.values()) {
            val dmgLvl = prefs.getInt("${type.id}_dmg", 0)
            val clipLvl = prefs.getInt("${type.id}_clip", 0)
            val reloadLvl = prefs.getInt("${type.id}_reload", 0)
            val unlocked = prefs.getBoolean("${type.id}_unlocked", type == WeaponType.PISTOL || type == WeaponType.RIFLE)
            weaponUpgrades[type] = WeaponUpgrade(
                damageLevel = dmgLvl,
                clipLevel = clipLvl,
                reloadLevel = reloadLvl,
                isUnlocked = unlocked
            )
        }
    }

    private fun saveUpgrades() {
        val editor = prefs.edit()
        editor.putInt("player_credits", _uiState.value.credits)
        for ((type, upg) in weaponUpgrades) {
            editor.putInt("${type.id}_dmg", upg.damageLevel)
            editor.putInt("${type.id}_clip", upg.clipLevel)
            editor.putInt("${type.id}_reload", upg.reloadLevel)
            editor.putBoolean("${type.id}_unlocked", upg.isUnlocked)
        }
        editor.apply()
    }

    private fun initWeapons() {
        for (type in WeaponType.values()) {
            val upg = weaponUpgrades[type] ?: WeaponUpgrade()
            val clip = upg.getClip(type.baseClip)
            weaponStates[type] = WeaponState(
                type = type,
                currentClip = clip,
                reserveAmmo = type.baseReserve
            )
        }
    }

    fun getActiveWeapon(): WeaponState {
        val selected = availableWeapons[_uiState.value.player.selectedWeaponIndex]
        return weaponStates[selected] ?: weaponStates[WeaponType.RIFLE]!!
    }

    fun startGame(mode: GameMode, arenaId: ArenaId) {
        activeMap = MapRegistry.getArena(arenaId)
        enemies.clear()
        pickups.clear()
        projectiles.clear()
        particles.clear()
        combatTexts.clear()

        initWeapons()

        val newPlayer = Player(
            x = activeMap.playerSpawnX,
            y = activeMap.playerSpawnY,
            angle = 0f,
            pitch = 0f,
            health = 100f,
            shield = 100f,
            selectedWeaponIndex = 1 // Start with Rifle
        )

        _uiState.update {
            it.copy(
                status = GameStateStatus.PLAYING,
                gameMode = mode,
                arenaId = arenaId,
                player = newPlayer,
                currentWave = 1,
                waveIntermissionSeconds = 0,
                botKills = 0,
                shotsFired = 0,
                shotsHit = 0,
                killFeed = emptyList(),
                crosshairHitmarker = false
            )
        }

        // Spawn initial pickups
        for ((idx, spawn) in activeMap.pickupSpawns.withIndex()) {
            val type = PickupType.values()[idx % PickupType.values().size]
            pickups.add(Pickup(id = entityIdCounter++, type = type, x = spawn.first, y = spawn.second))
        }

        if (mode == GameMode.WAVE_DEFENSE) {
            spawnWave(1)
        } else if (mode == GameMode.DEATHMATCH) {
            spawnDeathmatchBots(6)
        } else {
            // Firing range target dummies
            for (spawn in activeMap.enemySpawns.take(4)) {
                enemies.add(
                    EnemyBot(
                        id = entityIdCounter++,
                        type = EnemyType.CYBER_ENFORCER,
                        x = spawn.first,
                        y = spawn.second,
                        health = 250f
                    )
                )
            }
        }

        startGameLoop()
    }

    private fun spawnWave(wave: Int) {
        val enemyCount = 4 + wave * 2
        _uiState.update { it.copy(currentWave = wave, waveEnemiesLeft = enemyCount) }

        val isBossWave = (wave % 5 == 0)
        var spawned = 0

        for (spawn in activeMap.enemySpawns) {
            if (spawned >= enemyCount) break
            val type = if (isBossWave && spawned == 0) {
                EnemyType.TITAN_BOSS
            } else if (spawned % 2 == 0) {
                EnemyType.SCOUT_DRONE
            } else {
                EnemyType.CYBER_ENFORCER
            }
            enemies.add(
                EnemyBot(
                    id = entityIdCounter++,
                    type = type,
                    x = spawn.first + Random.nextFloat() * 0.4f - 0.2f,
                    y = spawn.second + Random.nextFloat() * 0.4f - 0.2f,
                    health = type.maxHealth
                )
            )
            spawned++
        }
    }

    private fun spawnDeathmatchBots(count: Int) {
        val spawns = activeMap.enemySpawns
        for (i in 0 until count) {
            val spawn = spawns[i % spawns.size]
            val type = if (i % 3 == 0) EnemyType.SCOUT_DRONE else EnemyType.CYBER_ENFORCER
            enemies.add(
                EnemyBot(
                    id = entityIdCounter++,
                    type = type,
                    x = spawn.first,
                    y = spawn.second
                )
            )
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lastTickTime = System.currentTimeMillis()

        gameLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.status == GameStateStatus.PLAYING) {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTickTime) / 1000f).coerceIn(0.001f, 0.05f)
                lastTickTime = now

                updateGame(dt)
                delay(16) // ~60 FPS update
            }
        }
    }

    fun pauseGame() {
        _uiState.update { it.copy(status = GameStateStatus.PAUSED) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(status = GameStateStatus.PLAYING) }
        startGameLoop()
    }

    fun returnToMenu() {
        gameLoopJob?.cancel()
        saveUpgrades()
        _uiState.update { it.copy(status = GameStateStatus.MENU) }
    }

    // Touch controls input: Movement from Virtual Joystick
    fun updateMovement(forward: Float, strafe: Float) {
        val player = _uiState.value.player
        if (_uiState.value.status != GameStateStatus.PLAYING) return

        val speedMultiplier = when {
            player.isSprinting && player.sprintEnergy > 0.1f -> 4.2f
            player.isCrouching -> 1.5f
            player.isADS -> 1.8f
            else -> 2.8f
        }

        val dirX = player.dirX
        val dirY = player.dirY
        // Strafe vector perpendicular to forward direction
        val strafeX = -dirY
        val strafeY = dirX

        val moveX = (dirX * forward + strafeX * strafe) * speedMultiplier
        val moveY = (dirY * forward + strafeY * strafe) * speedMultiplier

        val radius = 0.32f
        val newX = player.x + moveX * 0.016f
        val newY = player.y + moveY * 0.016f

        // Wall collisions with sliding
        if (!activeMap.isWall(newX + if (moveX > 0) radius else -radius, player.y)) {
            player.x = newX
        }
        if (!activeMap.isWall(player.x, newY + if (moveY > 0) radius else -radius)) {
            player.y = newY
        }

        // Head bobbing animation during movement
        val isMoving = abs(forward) > 0.1f || abs(strafe) > 0.1f
        if (isMoving) {
            player.headBob += 0.28f
        } else {
            player.headBob *= 0.85f
        }
    }

    // Touch controls input: Aim rotation and look pitch
    fun updateLook(deltaAngle: Float, deltaPitch: Float) {
        val player = _uiState.value.player
        val sens = _uiState.value.lookSensitivity * 0.0055f
        val invert = if (_uiState.value.invertLookY) -1f else 1f

        player.angle = (player.angle + deltaAngle * sens) % (2.0f * Math.PI.toFloat())
        if (player.angle < 0) player.angle += (2.0f * Math.PI.toFloat())

        player.pitch = (player.pitch - deltaPitch * sens * invert).coerceIn(-0.48f, 0.48f)
    }

    fun setFiring(firing: Boolean) {
        _uiState.update { it.copy(isFiring = firing) }
        if (firing) {
            tryFire()
        }
    }

    fun toggleADS() {
        val player = _uiState.value.player
        player.isADS = !player.isADS
    }

    fun toggleSprint() {
        val player = _uiState.value.player
        player.isSprinting = !player.isSprinting
    }

    fun toggleCrouch() {
        val player = _uiState.value.player
        player.isCrouching = !player.isCrouching
    }

    fun selectWeapon(index: Int) {
        if (index !in availableWeapons.indices) return
        val type = availableWeapons[index]
        val upg = weaponUpgrades[type] ?: return
        if (!upg.isUnlocked) return

        val player = _uiState.value.player
        player.selectedWeaponIndex = index
        soundSynth.playReload()
    }

    fun triggerReload() {
        val weapon = getActiveWeapon()
        val upg = weaponUpgrades[weapon.type] ?: WeaponUpgrade()
        val maxClip = upg.getClip(weapon.type.baseClip)

        if (weapon.currentClip < maxClip && weapon.reserveAmmo > 0 && !weapon.isReloading) {
            weapon.isReloading = true
            weapon.reloadProgress = 0f
            soundSynth.playReload()
        }
    }

    private fun tryFire() {
        val weapon = getActiveWeapon()
        val now = System.currentTimeMillis()
        if (weapon.isReloading) return
        if (now - weapon.lastFiredTime < weapon.type.fireRateMs) return

        if (weapon.currentClip <= 0) {
            soundSynth.playEmptyClick()
            triggerReload()
            return
        }

        // Fire weapon!
        weapon.currentClip--
        weapon.lastFiredTime = now
        weapon.recoilAnim = 1.0f

        val player = _uiState.value.player
        // Recoil screen kick
        player.pitch = (player.pitch + weapon.type.recoilKickY * 0.002f).coerceIn(-0.48f, 0.48f)
        player.screenShake = 0.4f
        player.muzzleLight = 1.0f

        _uiState.update { it.copy(shotsFired = it.shotsFired + 1) }

        when (weapon.type) {
            WeaponType.PISTOL -> {
                soundSynth.playPistol()
                fireHitscan(weapon, 1)
            }
            WeaponType.RIFLE -> {
                soundSynth.playRifle()
                fireHitscan(weapon, 1)
            }
            WeaponType.SHOTGUN -> {
                soundSynth.playShotgun()
                fireHitscan(weapon, weapon.type.pellets)
            }
            WeaponType.SNIPER -> {
                soundSynth.playSniper()
                fireHitscan(weapon, 1)
            }
            WeaponType.ROCKET -> {
                soundSynth.playRocketLaunch()
                fireRocket(weapon)
            }
        }
    }

    private fun fireHitscan(weapon: WeaponState, pelletCount: Int) {
        val player = _uiState.value.player
        val upg = weaponUpgrades[weapon.type] ?: WeaponUpgrade()
        val baseDmg = upg.getDamage(weapon.type.baseDamage)
        val damageMultiplier = if (player.damageBoostTimer > 0f) 2.0f else 1.0f

        var hitAny = false

        for (i in 0 until pelletCount) {
            val spreadX = (Random.nextFloat() * 2f - 1f) * (if (player.isADS) weapon.type.spreadAngle * 0.35f else weapon.type.spreadAngle)
            val rayAngle = player.angle + spreadX
            val rayDirX = cos(rayAngle)
            val rayDirY = sin(rayAngle)

            // Find closest enemy along ray
            var closestEnemy: EnemyBot? = null
            var closestDist = 18f

            for (enemy in enemies) {
                if (enemy.isDead) continue
                val toEnemyX = enemy.x - player.x
                val toEnemyY = enemy.y - player.y
                val dist = sqrt(toEnemyX * toEnemyX + toEnemyY * toEnemyY)
                if (dist > closestDist) continue

                // Check dot product to see if facing enemy
                val dot = (toEnemyX * rayDirX + toEnemyY * rayDirY) / dist
                if (dot > 0.94f) { // Within crosshair cone
                    // Check if wall obscures line of sight
                    if (hasLineOfSight(player.x, player.y, enemy.x, enemy.y)) {
                        closestDist = dist
                        closestEnemy = enemy
                    }
                }
            }

            if (closestEnemy != null) {
                hitAny = true
                val isHeadshot = (player.pitch > 0.08f) || (weapon.type == WeaponType.SNIPER && Random.nextFloat() < 0.45f)
                val finalDmg = baseDmg * damageMultiplier * (if (isHeadshot) weapon.type.headshotMultiplier else 1.0f)

                applyDamageToEnemy(closestEnemy, finalDmg, isHeadshot)
            }
        }

        if (hitAny) {
            _uiState.update { it.copy(shotsHit = it.shotsHit + 1, crosshairHitmarker = true) }
            hitmarkerTimer = 0.22f
            soundSynth.playHitMarker()
        }
    }

    private fun fireRocket(weapon: WeaponState) {
        val player = _uiState.value.player
        val upg = weaponUpgrades[weapon.type] ?: WeaponUpgrade()
        val dmg = upg.getDamage(weapon.type.baseDamage)

        projectiles.add(
            Projectile(
                id = entityIdCounter++,
                x = player.x + player.dirX * 0.4f,
                y = player.y + player.dirY * 0.4f,
                dx = player.dirX,
                dy = player.dirY,
                speed = 8.5f,
                damage = dmg,
                isPlayer = true,
                type = ProjectileType.ROCKET,
                explosionRadius = 3.2f
            )
        )
    }

    private fun hasLineOfSight(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val dx = x1 - x0
        val dy = y1 - y0
        val dist = sqrt(dx * dx + dy * dy)
        val steps = (dist * 4).toInt().coerceAtLeast(1)
        val stepX = dx / steps
        val stepY = dy / steps

        var curX = x0
        var curY = y0
        for (i in 0 until steps) {
            curX += stepX
            curY += stepY
            if (activeMap.isWall(curX, curY)) return false
        }
        return true
    }

    private fun applyDamageToEnemy(enemy: EnemyBot, damage: Float, isHeadshot: Boolean) {
        enemy.health -= damage
        enemy.hitFlashTimer = 0.15f
        enemy.state = EnemyState.CHASE

        // Spawn 3D floating damage number
        combatTexts.add(
            FloatingCombatText(
                id = entityIdCounter++,
                x = enemy.x,
                y = enemy.y,
                z = if (isHeadshot) 1.2f else 0.8f,
                text = if (isHeadshot) "HEADSHOT! -${damage.toInt()}" else "-${damage.toInt()}",
                color = if (isHeadshot) Color(0xFFFFD600) else Color(0xFFFF5252),
                isHeadshot = isHeadshot
            )
        )

        // Spawn spark particles
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x = enemy.x,
                    y = enemy.y,
                    z = 0.8f,
                    vx = (Random.nextFloat() - 0.5f) * 3f,
                    vy = (Random.nextFloat() - 0.5f) * 3f,
                    vz = Random.nextFloat() * 2f,
                    color = if (isHeadshot) Color(0xFFFFD600) else Color(0xFFFF1744),
                    size = 4f,
                    life = 0.4f,
                    maxLife = 0.4f
                )
            )
        }

        if (enemy.isDead && enemy.deathTimer == 0f) {
            handleEnemyKill(enemy, isHeadshot)
        }
    }

    private fun handleEnemyKill(enemy: EnemyBot, isHeadshot: Boolean) {
        enemy.state = EnemyState.DEAD
        enemy.deathTimer = 1.0f

        val player = _uiState.value.player
        player.kills++
        if (isHeadshot) player.headshots++

        val earnedScore = enemy.type.scoreValue * (if (isHeadshot) 2 else 1)
        val earnedCredits = enemy.type.creditValue

        player.score += earnedScore
        player.combo++
        player.comboTimer = 4.0f

        _uiState.update {
            it.copy(
                credits = it.credits + earnedCredits,
                totalScore = it.totalScore + earnedScore,
                botKills = it.botKills + 1
            )
        }

        soundSynth.playKill()

        // Big explosion particles
        for (i in 0 until 18) {
            particles.add(
                Particle(
                    x = enemy.x,
                    y = enemy.y,
                    z = 0.6f,
                    vx = (Random.nextFloat() - 0.5f) * 4.5f,
                    vy = (Random.nextFloat() - 0.5f) * 4.5f,
                    vz = Random.nextFloat() * 3.5f,
                    color = if (enemy.type == EnemyType.TITAN_BOSS) Color(0xFFFF5722) else Color(0xFF00E5FF),
                    size = 6f,
                    life = 0.6f,
                    maxLife = 0.6f
                )
            )
        }

        // Add to kill feed
        val killMsg = if (isHeadshot) "Eliminated ${enemy.type.displayName} (HEADSHOT +$earnedCredits C)" else "Eliminated ${enemy.type.displayName} (+$earnedCredits C)"
        _uiState.update {
            val list = it.killFeed.takeLast(3) + KillNotification(System.currentTimeMillis(), killMsg, isHeadshot)
            it.copy(killFeed = list)
        }

        // Random pickup drop (30% chance)
        if (Random.nextFloat() < 0.35f) {
            val dropType = PickupType.values().random()
            pickups.add(Pickup(id = entityIdCounter++, type = dropType, x = enemy.x, y = enemy.y))
        }

        // Mode specific win checks
        if (_uiState.value.gameMode == GameMode.WAVE_DEFENSE) {
            val remaining = enemies.count { !it.isDead && it.id != enemy.id }
            _uiState.update { it.copy(waveEnemiesLeft = remaining) }
            if (remaining <= 0) {
                // Wave cleared!
                handleWaveComplete()
            }
        } else if (_uiState.value.gameMode == GameMode.DEATHMATCH) {
            if (_uiState.value.botKills >= _uiState.value.deathmatchTargetKills) {
                _uiState.update { it.copy(status = GameStateStatus.VICTORY) }
                saveUpgrades()
            } else {
                // Respawn bot after delay
                viewModelScope.launch {
                    delay(2500)
                    if (_uiState.value.status == GameStateStatus.PLAYING) {
                        val spawn = activeMap.enemySpawns.random()
                        enemies.add(
                            EnemyBot(
                                id = entityIdCounter++,
                                type = if (Random.nextBoolean()) EnemyType.SCOUT_DRONE else EnemyType.CYBER_ENFORCER,
                                x = spawn.first,
                                y = spawn.second
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleWaveComplete() {
        val nextWave = _uiState.value.currentWave + 1
        if (nextWave > _uiState.value.totalWaves) {
            _uiState.update { it.copy(status = GameStateStatus.VICTORY) }
            saveUpgrades()
            return
        }

        viewModelScope.launch {
            for (sec in 4 downTo 1) {
                _uiState.update { it.copy(waveIntermissionSeconds = sec) }
                delay(1000)
            }
            _uiState.update { it.copy(waveIntermissionSeconds = 0) }
            spawnWave(nextWave)
        }
    }

    private fun updateGame(dt: Float) {
        val player = _uiState.value.player

        // Continuous automatic weapon firing if fire button held
        if (_uiState.value.isFiring && getActiveWeapon().type.isAutomatic) {
            tryFire()
        }

        // Update active weapon reload
        val weapon = getActiveWeapon()
        val upg = weaponUpgrades[weapon.type] ?: WeaponUpgrade()
        if (weapon.isReloading) {
            val reloadDuration = upg.getReloadTime(weapon.type.reloadTimeMs)
            weapon.reloadProgress += (dt * 1000f) / reloadDuration
            if (weapon.reloadProgress >= 1.0f) {
                weapon.isReloading = false
                weapon.reloadProgress = 0f
                val maxClip = upg.getClip(weapon.type.baseClip)
                val needed = maxClip - weapon.currentClip
                val toAdd = min(needed, weapon.reserveAmmo)
                weapon.currentClip += toAdd
                weapon.reserveAmmo -= toAdd
            }
        }

        // Weapon recoil decay
        if (weapon.recoilAnim > 0f) {
            weapon.recoilAnim = (weapon.recoilAnim - dt * 4.5f).coerceAtLeast(0f)
        }

        // Crosshair hitmarker decay
        if (hitmarkerTimer > 0f) {
            hitmarkerTimer -= dt
            if (hitmarkerTimer <= 0f) {
                _uiState.update { it.copy(crosshairHitmarker = false) }
            }
        }

        // Sprint Energy regen
        if (player.isSprinting) {
            player.sprintEnergy = (player.sprintEnergy - dt * 0.25f).coerceAtLeast(0f)
            if (player.sprintEnergy <= 0f) player.isSprinting = false
        } else {
            player.sprintEnergy = (player.sprintEnergy + dt * 0.18f).coerceAtMost(1.0f)
        }

        // Player buffs decay
        if (player.damageBoostTimer > 0f) player.damageBoostTimer -= dt
        if (player.speedStimTimer > 0f) player.speedStimTimer -= dt
        if (player.hurtFlashAlpha > 0f) player.hurtFlashAlpha = (player.hurtFlashAlpha - dt * 2f).coerceAtLeast(0f)
        if (player.shieldFlashAlpha > 0f) player.shieldFlashAlpha = (player.shieldFlashAlpha - dt * 2f).coerceAtLeast(0f)
        if (player.screenShake > 0f) player.screenShake = (player.screenShake - dt * 3f).coerceAtLeast(0f)
        if (player.muzzleLight > 0f) player.muzzleLight = (player.muzzleLight - dt * 5.5f).coerceAtLeast(0f)
        if (player.hitIndicatorAlpha > 0f) player.hitIndicatorAlpha = (player.hitIndicatorAlpha - dt * 1.8f).coerceAtLeast(0f)

        // Combo timer decay
        if (player.comboTimer > 0f) {
            player.comboTimer -= dt
            if (player.comboTimer <= 0f) player.combo = 0
        }

        // Update Projectiles
        val projIter = projectiles.iterator()
        while (projIter.hasNext()) {
            val p = projIter.next()
            p.lifetime -= dt
            val nextX = p.x + p.dx * p.speed * dt
            val nextY = p.y + p.dy * p.speed * dt

            // Check wall collision
            if (activeMap.isWall(nextX, nextY) || p.lifetime <= 0f) {
                explodeProjectile(p)
                projIter.remove()
                continue
            }

            // Check enemy collision if player projectile
            if (p.isPlayer) {
                var collided = false
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    val dist = sqrt((enemy.x - nextX) * (enemy.x - nextX) + (enemy.y - nextY) * (enemy.y - nextY))
                    if (dist < enemy.type.radius) {
                        explodeProjectile(p)
                        applyDamageToEnemy(enemy, p.damage, false)
                        collided = true
                        break
                    }
                }
                if (collided) {
                    projIter.remove()
                    continue
                }
            } else {
                // Enemy projectile hitting player
                val distToPlayer = sqrt((player.x - nextX) * (player.x - nextX) + (player.y - nextY) * (player.y - nextY))
                if (distToPlayer < 0.45f) {
                    damagePlayer(p.damage)
                    projIter.remove()
                    continue
                }
            }

            p.x = nextX
            p.y = nextY
        }

        // Update Enemies AI
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            if (enemy.hitFlashTimer > 0f) enemy.hitFlashTimer -= dt

            if (enemy.isDead) {
                enemy.deathTimer -= dt
                if (enemy.deathTimer <= 0f) {
                    enemyIter.remove()
                }
                continue
            }

            updateEnemyAi(enemy, player, dt)
        }

        // Update Pickups
        val pickupIter = pickups.iterator()
        while (pickupIter.hasNext()) {
            val pickup = pickupIter.next()
            pickup.bobPhase += dt * 3.5f

            // Check distance to player
            val dx = pickup.x - player.x
            val dy = pickup.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 0.65f) {
                applyPickup(pickup.type, player)
                soundSynth.playHitMarker()
                pickupIter.remove()
            }
        }

        // Update Particles
        val partIter = particles.iterator()
        while (partIter.hasNext()) {
            val p = partIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                partIter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vz -= 9.8f * dt // gravity
        }

        // Update Combat Texts
        val textIter = combatTexts.iterator()
        while (textIter.hasNext()) {
            val t = textIter.next()
            t.life -= dt * 1.5f
            t.z += dt * 0.8f
            if (t.life <= 0f) {
                textIter.remove()
            }
        }
    }

    private fun updateEnemyAi(enemy: EnemyBot, player: Player, dt: Float) {
        val dx = player.x - enemy.x
        val dy = player.y - enemy.y
        val dist = sqrt(dx * dx + dy * dy)

        // Look at player
        enemy.angle = atan2(dy, dx)
        val hasLOS = hasLineOfSight(enemy.x, enemy.y, player.x, player.y)

        if (hasLOS && dist < 14f) {
            enemy.state = EnemyState.ATTACK
        } else if (dist < 18f) {
            enemy.state = EnemyState.CHASE
        } else {
            enemy.state = EnemyState.PATROL
        }

        // Movement towards player or strafing
        val idealDist = when (enemy.type) {
            EnemyType.TITAN_BOSS -> 3.5f
            EnemyType.SCOUT_DRONE -> 2.5f
            EnemyType.CYBER_ENFORCER -> 4.0f
        }

        var moveSpeed = enemy.type.speed
        if (enemy.state == EnemyState.CHASE || (enemy.state == EnemyState.ATTACK && dist > idealDist)) {
            val step = moveSpeed * dt
            val targetX = enemy.x + cos(enemy.angle) * step
            val targetY = enemy.y + sin(enemy.angle) * step

            if (!activeMap.isWall(targetX, enemy.y)) enemy.x = targetX
            if (!activeMap.isWall(enemy.x, targetY)) enemy.y = targetY
            enemy.walkCycle += dt * 8f
        } else if (enemy.state == EnemyState.ATTACK && dist < idealDist * 0.7f) {
            // Back up slightly if player gets too close
            val step = moveSpeed * 0.6f * dt
            val targetX = enemy.x - cos(enemy.angle) * step
            val targetY = enemy.y - sin(enemy.angle) * step

            if (!activeMap.isWall(targetX, enemy.y)) enemy.x = targetX
            if (!activeMap.isWall(enemy.x, targetY)) enemy.y = targetY
            enemy.walkCycle += dt * 6f
        }

        // Bot Firing
        val now = System.currentTimeMillis()
        if (hasLOS && dist < 12f && now - enemy.lastAttackTime > enemy.type.fireRateMs) {
            enemy.lastAttackTime = now

            when (enemy.type) {
                EnemyType.SCOUT_DRONE -> {
                    // Rapid plasma burst
                    projectiles.add(
                        Projectile(
                            id = entityIdCounter++,
                            x = enemy.x,
                            y = enemy.y,
                            dx = cos(enemy.angle),
                            dy = sin(enemy.angle),
                            speed = 6.5f,
                            damage = enemy.type.damage,
                            isPlayer = false,
                            type = ProjectileType.PLASMA
                        )
                    )
                }
                EnemyType.CYBER_ENFORCER -> {
                    // Assault rifle fire (hitscan with spread or laser)
                    projectiles.add(
                        Projectile(
                            id = entityIdCounter++,
                            x = enemy.x,
                            y = enemy.y,
                            dx = cos(enemy.angle + (Random.nextFloat() - 0.5f) * 0.08f),
                            dy = sin(enemy.angle + (Random.nextFloat() - 0.5f) * 0.08f),
                            speed = 9.0f,
                            damage = enemy.type.damage,
                            isPlayer = false,
                            type = ProjectileType.LASER_BEAM
                        )
                    )
                }
                EnemyType.TITAN_BOSS -> {
                    // Heavy rocket barrage!
                    soundSynth.playExplosion()
                    for (offset in listOf(-0.15f, 0.15f)) {
                        projectiles.add(
                            Projectile(
                                id = entityIdCounter++,
                                x = enemy.x,
                                y = enemy.y,
                                dx = cos(enemy.angle + offset),
                                dy = sin(enemy.angle + offset),
                                speed = 5.5f,
                                damage = enemy.type.damage,
                                isPlayer = false,
                                type = ProjectileType.ROCKET,
                                explosionRadius = 2.4f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun explodeProjectile(p: Projectile) {
        soundSynth.playExplosion()
        val player = _uiState.value.player
        player.screenShake = 0.55f

        // Blast particles
        for (i in 0 until 16) {
            particles.add(
                Particle(
                    x = p.x,
                    y = p.y,
                    z = 0.5f,
                    vx = (Random.nextFloat() - 0.5f) * 6f,
                    vy = (Random.nextFloat() - 0.5f) * 6f,
                    vz = Random.nextFloat() * 4f,
                    color = Color(0xFFFF5722),
                    size = 5f,
                    life = 0.5f,
                    maxLife = 0.5f
                )
            )
        }

        // Area of effect damage
        if (p.explosionRadius > 0f) {
            for (enemy in enemies) {
                if (enemy.isDead) continue
                val dist = sqrt((enemy.x - p.x) * (enemy.x - p.x) + (enemy.y - p.y) * (enemy.y - p.y))
                if (dist <= p.explosionRadius) {
                    val falloff = 1f - (dist / p.explosionRadius)
                    applyDamageToEnemy(enemy, p.damage * falloff, false)
                }
            }
            // Can damage player if close
            val distToPlayer = sqrt((player.x - p.x) * (player.x - p.x) + (player.y - p.y) * (player.y - p.y))
            if (distToPlayer <= p.explosionRadius) {
                damagePlayer(p.damage * 0.4f * (1f - distToPlayer / p.explosionRadius))
            }
        }
    }

    private fun damagePlayer(amount: Float) {
        val player = _uiState.value.player
        if (player.health <= 0f) return

        soundSynth.playPlayerHurt()
        player.screenShake = 0.65f
        player.hitIndicatorAlpha = 1.0f

        // Shield absorbs first
        if (player.shield > 0f) {
            player.shieldFlashAlpha = 0.8f
            val absorbed = min(player.shield, amount)
            player.shield -= absorbed
            val remainder = amount - absorbed
            if (remainder > 0f) {
                player.health = (player.health - remainder).coerceAtLeast(0f)
                player.hurtFlashAlpha = 0.85f
            }
        } else {
            player.health = (player.health - amount).coerceAtLeast(0f)
            player.hurtFlashAlpha = 0.85f
        }

        if (player.health <= 0f) {
            // Game Over
            _uiState.update { it.copy(status = GameStateStatus.GAME_OVER) }
            saveUpgrades()
        }
    }

    private fun applyPickup(type: PickupType, player: Player) {
        when (type) {
            PickupType.MEDKIT -> player.health = (player.health + 50f).coerceAtMost(player.maxHealth)
            PickupType.SHIELD -> player.shield = (player.shield + 50f).coerceAtMost(player.maxShield)
            PickupType.AMMO -> {
                for (w in weaponStates.values) {
                    w.reserveAmmo = w.type.baseReserve
                }
            }
            PickupType.DAMAGE_BOOST -> player.damageBoostTimer = 10f
            PickupType.SPEED_STIM -> player.speedStimTimer = 8f
        }
    }

    // Armory Purchase & Upgrades
    fun unlockWeapon(type: WeaponType): Boolean {
        val currentCredits = _uiState.value.credits
        if (currentCredits < type.unlockCost) return false

        _uiState.update { it.copy(credits = it.credits - type.unlockCost) }
        val upg = weaponUpgrades[type] ?: WeaponUpgrade()
        weaponUpgrades[type] = upg.copy(isUnlocked = true)
        saveUpgrades()
        soundSynth.playReload()
        return true
    }

    fun upgradeWeaponDamage(type: WeaponType): Boolean {
        val cost = 150 + ((weaponUpgrades[type]?.damageLevel ?: 0) * 100)
        if (_uiState.value.credits < cost) return false

        _uiState.update { it.copy(credits = it.credits - cost) }
        val upg = weaponUpgrades[type] ?: WeaponUpgrade()
        weaponUpgrades[type] = upg.copy(damageLevel = upg.damageLevel + 1)
        saveUpgrades()
        soundSynth.playHitMarker()
        return true
    }

    fun upgradeWeaponClip(type: WeaponType): Boolean {
        val cost = 120 + ((weaponUpgrades[type]?.clipLevel ?: 0) * 80)
        if (_uiState.value.credits < cost) return false

        _uiState.update { it.copy(credits = it.credits - cost) }
        val upg = weaponUpgrades[type] ?: WeaponUpgrade()
        weaponUpgrades[type] = upg.copy(clipLevel = upg.clipLevel + 1)
        initWeapons()
        saveUpgrades()
        soundSynth.playHitMarker()
        return true
    }

    fun upgradeWeaponReload(type: WeaponType): Boolean {
        val cost = 100 + ((weaponUpgrades[type]?.reloadLevel ?: 0) * 80)
        if (_uiState.value.credits < cost) return false

        _uiState.update { it.copy(credits = it.credits - cost) }
        val upg = weaponUpgrades[type] ?: WeaponUpgrade()
        weaponUpgrades[type] = upg.copy(reloadLevel = upg.reloadLevel + 1)
        saveUpgrades()
        soundSynth.playHitMarker()
        return true
    }

    fun setSensitivity(value: Float) {
        _uiState.update { it.copy(lookSensitivity = value) }
        prefs.edit().putFloat("look_sens", value).apply()
    }

    fun toggleInvertLook() {
        val next = !_uiState.value.invertLookY
        _uiState.update { it.copy(invertLookY = next) }
        prefs.edit().putBoolean("invert_look", next).apply()
    }
}
