package com.diep.game

data class TankStats(
    var healthRegen: Int = 0,      // Max 7
    var maxHealth: Int = 0,         // Max 7
    var bodyDamage: Int = 0,        // Max 7
    var bulletSpeed: Int = 0,       // Max 7
    var bulletPenetration: Int = 0, // Max 7
    var bulletDamage: Int = 0,      // Max 7
    var reload: Int = 0,            // Max 7
    var movementSpeed: Int = 0      // Max 7
) {
    companion object {
        const val MAX_STAT_LEVEL = 7
        const val STAT_POINTS_PER_LEVEL = 1
    }

    fun getTotalPoints(): Int {
        return healthRegen + maxHealth + bodyDamage + bulletSpeed +
                bulletPenetration + bulletDamage + reload + movementSpeed
    }

    fun canUpgrade(stat: StatType): Boolean {
        return when (stat) {
            StatType.HEALTH_REGEN -> healthRegen < MAX_STAT_LEVEL
            StatType.MAX_HEALTH -> maxHealth < MAX_STAT_LEVEL
            StatType.BODY_DAMAGE -> bodyDamage < MAX_STAT_LEVEL
            StatType.BULLET_SPEED -> bulletSpeed < MAX_STAT_LEVEL
            StatType.BULLET_PENETRATION -> bulletPenetration < MAX_STAT_LEVEL
            StatType.BULLET_DAMAGE -> bulletDamage < MAX_STAT_LEVEL
            StatType.RELOAD -> reload < MAX_STAT_LEVEL
            StatType.MOVEMENT_SPEED -> movementSpeed < MAX_STAT_LEVEL
        }
    }

    fun upgrade(stat: StatType) {
        when (stat) {
            StatType.HEALTH_REGEN -> if (healthRegen < MAX_STAT_LEVEL) healthRegen++
            StatType.MAX_HEALTH -> if (maxHealth < MAX_STAT_LEVEL) maxHealth++
            StatType.BODY_DAMAGE -> if (bodyDamage < MAX_STAT_LEVEL) bodyDamage++
            StatType.BULLET_SPEED -> if (bulletSpeed < MAX_STAT_LEVEL) bulletSpeed++
            StatType.BULLET_PENETRATION -> if (bulletPenetration < MAX_STAT_LEVEL) bulletPenetration++
            StatType.BULLET_DAMAGE -> if (bulletDamage < MAX_STAT_LEVEL) bulletDamage++
            StatType.RELOAD -> if (reload < MAX_STAT_LEVEL) reload++
            StatType.MOVEMENT_SPEED -> if (movementSpeed < MAX_STAT_LEVEL) movementSpeed++
        }
    }

    fun getStatLevel(stat: StatType): Int {
        return when (stat) {
            StatType.HEALTH_REGEN -> healthRegen
            StatType.MAX_HEALTH -> maxHealth
            StatType.BODY_DAMAGE -> bodyDamage
            StatType.BULLET_SPEED -> bulletSpeed
            StatType.BULLET_PENETRATION -> bulletPenetration
            StatType.BULLET_DAMAGE -> bulletDamage
            StatType.RELOAD -> reload
            StatType.MOVEMENT_SPEED -> movementSpeed
        }
    }

    // Calculate actual values based on stat levels
    fun getHealthRegenRate(): Float = healthRegen * 0.5f // 0 to 3.5 HP/sec
    fun getMaxHealthBonus(): Float = maxHealth * 20f // 0 to 140 extra HP
    fun getBodyDamageMultiplier(): Float = 1f + bodyDamage * 0.2f // 1x to 2.4x
    fun getBulletSpeedMultiplier(): Float = 1f + bulletSpeed * 0.15f // 1x to 2.05x
    fun getBulletPenetrationBonus(): Float = bulletPenetration * 5f // 0 to 35 extra penetration
    fun getBulletDamageMultiplier(): Float = 1f + bulletDamage * 0.2f // 1x to 2.4x
    fun getReloadMultiplier(): Float = 1f / (1f + reload * 0.15f) // Faster reload (lower cooldown)
    fun getMovementSpeedMultiplier(): Float = 1f + movementSpeed * 0.1f // 1x to 1.7x
}

enum class StatType(val displayName: String, val shortName: String) {
    HEALTH_REGEN("Health Regen", "H.Regen"),
    MAX_HEALTH("Max Health", "Health"),
    BODY_DAMAGE("Body Damage", "B.Dam"),
    BULLET_SPEED("Bullet Speed", "B.Spd"),
    BULLET_PENETRATION("Bullet Penetration", "B.Pen"),
    BULLET_DAMAGE("Bullet Damage", "B.Dam"),
    RELOAD("Reload", "Reload"),
    MOVEMENT_SPEED("Movement Speed", "M.Spd")
}
