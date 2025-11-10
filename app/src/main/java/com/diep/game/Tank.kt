package com.diep.game

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Tank(x: Float, y: Float) : GameObject(x, y, 30f) {
    var angle: Float = 0f
    var health: Float = 100f
    var maxHealth: Float = 100f
    var score: Int = 0
    var level: Int = 1
    var availableStatPoints: Int = 0

    var tankClass: TankClass = TankClass.BASIC
    val stats: TankStats = TankStats()

    private var healthRegenTimer: Float = 0f
    private val healthRegenInterval: Float = 1f // Regen every second

    // Barrel alternation for multi-barrel tanks
    private var currentBarrel: Int = 0

    private val bodyPaint = Paint().apply {
        color = 0xFF00B2E1.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barrelPaint = Paint().apply {
        color = 0xFF999999.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val healthBgPaint = Paint().apply {
        color = 0xFF555555.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val healthFgPaint = Paint().apply {
        color = 0xFF85E37D.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    fun aimAt(targetX: Float, targetY: Float) {
        angle = atan2(targetY - y, targetX - x)
    }

    fun shoot(): List<Bullet> {
        val bullets = mutableListOf<Bullet>()
        val barrelCount = tankClass.barrelCount

        if (barrelCount == 1) {
            // Single barrel
            val bulletX = x + cos(angle) * (radius + 20)
            val bulletY = y + sin(angle) * (radius + 20)
            bullets.add(createBullet(bulletX, bulletY, angle))
        } else {
            // Multiple barrels - shoot from current barrel only
            val barrelAngle = if (tankClass == TankClass.FLANK_GUARD) {
                // Flank guard shoots from opposite barrels
                angle + currentBarrel * tankClass.barrelSpread
            } else {
                // Twin shoots from alternating barrels
                val offset = if (currentBarrel == 0) -tankClass.barrelSpread / 2 else tankClass.barrelSpread / 2
                angle + offset
            }

            val bulletX = x + cos(barrelAngle) * (radius + 20)
            val bulletY = y + sin(barrelAngle) * (radius + 20)
            bullets.add(createBullet(bulletX, bulletY, angle))

            // Alternate barrels
            currentBarrel = (currentBarrel + 1) % barrelCount
        }

        return bullets
    }

    private fun createBullet(x: Float, y: Float, angle: Float): Bullet {
        val speedMultiplier = stats.getBulletSpeedMultiplier() * tankClass.bulletSpeed
        val damageMultiplier = stats.getBulletDamageMultiplier() * tankClass.bulletDamage
        return Bullet(x, y, angle, isPlayerBullet = true, speedMultiplier, damageMultiplier)
    }

    fun getShootCooldown(): Float {
        return 0.15f * stats.getReloadMultiplier() / tankClass.fireRate
    }

    fun getMoveSpeed(): Float {
        return 300f * stats.getMovementSpeedMultiplier()
    }

    fun takeDamage(damage: Float) {
        health -= damage / stats.getBodyDamageMultiplier()
        if (health <= 0) {
            health = 0f
            isAlive = false
        }
    }

    fun upgradeStat(stat: StatType): Boolean {
        if (availableStatPoints > 0 && stats.canUpgrade(stat)) {
            stats.upgrade(stat)
            availableStatPoints--

            // Update max health if health stat was upgraded
            if (stat == StatType.MAX_HEALTH) {
                val oldMaxHealth = maxHealth
                maxHealth = 100f + stats.getMaxHealthBonus()
                // Increase current health proportionally
                health += (maxHealth - oldMaxHealth)
            }

            return true
        }
        return false
    }

    fun upgradeTankClass(newClass: TankClass): Boolean {
        val availableUpgrades = tankClass.getNextUpgrade(level)
        if (newClass in availableUpgrades) {
            tankClass = newClass
            currentBarrel = 0
            return true
        }
        return false
    }

    fun addScore(points: Int) {
        score += points
        // Level up every 100 points
        val newLevel = score / 100 + 1
        if (newLevel > level) {
            val levelsGained = newLevel - level
            level = newLevel

            // Grant stat points
            availableStatPoints += levelsGained * TankStats.STAT_POINTS_PER_LEVEL

            // Auto-upgrade stats in balanced way (temporary, will add UI later)
            autoUpgradeStats(availableStatPoints)
            availableStatPoints = 0

            // Fully restore health on level up
            maxHealth = 100f + stats.getMaxHealthBonus()
            health = maxHealth
        }
    }

    private fun autoUpgradeStats(points: Int) {
        // Distribute points in a balanced way
        val statsToUpgrade = listOf(
            StatType.RELOAD,
            StatType.BULLET_DAMAGE,
            StatType.MAX_HEALTH,
            StatType.MOVEMENT_SPEED,
            StatType.BULLET_SPEED,
            StatType.HEALTH_REGEN,
            StatType.BODY_DAMAGE,
            StatType.BULLET_PENETRATION
        )

        var remainingPoints = points
        var currentStatIndex = 0

        while (remainingPoints > 0) {
            val stat = statsToUpgrade[currentStatIndex % statsToUpgrade.size]
            if (stats.canUpgrade(stat)) {
                stats.upgrade(stat)
                if (stat == StatType.MAX_HEALTH) {
                    val oldMaxHealth = maxHealth
                    maxHealth = 100f + stats.getMaxHealthBonus()
                    health += (maxHealth - oldMaxHealth)
                }
                remainingPoints--
            }
            currentStatIndex++

            // Prevent infinite loop if all stats are maxed
            if (currentStatIndex > statsToUpgrade.size * TankStats.MAX_STAT_LEVEL) {
                break
            }
        }
    }

    override fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        // Health regeneration based on stats
        val regenRate = stats.getHealthRegenRate()
        if (regenRate > 0 && health < maxHealth) {
            healthRegenTimer += deltaTime
            if (healthRegenTimer >= healthRegenInterval) {
                health += regenRate
                if (health > maxHealth) health = maxHealth
                healthRegenTimer = 0f
            }
        }
    }

    override fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        // Draw barrels based on tank class
        val barrelLength = radius + 35
        val barrelWidth = 15f

        when (tankClass) {
            TankClass.BASIC -> drawSingleBarrel(canvas, screenX, screenY, angle, barrelLength, barrelWidth)
            TankClass.TWIN -> {
                // Two barrels side by side
                val spread = tankClass.barrelSpread
                drawSingleBarrel(canvas, screenX, screenY, angle - spread / 2, barrelLength, barrelWidth)
                drawSingleBarrel(canvas, screenX, screenY, angle + spread / 2, barrelLength, barrelWidth)
            }
            TankClass.MACHINE_GUN -> {
                // Wider barrel
                drawSingleBarrel(canvas, screenX, screenY, angle, barrelLength - 5, barrelWidth + 5)
            }
            TankClass.SNIPER -> {
                // Longer, thinner barrel
                drawSingleBarrel(canvas, screenX, screenY, angle, barrelLength + 15, barrelWidth - 3)
            }
            TankClass.FLANK_GUARD -> {
                // Front and back barrels
                drawSingleBarrel(canvas, screenX, screenY, angle, barrelLength, barrelWidth)
                drawSingleBarrel(canvas, screenX, screenY, angle + 3.14159f, barrelLength, barrelWidth)
            }
        }

        // Draw body
        canvas.drawCircle(screenX, screenY, radius, bodyPaint)
        canvas.drawCircle(screenX, screenY, radius, borderPaint)

        // Draw health bar
        val healthBarWidth = 60f
        val healthBarHeight = 6f
        val healthBarY = screenY - radius - 15

        canvas.drawRect(
            screenX - healthBarWidth / 2,
            healthBarY,
            screenX + healthBarWidth / 2,
            healthBarY + healthBarHeight,
            healthBgPaint
        )

        val healthPercent = health / maxHealth
        canvas.drawRect(
            screenX - healthBarWidth / 2,
            healthBarY,
            screenX - healthBarWidth / 2 + healthBarWidth * healthPercent,
            healthBarY + healthBarHeight,
            healthFgPaint
        )
    }

    private fun drawSingleBarrel(canvas: Canvas, screenX: Float, screenY: Float, barrelAngle: Float, length: Float, width: Float) {
        canvas.save()
        canvas.rotate(Math.toDegrees(barrelAngle.toDouble()).toFloat(), screenX, screenY)
        canvas.drawRect(
            screenX + radius - width / 2,
            screenY - width / 2,
            screenX + length,
            screenY + width / 2,
            barrelPaint
        )
        canvas.drawRect(
            screenX + radius - width / 2,
            screenY - width / 2,
            screenX + length,
            screenY + width / 2,
            borderPaint
        )
        canvas.restore()
    }
}
