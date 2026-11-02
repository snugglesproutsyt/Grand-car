package com.example.model

enum class WeaponType(
    val id: String,
    val displayName: String,
    val category: String,
    val baseDamage: Float,
    val fireRateMs: Long,
    val baseClip: Int,
    val baseReserve: Int,
    val reloadTimeMs: Long,
    val spreadAngle: Float,
    val isAutomatic: Boolean,
    val pellets: Int,
    val headshotMultiplier: Float,
    val zoomFactor: Float,
    val recoilKickY: Float,
    val unlockCost: Int
) {
    PISTOL(
        id = "pistol",
        displayName = "Nova-9 Tactical",
        category = "Sidearm",
        baseDamage = 28f,
        fireRateMs = 260L,
        baseClip = 15,
        baseReserve = 90,
        reloadTimeMs = 1200L,
        spreadAngle = 0.02f,
        isAutomatic = false,
        pellets = 1,
        headshotMultiplier = 2.0f,
        zoomFactor = 1.25f,
        recoilKickY = 8f,
        unlockCost = 0
    ),
    RIFLE(
        id = "rifle",
        displayName = "V-7 Sentinel Rifle",
        category = "Assault Carbine",
        baseDamage = 34f,
        fireRateMs = 110L, // ~550 RPM
        baseClip = 30,
        baseReserve = 180,
        reloadTimeMs = 2000L,
        spreadAngle = 0.045f,
        isAutomatic = true,
        pellets = 1,
        headshotMultiplier = 1.75f,
        zoomFactor = 1.35f,
        recoilKickY = 12f,
        unlockCost = 300
    ),
    SHOTGUN(
        id = "shotgun",
        displayName = "Devastator-12",
        category = "Combat Shotgun",
        baseDamage = 16f, // 8 pellets * 16 = 128 max burst!
        fireRateMs = 650L,
        baseClip = 8,
        baseReserve = 48,
        reloadTimeMs = 2400L,
        spreadAngle = 0.12f,
        isAutomatic = false,
        pellets = 8,
        headshotMultiplier = 1.5f,
        zoomFactor = 1.15f,
        recoilKickY = 22f,
        unlockCost = 600
    ),
    SNIPER(
        id = "sniper",
        displayName = "Reaper X Plasma",
        category = "Sniper Rifle",
        baseDamage = 120f,
        fireRateMs = 950L,
        baseClip = 5,
        baseReserve = 30,
        reloadTimeMs = 2800L,
        spreadAngle = 0.005f,
        isAutomatic = false,
        pellets = 1,
        headshotMultiplier = 2.5f, // 300 headshot damage!
        zoomFactor = 2.8f, // High magnification telescopic scope
        recoilKickY = 26f,
        unlockCost = 1000
    ),
    ROCKET(
        id = "rocket",
        displayName = "Havoc RPG",
        category = "Heavy Ordnance",
        baseDamage = 240f,
        fireRateMs = 1400L,
        baseClip = 1,
        baseReserve = 12,
        reloadTimeMs = 2600L,
        spreadAngle = 0.01f,
        isAutomatic = false,
        pellets = 1,
        headshotMultiplier = 1.0f,
        zoomFactor = 1.2f,
        recoilKickY = 32f,
        unlockCost = 1500
    )
}

data class WeaponUpgrade(
    val damageLevel: Int = 0, // +10% per level
    val clipLevel: Int = 0,   // +20% per level
    val reloadLevel: Int = 0, // -12% reload time per level
    val isUnlocked: Boolean = false,
    val skinSelected: Int = 0 // 0: Default, 1: Neon Cyan, 2: Cyber Gold
) {
    fun getDamage(base: Float): Float = base * (1f + damageLevel * 0.12f)
    fun getClip(base: Int): Int = base + (base * (clipLevel * 0.2f)).toInt()
    fun getReloadTime(base: Long): Long = (base * (1f - (reloadLevel * 0.12f))).toLong().coerceAtLeast(600L)
}

data class WeaponState(
    val type: WeaponType,
    var currentClip: Int,
    var reserveAmmo: Int,
    var isReloading: Boolean = false,
    var reloadProgress: Float = 0f,
    var lastFiredTime: Long = 0L,
    var recoilAnim: Float = 0f // 0f to 1f for kickback animation
)
