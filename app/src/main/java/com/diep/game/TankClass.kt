package com.diep.game

enum class TankClass(
    val displayName: String,
    val barrelCount: Int,
    val barrelSpread: Float, // Angle between barrels in radians
    val fireRate: Float, // Shots per second multiplier
    val bulletSpeed: Float, // Speed multiplier
    val bulletDamage: Float, // Damage multiplier
    val recoil: Float // Recoil multiplier
) {
    BASIC(
        displayName = "Basic",
        barrelCount = 1,
        barrelSpread = 0f,
        fireRate = 1f,
        bulletSpeed = 1f,
        bulletDamage = 1f,
        recoil = 1f
    ),
    TWIN(
        displayName = "Twin",
        barrelCount = 2,
        barrelSpread = 0.15f,
        fireRate = 1.2f,
        bulletSpeed = 1f,
        bulletDamage = 0.9f,
        recoil = 1.2f
    ),
    MACHINE_GUN(
        displayName = "Machine Gun",
        barrelCount = 1,
        barrelSpread = 0f,
        fireRate = 2.5f,
        bulletSpeed = 0.9f,
        bulletDamage = 0.6f,
        recoil = 0.8f
    ),
    SNIPER(
        displayName = "Sniper",
        barrelCount = 1,
        barrelSpread = 0f,
        fireRate = 0.7f,
        bulletSpeed = 1.5f,
        bulletDamage = 1.5f,
        recoil = 1.5f
    ),
    FLANK_GUARD(
        displayName = "Flank Guard",
        barrelCount = 2,
        barrelSpread = 3.14159f, // Pi radians (180 degrees)
        fireRate = 1f,
        bulletSpeed = 1f,
        bulletDamage = 1f,
        recoil = 0.5f
    );

    fun getNextUpgrade(currentLevel: Int): List<TankClass> {
        return when {
            this == BASIC && currentLevel >= 15 -> listOf(TWIN, MACHINE_GUN, SNIPER, FLANK_GUARD)
            else -> emptyList()
        }
    }
}
