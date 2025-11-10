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

    fun shoot(): Bullet {
        val bulletX = x + cos(angle) * (radius + 20)
        val bulletY = y + sin(angle) * (radius + 20)
        return Bullet(bulletX, bulletY, angle, isPlayerBullet = true)
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

    override fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        // Health only regenerates on level up
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
