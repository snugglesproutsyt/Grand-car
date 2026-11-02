package com.example.model

enum class ArenaId(val displayName: String, val subtitle: String) {
    NEO_TOKYO("Neo-Tokyo Outpost", "Urban Cyber District with central plaza & defense towers"),
    BIO_BUNKER("Bio-Lab Facility", "Subterranean high-security research base with CQB corridors"),
    TITAN_COLOSSEUM("Titan Colosseum", "Heavy battle arena with cover pillars & sniper catwalks")
}

data class ArenaMap(
    val id: ArenaId,
    val width: Int,
    val height: Int,
    val grid: IntArray,
    val playerSpawnX: Float,
    val playerSpawnY: Float,
    val enemySpawns: List<Pair<Float, Float>>,
    val pickupSpawns: List<Pair<Float, Float>>
) {
    fun getTile(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return 1
        return grid[y * width + x]
    }

    fun isWall(x: Float, y: Float): Boolean {
        val tileX = x.toInt()
        val tileY = y.toInt()
        return getTile(tileX, tileY) > 0
    }
}

object MapRegistry {
    val neoTokyo = ArenaMap(
        id = ArenaId.NEO_TOKYO,
        width = 16,
        height = 16,
        grid = intArrayOf(
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 1,
            1, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 1,
            1, 0, 2, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 2, 0, 1,
            1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1,
            1, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 1,
            1, 2, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 2, 1,
            1, 0, 0, 0, 0, 0, 4, 0, 0, 4, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 4, 0, 0, 4, 0, 0, 0, 0, 0, 1,
            1, 2, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 2, 1,
            1, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 1,
            1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1,
            1, 0, 2, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 2, 0, 1,
            1, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 1,
            1, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
        ),
        playerSpawnX = 1.5f,
        playerSpawnY = 1.5f,
        enemySpawns = listOf(
            Pair(13.5f, 13.5f),
            Pair(13.5f, 2.5f),
            Pair(2.5f, 13.5f),
            Pair(7.5f, 2.5f),
            Pair(2.5f, 7.5f),
            Pair(13.5f, 8.5f),
            Pair(8.5f, 13.5f)
        ),
        pickupSpawns = listOf(
            Pair(7.5f, 7.5f),
            Pair(3.5f, 7.5f),
            Pair(11.5f, 7.5f),
            Pair(7.5f, 3.5f),
            Pair(7.5f, 11.5f)
        )
    )

    val bioBunker = ArenaMap(
        id = ArenaId.BIO_BUNKER,
        width = 16,
        height = 16,
        grid = intArrayOf(
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 0, 0, 0, 5, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 5,
            5, 0, 4, 0, 5, 0, 4, 4, 4, 0, 5, 0, 4, 4, 0, 5,
            5, 0, 4, 0, 0, 0, 4, 0, 4, 0, 0, 0, 0, 4, 0, 5,
            5, 0, 4, 4, 5, 0, 0, 0, 0, 0, 5, 4, 4, 4, 0, 5,
            5, 0, 0, 0, 5, 5, 0, 0, 0, 5, 5, 0, 0, 0, 0, 5,
            5, 5, 0, 5, 5, 0, 0, 2, 0, 0, 5, 5, 0, 5, 5, 5,
            5, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 5,
            5, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 5,
            5, 5, 0, 5, 5, 0, 0, 2, 0, 0, 5, 5, 0, 5, 5, 5,
            5, 0, 0, 0, 5, 5, 0, 0, 0, 5, 5, 0, 0, 0, 0, 5,
            5, 0, 4, 4, 5, 0, 0, 0, 0, 0, 5, 4, 4, 4, 0, 5,
            5, 0, 4, 0, 0, 0, 4, 0, 4, 0, 0, 0, 0, 4, 0, 5,
            5, 0, 4, 0, 5, 0, 4, 4, 4, 0, 5, 0, 4, 4, 0, 5,
            5, 0, 0, 0, 5, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5
        ),
        playerSpawnX = 3.5f,
        playerSpawnY = 3.5f,
        enemySpawns = listOf(
            Pair(12.5f, 12.5f),
            Pair(12.5f, 3.5f),
            Pair(3.5f, 12.5f),
            Pair(7.5f, 7.5f),
            Pair(7.5f, 1.5f),
            Pair(1.5f, 7.5f)
        ),
        pickupSpawns = listOf(
            Pair(7.5f, 7.5f),
            Pair(7.5f, 3.5f),
            Pair(7.5f, 11.5f),
            Pair(1.5f, 1.5f),
            Pair(13.5f, 13.5f)
        )
    )

    val titanColosseum = ArenaMap(
        id = ArenaId.TITAN_COLOSSEUM,
        width = 16,
        height = 16,
        grid = intArrayOf(
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3,
            3, 0, 1, 1, 0, 0, 2, 0, 0, 2, 0, 0, 1, 1, 0, 3,
            3, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 3,
            3, 0, 0, 0, 0, 5, 0, 0, 0, 0, 5, 0, 0, 0, 0, 3,
            3, 0, 0, 0, 5, 5, 0, 0, 0, 0, 5, 5, 0, 0, 0, 3,
            3, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 3,
            3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 3,
            3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 3,
            3, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 3,
            3, 0, 0, 0, 5, 5, 0, 0, 0, 0, 5, 5, 0, 0, 0, 3,
            3, 0, 0, 0, 0, 5, 0, 0, 0, 0, 5, 0, 0, 0, 0, 3,
            3, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 3,
            3, 0, 1, 1, 0, 0, 2, 0, 0, 2, 0, 0, 1, 1, 0, 3,
            3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3
        ),
        playerSpawnX = 2.5f,
        playerSpawnY = 7.5f,
        enemySpawns = listOf(
            Pair(13.5f, 7.5f),
            Pair(7.5f, 2.5f),
            Pair(7.5f, 13.5f),
            Pair(11.5f, 3.5f),
            Pair(11.5f, 11.5f),
            Pair(3.5f, 3.5f)
        ),
        pickupSpawns = listOf(
            Pair(7.5f, 5.5f),
            Pair(7.5f, 9.5f),
            Pair(3.5f, 7.5f),
            Pair(11.5f, 7.5f)
        )
    )

    fun getArena(id: ArenaId): ArenaMap = when (id) {
        ArenaId.NEO_TOKYO -> neoTokyo
        ArenaId.BIO_BUNKER -> bioBunker
        ArenaId.TITAN_COLOSSEUM -> titanColosseum
    }
}
