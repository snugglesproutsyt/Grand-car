package com.example

import com.example.model.ArenaId
import com.example.model.MapRegistry
import com.example.model.WeaponType
import com.example.model.WeaponUpgrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testWeaponUpgradeCalculations() {
        val baseDamage = 34f
        val upgrade = WeaponUpgrade(damageLevel = 2, clipLevel = 1, reloadLevel = 1)

        // Damage: +12% per level (2 levels = +24%)
        val expectedDmg = baseDamage * (1f + 2 * 0.12f)
        assertEquals(expectedDmg, upgrade.getDamage(baseDamage), 0.01f)

        // Clip: 30 base + (30 * 0.20 * 1) = 36
        val baseClip = 30
        assertEquals(36, upgrade.getClip(baseClip))

        // Reload: -12% of 2000ms = 1760ms
        val baseReload = 2000L
        assertEquals(1760L, upgrade.getReloadTime(baseReload))
    }

    @Test
    fun testMapWallCollisions() {
        val map = MapRegistry.neoTokyo
        // Outer perimeter walls
        assertTrue("Tile (0,0) must be a wall", map.isWall(0.5f, 0.5f))
        assertTrue("Tile (15,15) must be a wall", map.isWall(15.5f, 15.5f))

        // Player spawn area should be open floor
        assertFalse("Player spawn (1.5, 1.5) must not be a wall", map.isWall(1.5f, 1.5f))
    }

    @Test
    fun testWeaponTypesConfiguration() {
        assertTrue("Sniper should have zoom magnification greater than 2.0", WeaponType.SNIPER.zoomFactor > 2.0f)
        assertTrue("Shotgun should fire multiple pellets", WeaponType.SHOTGUN.pellets > 1)
        assertTrue("Rifle should be automatic", WeaponType.RIFLE.isAutomatic)
        assertFalse("Pistol should not be automatic", WeaponType.PISTOL.isAutomatic)
    }

    @Test
    fun testAllArenaMapsValidity() {
        for (arena in ArenaId.values()) {
            val map = MapRegistry.getArena(arena)
            assertEquals("Map grid size must match width x height", map.width * map.height, map.grid.size)
            assertFalse("Player spawn should be on walkable ground", map.isWall(map.playerSpawnX, map.playerSpawnY))
            assertTrue("Should have enemy spawn locations", map.enemySpawns.isNotEmpty())
            assertTrue("Should have pickup spawn locations", map.pickupSpawns.isNotEmpty())
        }
    }
}
