package com.diep.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class EnemyType {
    SQUARE, TRIANGLE, PENTAGON
}

class Enemy(
    x: Float,
    y: Float,
    val type: EnemyType
) : GameObject(x, y, getRadiusForType(type)) {

    var health: Float
    var maxHealth: Float
    private val scoreValue: Int
    private val rotationSpeed: Float = Random.nextFloat() * 2f - 1f // -1 to 1 rad/s
    private var rotation: Float = Random.nextFloat() * 360f

    private val paint: Paint
    private val borderPaint = Paint().apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
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

    init {
        val (hp, score, paintColor) = when (type) {
            EnemyType.SQUARE -> Triple(30f, 10, 0xFFFFE869.toInt())
            EnemyType.TRIANGLE -> Triple(50f, 25, 0xFFFC7677.toInt())
            EnemyType.PENTAGON -> Triple(100f, 130, 0xFF768DFC.toInt())
        }
        health = hp
        maxHealth = hp
        scoreValue = score
        paint = Paint().apply {
            color = paintColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Random movement
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        val speed = 50f
        velocityX = cos(angle) * speed
        velocityY = sin(angle) * speed
    }

    fun takeDamage(damage: Float): Int {
        health -= damage
        if (health <= 0) {
            health = 0f
            isAlive = false
            return scoreValue
        }
        return 0
    }

    override fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime
        rotation += rotationSpeed * deltaTime * 60f
    }

    override fun draw(canvas: Canvas, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        canvas.save()
        canvas.rotate(rotation, screenX, screenY)

        when (type) {
            EnemyType.SQUARE -> {
                canvas.drawRect(
                    screenX - radius,
                    screenY - radius,
                    screenX + radius,
                    screenY + radius,
                    paint
                )
                canvas.drawRect(
                    screenX - radius,
                    screenY - radius,
                    screenX + radius,
                    screenY + radius,
                    borderPaint
                )
            }
            EnemyType.TRIANGLE -> drawPolygon(canvas, screenX, screenY, 3)
            EnemyType.PENTAGON -> drawPolygon(canvas, screenX, screenY, 5)
        }

        canvas.restore()

        // Draw health bar if damaged
        if (health < maxHealth) {
            val healthBarWidth = radius * 1.5f
            val healthBarHeight = 5f
            val healthBarY = screenY - radius - 12

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

    private fun drawPolygon(canvas: Canvas, cx: Float, cy: Float, sides: Int) {
        val path = Path()
        val angleStep = 2 * Math.PI / sides

        for (i in 0 until sides) {
            val angle = i * angleStep - Math.PI / 2
            val px = cx + radius * cos(angle).toFloat()
            val py = cy + radius * sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        canvas.drawPath(path, paint)
        canvas.drawPath(path, borderPaint)
    }

    companion object {
        private fun getRadiusForType(type: EnemyType): Float {
            return when (type) {
                EnemyType.SQUARE -> 25f
                EnemyType.TRIANGLE -> 28f
                EnemyType.PENTAGON -> 35f
            }
        }
    }
}
