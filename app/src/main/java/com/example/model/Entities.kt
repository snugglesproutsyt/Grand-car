package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

enum class GameMode(val displayName: String, val description: String) {
    WAVE_DEFENSE("Wave Defense", "Survive escalating waves of cyber bots, drones, and heavy Titan bosses!"),
    DEATHMATCH("Skirmish 3D", "Free-for-all bot arena battle. First to 25 eliminations claims victory!"),
    FIRING_RANGE("Firing Range", "Test target practice with infinite ammo, weapon handling, and recoil drills.")
}

enum class EnemyType(
    val displayName: String,
    val maxHealth: Float,
    val speed: Float,
    val fireRateMs: Long,
    val damage: Float,
    val scoreValue: Int,
    val creditValue: Int,
    val radius: Float
) {
    SCOUT_DRONE("Scout Drone", 65f, 2.8f, 750L, 10f, 100, 25, 0.45f),
    CYBER_ENFORCER("Cyber Enforcer", 130f, 2.2f, 950L, 18f, 200, 50, 0.55f),
    TITAN_BOSS("Titan Mech IX", 700f, 1.4f, 1200L, 35f, 1000, 300, 0.95f)
}

enum class EnemyState {
    PATROL,
    CHASE,
    ATTACK,
    HURT,
    DEAD
}

data class EnemyBot(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var health: Float = type.maxHealth,
    var state: EnemyState = EnemyState.PATROL,
    var targetX: Float = x,
    var targetY: Float = y,
    var lastAttackTime: Long = 0L,
    var hitFlashTimer: Float = 0f,
    var walkCycle: Float = 0f,
    var deathTimer: Float = 0f
) {
    val isDead: Boolean get() = health <= 0f
}

enum class PickupType(val displayName: String, val color: Color) {
    MEDKIT("Nano-Medkit (+50 HP)", Color(0xFF00E676)),
    SHIELD("Cyber Shield (+50 Armor)", Color(0xFF00E5FF)),
    AMMO("Ammo Drop (Max Reserve)", Color(0xFFFFAB00)),
    DAMAGE_BOOST("Overcharge (2x Damage)", Color(0xFFFF1744)),
    SPEED_STIM("Adrenaline (+40% Speed)", Color(0xFFD500F9))
}

data class Pickup(
    val id: Int,
    val type: PickupType,
    val x: Float,
    val y: Float,
    var bobPhase: Float = 0f
)

enum class ProjectileType {
    BULLET,
    ROCKET,
    PLASMA,
    LASER_BEAM
}

data class Projectile(
    val id: Int,
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val speed: Float,
    val damage: Float,
    val isPlayer: Boolean,
    val type: ProjectileType,
    val explosionRadius: Float = 0f,
    var lifetime: Float = 4.0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val color: Color,
    val size: Float,
    var life: Float,
    val maxLife: Float
)

data class FloatingCombatText(
    val id: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    val text: String,
    val color: Color,
    var life: Float = 1.0f,
    val isHeadshot: Boolean = false
)

data class Player(
    var x: Float = 3.5f,
    var y: Float = 3.5f,
    var angle: Float = 0f, // radians
    var pitch: Float = 0f, // vertical camera tilt (-0.5 to 0.5)
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var shield: Float = 100f,
    var maxShield: Float = 100f,
    var sprintEnergy: Float = 1.0f,
    var isSprinting: Boolean = false,
    var isCrouching: Boolean = false,
    var isADS: Boolean = false,
    var selectedWeaponIndex: Int = 1, // Start with rifle or pistol
    var score: Int = 0,
    var credits: Int = 150,
    var kills: Int = 0,
    var headshots: Int = 0,
    var combo: Int = 0,
    var comboTimer: Float = 0f,
    var damageBoostTimer: Float = 0f,
    var speedStimTimer: Float = 0f,
    var hurtFlashAlpha: Float = 0f,
    var shieldFlashAlpha: Float = 0f,
    var headBob: Float = 0f,
    var screenShake: Float = 0f,
    var muzzleLight: Float = 0f,
    var lastHitAngle: Float = 0f,
    var hitIndicatorAlpha: Float = 0f
) {
    val dirX: Float get() = cos(angle)
    val dirY: Float get() = sin(angle)
}
