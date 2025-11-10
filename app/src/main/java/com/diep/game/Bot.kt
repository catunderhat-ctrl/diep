package com.diep.game

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Bot(x: Float, y: Float) : GameObject(x, y, 30f) {
    var angle: Float = 0f
    var health: Float = 100f
    var maxHealth: Float = 100f
    var score: Int = 0
    var level: Int = 1

    // AI properties
    private var target: GameObject? = null
    private var targetSearchTimer: Float = 0f
    private val targetSearchInterval: Float = 1f // Search for target every second
    var shootTimer: Float = 0f // Made public so GameView can reset it after shooting
    val shootCooldown: Float = 0.2f // Made public so GameView can access it
    private val aggroRange: Float = 600f
    private val shootRange: Float = 500f
    private val retreatHealthPercent: Float = 0.3f

    // Bot color variants
    private val botColorIndex = Random.nextInt(3)

    private val bodyPaint = Paint().apply {
        color = when (botColorIndex) {
            0 -> 0xFFFF6B6B.toInt() // Red bot
            1 -> 0xFF4ECDC4.toInt() // Teal bot
            else -> 0xFFFFE66D.toInt() // Yellow bot
        }
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

    fun shoot(): Bullet {
        val bulletX = x + cos(angle) * (radius + 20)
        val bulletY = y + sin(angle) * (radius + 20)
        return Bullet(bulletX, bulletY, angle, isPlayerBullet = false)
    }

    fun takeDamage(damage: Float) {
        health -= damage
        if (health <= 0) {
            health = 0f
            isAlive = false
        }
    }

    fun addScore(points: Int) {
        score += points
        // Level up every 100 points
        val newLevel = score / 100 + 1
        if (newLevel > level) {
            level = newLevel
            maxHealth += 20
            health = maxHealth
        }
    }

    fun updateAI(deltaTime: Float, player: Tank, enemies: List<Enemy>, otherBots: List<Bot>) {
        // Update target search timer
        targetSearchTimer -= deltaTime
        if (targetSearchTimer <= 0f) {
            findTarget(player, enemies, otherBots)
            targetSearchTimer = targetSearchInterval
        }

        // AI behavior based on target
        target?.let { currentTarget ->
            if (!currentTarget.isAlive) {
                target = null
                return
            }

            val distanceToTarget = distanceTo(currentTarget)
            val healthPercent = health / maxHealth

            // Aim at target
            aimAt(currentTarget.x, currentTarget.y)

            // Movement logic
            when {
                // Retreat if low health
                healthPercent < retreatHealthPercent -> {
                    // Move away from target
                    val retreatAngle = atan2(y - currentTarget.y, x - currentTarget.x)
                    velocityX = cos(retreatAngle) * 200f
                    velocityY = sin(retreatAngle) * 200f
                }
                // Move closer if too far
                distanceToTarget > shootRange * 0.7f -> {
                    val approachAngle = atan2(currentTarget.y - y, currentTarget.x - x)
                    velocityX = cos(approachAngle) * 250f
                    velocityY = sin(approachAngle) * 250f
                }
                // Maintain distance
                distanceToTarget < shootRange * 0.4f -> {
                    val retreatAngle = atan2(y - currentTarget.y, x - currentTarget.x)
                    velocityX = cos(retreatAngle) * 150f
                    velocityY = sin(retreatAngle) * 150f
                }
                // Circle strafe
                else -> {
                    val strafeAngle = atan2(currentTarget.y - y, currentTarget.x - x) + Math.PI.toFloat() / 2
                    velocityX = cos(strafeAngle) * 180f
                    velocityY = sin(strafeAngle) * 180f
                }
            }

            // Update shoot timer (actual shooting happens in GameView)
            shootTimer -= deltaTime

            // Add some aiming inaccuracy when ready to shoot
            if (distanceToTarget < shootRange && shootTimer <= 0f) {
                val inaccuracy = (Random.nextFloat() - 0.5f) * 0.2f
                angle += inaccuracy
                // Note: shootTimer is reset in GameView after actual shooting
            }
        } ?: run {
            // No target - wander randomly
            if (Random.nextFloat() < 0.02f) { // 2% chance per frame to change direction
                val wanderAngle = Random.nextFloat() * 2 * Math.PI.toFloat()
                velocityX = cos(wanderAngle) * 100f
                velocityY = sin(wanderAngle) * 100f
            }
        }
    }

    private fun findTarget(player: Tank, enemies: List<Enemy>, otherBots: List<Bot>) {
        var closestTarget: GameObject? = null
        var closestDistance = Float.MAX_VALUE

        // Consider player as target
        if (player.isAlive) {
            val distanceToPlayer = distanceTo(player)
            if (distanceToPlayer < aggroRange) {
                closestTarget = player
                closestDistance = distanceToPlayer
            }
        }

        // Consider enemies as targets (bots also farm enemies)
        enemies.forEach { enemy ->
            if (enemy.isAlive) {
                val distance = distanceTo(enemy)
                if (distance < aggroRange && distance < closestDistance) {
                    closestTarget = enemy
                    closestDistance = distance
                }
            }
        }

        // Optionally consider other bots as targets (bot vs bot combat)
        if (Random.nextFloat() < 0.3f) { // 30% chance to target other bots
            otherBots.forEach { bot ->
                if (bot != this && bot.isAlive) {
                    val distance = distanceTo(bot)
                    if (distance < aggroRange * 0.7f && distance < closestDistance) {
                        closestTarget = bot
                        closestDistance = distance
                    }
                }
            }
        }

        target = closestTarget
    }

    fun canShoot(): Boolean {
        return shootTimer <= 0f && target != null && distanceTo(target!!) < shootRange
    }

    override fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        // Regenerate health slowly
        if (health < maxHealth) {
            health += 1.5f * deltaTime
            if (health > maxHealth) health = maxHealth
        }
    }

    override fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        // Draw barrel
        val barrelLength = radius + 35
        val barrelWidth = 15f
        canvas.save()
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat(), screenX, screenY)
        canvas.drawRect(
            screenX + radius - barrelWidth / 2,
            screenY - barrelWidth / 2,
            screenX + barrelLength,
            screenY + barrelWidth / 2,
            barrelPaint
        )
        canvas.drawRect(
            screenX + radius - barrelWidth / 2,
            screenY - barrelWidth / 2,
            screenX + barrelLength,
            screenY + barrelWidth / 2,
            borderPaint
        )
        canvas.restore()

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
}
