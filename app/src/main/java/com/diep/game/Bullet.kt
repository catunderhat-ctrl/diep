package com.diep.game

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

class Bullet(
    x: Float,
    y: Float,
    angle: Float,
    val isPlayerBullet: Boolean,
    private val speed: Float = 800f
) : GameObject(x, y, 8f) {

    var lifetime: Float = 3f // seconds
    val damage: Float = 20f

    private val paint = Paint().apply {
        color = if (isPlayerBullet) 0xFF00B2E1.toInt() else 0xFFFC7677.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    init {
        velocityX = cos(angle) * speed
        velocityY = sin(angle) * speed
    }

    override fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime

        lifetime -= deltaTime
        if (lifetime <= 0) {
            isAlive = false
        }
    }

    override fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        canvas.drawCircle(screenX, screenY, radius, paint)
        canvas.drawCircle(screenX, screenY, radius, borderPaint)
    }
}
